/**
 * An Image Picker Plugin for Cordova/PhoneGap.
 */
package com.synconset;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import android.app.ActivityManager;
import android.content.Context;

import com.photoselector.ui.PhotoSelectorActivity;

public class ImagePicker extends CordovaPlugin {
	public static String TAG = "ImagePicker";

	private CallbackContext callbackContext;
	private JSONObject params;
	private int maxImages;

	public boolean execute(String action, final JSONArray args, final CallbackContext callbackContext) throws JSONException {
		if(callbackContext == null)
			return false;

		this.callbackContext = callbackContext;

		this.params = args.getJSONObject(0);
		ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
		ActivityManager activityManager = (ActivityManager) this.cordova.getActivity().getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
		activityManager.getMemoryInfo(mi);
		long totalMegs = mi.totalMem / 1048576L;
		// long availableMegs = mi.availMem / 1048576L;
		// long threshold = mi.threshold / 1048576L;

		System.out.println("[NIX] totalMegs: " + totalMegs);
		// System.out.println("[NIX] availableMegs: " + availableMegs);
		// System.out.println("[NIX] threshold: " + threshold);


		if (action.equals("getPictures")) {
			Intent intent = new Intent(cordova.getActivity(), PhotoSelectorActivity.class);
			//Intent intent = new Intent(cordova.getActivity(), MultiImageChooserActivity.class);
			int max = 5;
			int desiredWidth = 0;
			int desiredHeight = 0;
			int quality = 100;
			ArrayList<String>  preSelectedAssets = new ArrayList<String>();
			if (this.params.has("maximumImagesCount")) {
				if(totalMegs > 1000) {
					max = this.params.getInt("maximumImagesCount");
				}
			}
			System.out.println("[NIX] Maximum images: " + max);
			if (this.params.has("width")) {
				desiredWidth = this.params.getInt("width");
			}
			if (this.params.has("height")) {
				desiredHeight = this.params.getInt("height");
			}
			if (this.params.has("quality")) {
				quality = this.params.getInt("quality");
			}
			if (this.params.has("assets")) {
				JSONArray assets = this.params.getJSONArray("assets");
				if (assets != null) {
					for(int i=0; i < assets.length(); i++) {
						preSelectedAssets.add(assets.get(i).toString());
					}
				}
			}
			maxImages = max;
//			preSelectedAssets.add("file:///storage/emulated/0/Pictures/IMG_20160108_112930.jpg".replaceAll("file://", ""));
//			preSelectedAssets.add("file:///storage/emulated/0/Pictures/photo-1448975750337-b0290d621d6d.jpeg".replaceAll("file://", ""));
			intent.putExtra("MAX_IMAGES", max);
			intent.putExtra("WIDTH", desiredWidth);
			intent.putExtra("HEIGHT", desiredHeight);
			intent.putExtra("QUALITY", quality);
			intent.putExtra("PRE_SELECTED_ASSETS", preSelectedAssets);
			intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
			/*
			if (this.cordova != null) {
				this.cordova.startActivityForResult((CordovaPlugin) this, intent, 0);
			}
			*/
			if (this.cordova != null) {
				this.cordova.startActivityForResult((CordovaPlugin) this, intent, 0);
			}
		} else if (action.equals("cleanupTempFiles")) {
			cleanupTempFiles();
		}
		return true;
	}


	private void cleanupTempFiles() {
		File filePath = new File(System.getProperty("java.io.tmpdir"));
		for(final File fileEntry: filePath.listFiles()) {
			System.out.println("File Entry: " + fileEntry);
			fileEntry.delete();
		}
		this.callbackContext.success(new JSONObject());
	}


	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == Activity.RESULT_OK && data != null) {
			ArrayList<String> fileNames = data.getStringArrayListExtra("MULTIPLEFILENAMES");
			ArrayList<String> preSelectedAssets = data.getStringArrayListExtra("SELECTED_ASSETS");
			JSONArray jsFileNames = new JSONArray(fileNames);
			JSONArray jsPreSelectedAssets = new JSONArray(preSelectedAssets);
			JSONObject res = new JSONObject();

			try {
				res.put("images", jsFileNames);
				res.put("preSelectedAssets", jsPreSelectedAssets);
				res.put("max", maxImages);
			} catch (JSONException e) {
				e.printStackTrace();
			}

			if(res == null)
				this.callbackContext.error("No images selected");
			this.callbackContext.success(res);
		} else if (resultCode == Activity.RESULT_CANCELED && data != null) {
			String error = data.getStringExtra("ERRORMESSAGE");
			if(error == null)
				this.callbackContext.error("No images selected");
			this.callbackContext.error(error);
		} else if (resultCode == Activity.RESULT_CANCELED) {
			JSONArray res = new JSONArray();
			if(res == null)
				this.callbackContext.error("No images selected");
			this.callbackContext.success(res);
		} else {
			this.callbackContext.error("No images selected");
		}
	}
}

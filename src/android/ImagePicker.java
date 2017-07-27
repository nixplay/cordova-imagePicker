/**
 * An Image Picker Plugin for Cordova/PhoneGap.
 */
package com.synconset;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;

import static android.app.Activity.RESULT_OK;

public class ImagePicker extends CordovaPlugin {
    public static String TAG = "ImagePicker";

    private CallbackContext callbackContext;
    private JSONObject params;
    private int maxImages;
    private int desiredWidth = 0;
    private int desiredHeight = 0;
    private int quality = 100;
    private ArrayList<String> preSelectedAssets = new ArrayList<String>();
    public static final int PERMISSION_DENIED_ERROR = 20;
    public static final int SAVE_TO_ALBUM_SEC = 1;
    private Intent intent;

    protected final static String[] permissions = {Manifest.permission.READ_EXTERNAL_STORAGE};


    protected void getReadPermission(int requestCode) {
        cordova.requestPermission(this, requestCode, Manifest.permission.READ_EXTERNAL_STORAGE);
    }

    public boolean execute(String action, final JSONArray args, final CallbackContext callbackContext) throws JSONException {
        if (callbackContext == null)
            return false;

        this.callbackContext = callbackContext;

        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        ActivityManager activityManager = (ActivityManager) this.cordova.getActivity().getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
        activityManager.getMemoryInfo(mi);
        long totalMegs = mi.totalMem / 1048576L;
        System.out.println("[NIX] totalMegs: " + totalMegs);

        if (action.equals("getPictures")) {
            this.params = args.getJSONObject(0);
            this.desiredWidth = 0;
            this.desiredHeight = 0;
            this.quality = 100;
            if (this.params.has("maximumImagesCount")) {

                this.maxImages = this.params.getInt("maximumImagesCount");
            } else {
                this.maxImages = 100;
            }
			System.out.println("[NIX] Maximum images: " + this.maxImages);
            if (this.params.has("width")) {
                this.desiredWidth = this.params.getInt("width");
            }
            if (this.params.has("height")) {
                this.desiredHeight = this.params.getInt("height");
            }
            if (this.params.has("quality")) {
                this.quality = this.params.getInt("quality");
            }
//			if (this.params.has("assets")) {
//				JSONArray assets = this.params.getJSONArray("assets");
//				if (assets != null) {
//					for(int i=0; i < assets.length(); i++) {
//						this.preSelectedAssets.add(assets.get(i).toString());
//					}
//				}
//			}
//			intent.putExtra("MAX_IMAGES", this.maxImages);
//			intent.putExtra("WIDTH", this.desiredWidth);
//			intent.putExtra("HEIGHT", this.desiredHeight);
//			intent.putExtra("QUALITY", this.quality);
//			intent.putExtra("PRE_SELECTED_ASSETS", this.preSelectedAssets);
//			intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
//			if(cordova.hasPermission(permissions[0])) {
//				this.cordova.startActivityForResult(this, intent, 0);
//			} else if (this.cordova != null) {
//				getReadPermission(SAVE_TO_ALBUM_SEC);
//			}

            intent = new Intent(cordova.getActivity(), ImagePickerPluginActivity.class);
            intent.putExtra("WIDTH", this.desiredWidth);
            intent.putExtra("HEIGHT", this.desiredHeight);
            intent.putExtra("QUALITY", this.quality);
            intent.putExtra("MAX_IMAGES", this.maxImages);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            if (cordova.hasPermission(permissions[0])) {
                this.cordova.startActivityForResult(this, intent, ImagePickerPluginActivity.REQUEST_IMAGEPICKER);
            } else if (this.cordova != null) {
                getReadPermission(SAVE_TO_ALBUM_SEC);
            }


        } else if (action.equals("cleanupTempFiles")) {
            cleanupTempFiles();
        }
        return true;
    }


    private void cleanupTempFiles() {
        File filePath = new File(System.getProperty("java.io.tmpdir"));
        for (final File fileEntry : filePath.listFiles()) {
            System.out.println("File Entry: " + fileEntry);
            fileEntry.delete();
        }
        this.callbackContext.success(new JSONObject());
    }

    public void onRequestPermissionResult(int requestCode, String[] permissions,
                                          int[] grantResults) throws JSONException {
        for (int r : grantResults) {
            if (r == PackageManager.PERMISSION_DENIED) {
                this.callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, PERMISSION_DENIED_ERROR));
                return;
            }
        }
        switch (requestCode) {
            case SAVE_TO_ALBUM_SEC:
                if (intent != null) {
                    this.cordova.startActivityForResult(this, intent, ImagePickerPluginActivity.REQUEST_IMAGEPICKER);
//					this.cordova.startActivityForResult(this, intent, 0);
                }
                break;
        }
    }


    public Bundle onSaveInstanceState() {
        Bundle state = new Bundle();

        state.putInt("maxImages", this.maxImages);
        state.putInt("desiredWidth", this.desiredWidth);
        state.putInt("desiredHeight", this.desiredHeight);
        state.putInt("quality", this.quality);
        state.putStringArrayList("preSelectedAssets", this.preSelectedAssets);

        return state;
    }

    public void onRestoreStateForActivityResult(Bundle state, CallbackContext callbackContext) {
        this.maxImages = state.getInt("maxImages");
        this.desiredWidth = state.getInt("desiredWidth");
        this.desiredHeight = state.getInt("desiredHeight");
        this.quality = state.getInt("quality");
        this.preSelectedAssets = state.getStringArrayList("preSelectedAssets");

        this.callbackContext = callbackContext;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK && data != null && requestCode == ImagePickerPluginActivity.REQUEST_IMAGEPICKER) {
            ArrayList<String> fileNames = data.getStringArrayListExtra("MULTIPLEFILENAMES");
            this.preSelectedAssets = data.getStringArrayListExtra("SELECTED_ASSETS");
            ArrayList<String> invalidImages = data.getStringArrayListExtra("INVALID_IMAGES");
            JSONArray jsFileNames = new JSONArray(fileNames);
            JSONArray jsPreSelectedAssets = new JSONArray(this.preSelectedAssets);
            JSONObject res = new JSONObject();
//
            try {
                res.put("images", jsFileNames);
                res.put("preSelectedAssets", jsPreSelectedAssets);
                res.put("maxImages", maxImages);
                if (invalidImages != null && invalidImages.size() > 0)
                    res.put("invalidImages", new JSONArray(invalidImages));
            } catch (JSONException e) {
                e.printStackTrace();
            }

//			if(resultCode == RESULT_OK) {
//				if(requestCode == Picker.PICK_IMAGE_DEVICE) {
//					imagePicker.submit(data);
//				}
//			}

            this.callbackContext.success(res);
        } else if (resultCode == Activity.RESULT_CANCELED && data != null) {
            String error = data.getStringExtra("ERRORMESSAGE");
            if (error == null)
                this.callbackContext.error("No images selected");
            this.callbackContext.error(error);
        } else if (resultCode == Activity.RESULT_CANCELED) {
            JSONArray res = new JSONArray();
            if (res == null)
                this.callbackContext.error("No images selected");
            this.callbackContext.success(res);
        } else {
            this.callbackContext.error("No images selected");
        }
    }
}

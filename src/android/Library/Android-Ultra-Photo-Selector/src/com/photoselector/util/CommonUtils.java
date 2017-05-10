package com.photoselector.util;

import com.photoselector.ui.PhotoSelectorActivity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore.Images.ImageColumns;
import android.util.DisplayMetrics;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

/**
 * 通锟矫癸拷锟斤拷锟斤拷
 * 
 * @author chenww
 * 
 */
public class CommonUtils {

	/**
	 * 锟斤拷锟斤拷activity
	 */
	public static void launchActivity(Context context, Class<?> activity) {
		Intent intent = new Intent(context, activity);
		intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
		context.startActivity(intent);
	}

	/**
	 * 锟斤拷锟斤拷activity(锟斤拷锟斤拷锟�)
	 */
	public static void launchActivity(Context context, Class<?> activity, Bundle bundle) {
		Intent intent = new Intent(context, activity);
		intent.putExtras(bundle);
		intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
		context.startActivity(intent);
	}

	/**
	 * 锟斤拷锟斤拷activity(锟斤拷锟斤拷锟�)
	 */
	public static void launchActivity(Context context, Class<?> activity, String key, int value) {
		Bundle bundle = new Bundle();
		bundle.putInt(key, value);
		launchActivity(context, activity, bundle);
	}

	public static void launchActivity(Context context, Class<?> activity, String key, String value) {
		Bundle bundle = new Bundle();
		bundle.putString(key, value);
		launchActivity(context, activity, bundle);
	}

	public static void launchActivityForResult(Activity context, Class<?> activity, int requestCode) {
		Intent intent = new Intent(context, activity);
		intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
		context.startActivityForResult(intent, requestCode);
	}
	
	public static void launchActivityForResult(Activity context, Class<?> activity, int requestCode, int maxImage) {
		Intent intent = new Intent(context, activity);
		intent.putExtra(PhotoSelectorActivity.KEY_MAX, maxImage);
		intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
		context.startActivityForResult(intent, requestCode);
	}

	public static void launchActivityForResult(Activity activity, Intent intent, int requestCode) {
		activity.startActivityForResult(intent, requestCode);
	}

	/** 锟斤拷锟斤拷一锟斤拷锟斤拷锟斤拷 */
	public static void launchService(Context context, Class<?> service) {
		Intent intent = new Intent(context, service);
		context.startService(intent);
	}

	public static void stopService(Context context, Class<?> service) {
		Intent intent = new Intent(context, service);
		context.stopService(intent);
	}

	/**
	 * 锟叫讹拷锟街凤拷锟角凤拷为锟斤拷
	 * @param text
	 * @return true null false !null
	 */
	public static boolean isNull(CharSequence text) {
		if (text == null || "".equals(text.toString().trim()) || "null".equals(text))
			return true;
		return false;
	}

	/** 锟斤拷取锟斤拷幕锟斤拷锟� */
	public static int getWidthPixels(Activity activity) {
		DisplayMetrics dm = new DisplayMetrics();
		activity.getWindowManager().getDefaultDisplay().getMetrics(dm);
		return dm.widthPixels;
	}

	/** 锟斤拷取锟斤拷幕锟竭讹拷 */
	public static int getHeightPixels(Activity activity) {
		DisplayMetrics dm = new DisplayMetrics();
		activity.getWindowManager().getDefaultDisplay().getMetrics(dm);
		return dm.heightPixels;
	}

	/** 通锟斤拷Uri锟斤拷取图片路锟斤拷 */
	public static String query(Context context, Uri uri) {
		Cursor cursor = context.getContentResolver().query(uri, new String[] { ImageColumns.DATA }, null, null, null);
		cursor.moveToNext();
		return cursor.getString(cursor.getColumnIndex(ImageColumns.DATA));
	}

	//ImageLoader - Configuration
	public static DisplayImageOptions displayImageOptions;
	public static ImageLoaderConfiguration config;


}
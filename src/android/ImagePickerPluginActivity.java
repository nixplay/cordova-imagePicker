package com.synconset;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.kaopiz.kprogresshud.KProgressHUD;
import com.kbeanie.multipicker.api.CacheLocation;
import com.kbeanie.multipicker.api.Picker;
import com.kbeanie.multipicker.api.callbacks.ImagePickerCallback;
import com.kbeanie.multipicker.api.entity.ChosenImage;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ImagePickerPluginActivity extends Activity {
    private static final String TAG = ImagePickerPluginActivity.class.getSimpleName();
    public static final int REQUEST_IMAGEPICKER = 0x41;
    private static final String KEY_FILES = "MULTIPLEFILENAMES";
    private com.kbeanie.multipicker.api.ImagePicker imagePicker;
    private KProgressHUD kProgressHUD;
    private int width;
    private int height;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        imagePicker = new com.kbeanie.multipicker.api.ImagePicker(this);
        Bundle bundle = getIntent().getExtras();

        this.width = bundle.getInt("WIDTH");
        this.height = bundle.getInt("HEIGHT");

        imagePicker.setImagePickerCallback(new ImagePickerCallback() {
            @Override
            public void onImagesChosen(List<ChosenImage> images) {
                //dismiss dialog
                // Display images
                kProgressHUD.dismiss();
                ArrayList<String> imageList = new ArrayList<String>();
                for (ChosenImage file : images) {
                    if (file.getQueryUri().contains("com.google.android.apps.photos.contentprovider")) {
                        imageList.add(Uri.fromFile(new File(file.getOriginalPath())).toString());
                    } else {
                        imageList.add(file.getQueryUri());
                    }
//
                }
                Bundle conData = new Bundle();
                conData.putStringArrayList(KEY_FILES, imageList);

                Intent intent = new Intent();
                intent.putExtras(conData);
                setResult(RESULT_OK, intent);
                finishActivity(REQUEST_IMAGEPICKER);
                finish();

            }

            @Override
            public void onError(String message) {
                // Do error handling
                Log.d(TAG, message);
            }
        });
        if (this.width > 0 && this.height > 0) {
            imagePicker.ensureMaxSize(this.width, this.height);
        }
        imagePicker.allowMultiple(); // Default is false
        imagePicker.shouldGenerateMetadata(true);
        imagePicker.setCacheLocation(CacheLocation.INTERNAL_APP_DIR);//use internal cache directory
        imagePicker.shouldGenerateThumbnails(false); // Default is true
        imagePicker.pickImage();

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK && data != null) {
            if (requestCode == Picker.PICK_IMAGE_DEVICE) {
//                show dialog
                kProgressHUD = KProgressHUD.create(ImagePickerPluginActivity.this)
                        .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
                        .setDetailsLabel("Downloading data")
                        .setCancellable(false)
                        .setAnimationSpeed(2)
                        .setDimAmount(0.5f)
                        .show();

                imagePicker.submit(data);
            } else {
                Intent intent = new Intent();
                setResult(RESULT_CANCELED, intent);
                finishActivity(REQUEST_IMAGEPICKER);
                finish();

            }
        } else {
            Intent intent = new Intent();
            setResult(RESULT_CANCELED, intent);
            finish();
        }
    }
}

package com.synconset;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.esafirm.imagepicker.model.Image;
import com.kaopiz.kprogresshud.KProgressHUD;

import java.util.ArrayList;

public class ImagePickerPluginActivity extends Activity {
    private static final String TAG = ImagePickerPluginActivity.class.getSimpleName();
    public static final int REQUEST_IMAGEPICKER = 0x41;
    private static final String KEY_FILES = "MULTIPLEFILENAMES";
    private static final int REQUEST_CODE_PICKER = 0x111;
    private KProgressHUD kProgressHUD;
    private int width;
    private int height;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        imagePicker = new com.kbeanie.multipicker.api.ImagePicker(this);

        Bundle bundle = getIntent().getExtras();

        this.width = bundle.getInt("WIDTH");
        this.height = bundle.getInt("HEIGHT");
        com.esafirm.imagepicker.features.ImagePicker
                .create(this)
                .returnAfterFirst(false)
                .folderMode(true) // folder mode (false by default)
                .folderTitle("Folder") // folder selection title
                .imageTitle("Tap to select") // image selection title
                .multi() // multi mode (default mode)
                .limit(1000) // max images can be selected (99 by default)
                .showCamera(true) // show camera or not (true by default)
                .imageDirectory("Camera") // directory name for captured image  ("Camera" folder by default)
                .enableLog(false) // disabling log

                .start(REQUEST_CODE_PICKER); // start image picker activity with request code


        /*imagePicker.setImagePickerCallback(new ImagePickerCallback() {
            @Override
            public void onImagesChosen(List<ChosenImage> images) {
                //dismiss dialog
                // Display images
                kProgressHUD.dismiss();
                ArrayList<String> imageList = new ArrayList<String>();
                for (ChosenImage file : images) {
                    //resolve content parsing
                    if (file.getQueryUri().contains("com.google.android.apps.photos.contentprovider") || file.getQueryUri().contains("com.google.android.apps.docs.storage.legacy") ) {
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
        imagePicker.pickImage();*/

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_PICKER && resultCode == RESULT_OK && data != null) {
            ArrayList<String> imageList = new ArrayList<String>();
            ArrayList<Image> images = (ArrayList<Image>)  com.esafirm.imagepicker.features.ImagePicker.getImages(data);
            for (int i = 0 ; i < images.size() ; i++){
                imageList.add(images.get(i).getPath());
            }
            Bundle conData = new Bundle();
            conData.putStringArrayList(KEY_FILES, imageList);

            Intent intent = new Intent();
            intent.putExtras(conData);
            setResult(RESULT_OK, intent);
            finishActivity(REQUEST_IMAGEPICKER);
            finish();
        }else{
            Intent intent = new Intent();

            setResult(RESULT_CANCELED, intent);
            finishActivity(REQUEST_IMAGEPICKER);
            finish();
        }
        /*if (resultCode == RESULT_OK && data != null) {
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
        }*/
    }
}

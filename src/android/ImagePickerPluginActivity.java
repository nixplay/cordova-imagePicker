package com.synconset;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.kaopiz.kprogresshud.KProgressHUD;

import java.util.ArrayList;

import me.iwf.photopicker.PhotoPicker;
import me.iwf.photopicker.PhotoPreview;

public class ImagePickerPluginActivity extends Activity {
    private static final String TAG = ImagePickerPluginActivity.class.getSimpleName();
    public static final int REQUEST_IMAGEPICKER = 0x41;
    private static final String KEY_FILES = "MULTIPLEFILENAMES";
    private static final int REQUEST_CODE_PICKER = 0x111;
    private KProgressHUD kProgressHUD;
    private int width;
    private int height;
    FakeR fakeR;
    private int maxImages;
    private ArrayList<String> preselectedAsset;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        imagePicker = new com.kbeanie.multipicker.api.ImagePicker(this);
        fakeR = new FakeR(this);
        Bundle bundle = getIntent().getExtras();

        this.width = bundle.getInt("WIDTH");
        this.height = bundle.getInt("HEIGHT");
        this.maxImages = bundle.getInt("MAX_IMAGES");
        this.preselectedAsset = bundle.getStringArrayList("preselectedAsset");

        if(this.preselectedAsset == null){
            preselectedAsset = new ArrayList<String>();
        }

        PhotoPicker.builder()
                .setPhotoCount(this.maxImages)
                .setGridColumnCount(4)
                .setShowCamera(true)
                .setPreviewEnabled(false)
                .setShowGif(false)
                .setSelected(this.preselectedAsset)
                .start(ImagePickerPluginActivity.this);
//        com.esafirm.imagepicker.features.ImagePicker
//                .create(this)
//                .returnAfterFirst(false)
////                .folderMode(true) // folder mode (false by default)
////                .folderTitle(getString(fakeR.getId("string","ALBUM"))) // folder selection title
//                .imageTitle("Tap to select") // image selection title
//                .multi() // multi mode (default mode)
//                .limit(maxImages) // max images can be selected (99 by default)
//                .showCamera(true) // show camera or not (true by default)
////                .enableLog(false) // disabling log
//                .theme(fakeR.getId("style","ImagePickerTheme"))
//                .start(REQUEST_CODE_PICKER); // start image picker activity with request code


    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK &&
                (requestCode == PhotoPicker.REQUEST_CODE || requestCode == PhotoPreview.REQUEST_CODE)) {
            ArrayList<String> photos = null;
            if (data != null) {
                photos = data.getStringArrayListExtra(PhotoPicker.KEY_SELECTED_PHOTOS);
            }
//            ArrayList<String> imageList = new ArrayList<String>();
//            ArrayList<Image> images = (ArrayList<Image>)  com.esafirm.imagepicker.features.ImagePicker.getImages(data);
//            for (int i = 0 ; i < images.size() ; i++){
//                imageList.add(images.get(i).getPath());
//            }
            Bundle conData = new Bundle();
            conData.putStringArrayList (KEY_FILES, photos);

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

    }
}

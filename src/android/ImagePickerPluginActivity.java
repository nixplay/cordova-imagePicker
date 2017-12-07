package com.synconset;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;

import com.kaopiz.kprogresshud.KProgressHUD;
import com.zhihu.matisse.Matisse;
import com.zhihu.matisse.MimeType;
import com.zhihu.matisse.engine.impl.GlideEngine;
import com.zhihu.matisse.internal.entity.CaptureStrategy;
import com.zhihu.matisse.internal.utils.PathUtils;

import java.util.ArrayList;
import java.util.List;

public class ImagePickerPluginActivity extends Activity {
    private static final String TAG = ImagePickerPluginActivity.class.getSimpleName();
    public static final int REQUEST_IMAGEPICKER = 0x41;
    private static final String KEY_FILES = "MULTIPLEFILENAMES";
    private static final int REQUEST_CODE_PICKER = 0x111;
    private static final int REQUEST_CODE_CHOOSE = 0x111;
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
        Matisse.from(ImagePickerPluginActivity.this)
                .choose(MimeType.of(
                        MimeType.JPEG,
                        MimeType.PNG

                ), false)
                .countable(true)
                .capture(true)
                .captureStrategy(
                        new CaptureStrategy(true, getApplication().getPackageName()+".fileprovider"))
                .maxSelectable(this.maxImages)
                .gridExpectedSize(.gridExpectedSize(convertDpToPixel(120,ImagePickerPluginActivity.this)))
                .restrictOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
                .thumbnailScale(0.85f)
                .imageEngine(new GlideEngine())
                .enablePreview(false)
                .showUseOrigin(false)
                .forResult(REQUEST_CODE_CHOOSE);
//        PhotoPicker.builder()
//                .setPhotoCount(this.maxImages)
//                .setGridColumnCount(4)
//                .setShowCamera(true)
//                .setPreviewEnabled(false)
//                .setShowGif(false)
//                .setSelected(this.preselectedAsset)
//                .start(ImagePickerPluginActivity.this);


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
//        if (resultCode == RESULT_OK && (requestCode == PhotoPicker.REQUEST_CODE || requestCode == PhotoPreview.REQUEST_CODE)) {

        if (resultCode == RESULT_OK && requestCode == REQUEST_CODE_CHOOSE){
            ArrayList<String> photos = new ArrayList<String>();
            List<Uri> result = Matisse.obtainResult(data);

            for (int i = 0 ; i < result.size() ; i++){
                photos.add(PathUtils.getPath(getApplicationContext(),result.get(i)));
            }
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
    /**
     * This method converts dp unit to equivalent pixels, depending on device density.
     *
     * @param dp A value in dp (density independent pixels) unit. Which we need to convert into pixels
     * @param context Context to get resources and device specific display metrics
     * @return A float value to represent px equivalent to dp depending on device density
     */
    public static float convertDpToPixel(float dp, Context context){
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        float px = dp * ((float)metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
        return px;
    }
}

package com.photoselector.ui;
/**
 * 
 * @author Aizaz AZ
 *
 */
import java.util.List;

import android.os.Bundle;

import com.photoselector.domain.PhotoSelectorDomain;
import com.photoselector.model.PhotoModel;
import com.photoselector.ui.PhotoSelectorActivity.OnLocalReccentListener;
import com.photoselector.util.CommonUtils;

public class PhotoPreviewActivity extends BasePhotoPreviewActivity implements OnLocalReccentListener {

	private PhotoSelectorDomain photoSelectorDomain;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		photoSelectorDomain = new PhotoSelectorDomain(getApplicationContext());

		init(getIntent().getExtras());
	}

	@SuppressWarnings("unchecked")
	protected void init(Bundle extras) {
		if (extras == null)
			return;

		if (extras.containsKey("photos")) { // 棰勮�鍥剧墖
			photos = (List<PhotoModel>) extras.getSerializable("photos");
			current = extras.getInt("position", 0);
			updatePercent();
			bindData();
		} else if (extras.containsKey("album")) { // 鐐瑰嚮鍥剧墖鏌ョ湅
			String albumName = extras.getString("album"); // 鐩稿唽
			this.current = extras.getInt("position");
			if (!CommonUtils.isNull(albumName) && albumName.equals(PhotoSelectorActivity.RECCENT_PHOTO)) {
				photoSelectorDomain.getReccent(this);
			} else {
				photoSelectorDomain.getAlbum(albumName, this);
			}
		}
	}

	@Override
	public void onPhotoLoaded(List<PhotoModel> photos) {
		this.photos = photos;
		updatePercent();
		bindData(); // 鏇存柊鐣岄潰
	}

	@Override
	public void onPhotoAdded(List<PhotoModel> photos) {
		// TODO Auto-generated method stubthis.photos = photos;
		updatePercent();
		bindData();
	}

}

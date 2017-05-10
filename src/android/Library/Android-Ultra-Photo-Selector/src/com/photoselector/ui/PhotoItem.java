package com.photoselector.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.synconset.FakeR;;
import com.photoselector.model.PhotoModel;

/**
 * @author Aizaz AZ
 *
 */

public class PhotoItem extends LinearLayout implements OnCheckedChangeListener,
		OnLongClickListener, View.OnClickListener {

	private FakeR fakeR;
	private ImageView ivPhoto;
	private CheckBox cbPhoto;
	private onPhotoItemCheckedListener listener;
	private PhotoModel photo;
	private boolean isCheckAll;
	private onItemClickListener l;
	private int position;	

	private PhotoItem(Context context) {
		super(context);
		fakeR = new FakeR(context);
	}

	public PhotoItem(Context context, onPhotoItemCheckedListener listener) {
		this(context);
		LayoutInflater.from(context).inflate(fakeR.getId("layout", "layout_photoitem"), this,
				true);
		this.listener = listener;

		setOnLongClickListener(this);

		ivPhoto = (ImageView) findViewById(fakeR.getId("id", "iv_photo_lpsi"));
		cbPhoto = (CheckBox) findViewById(fakeR.getId("id", "cb_photo_lpsi"));

		cbPhoto.setOnCheckedChangeListener(this); // CheckBox閫変腑鐘舵�佹敼鍙樼洃鍚�櫒
		ivPhoto.setOnClickListener(this);
	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

		if (!isCheckAll) {
			listener.onCheckedChanged(photo, buttonView, isChecked); // 璋冪敤涓荤晫闈㈠洖璋冨嚱鏁�
		}
		
		// 璁╁浘鐗囧彉鏆楁垨鑰呭彉浜�
		if (buttonView.isChecked()) {
			setDrawingable();
			ivPhoto.setColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY);
			photo.setChecked(true);
		} else {
			ivPhoto.clearColorFilter();
			photo.setChecked(false);
		}
	}

	/** 璁剧疆璺�緞涓嬬殑鍥剧墖瀵瑰簲鐨勭缉鐣ュ浘 */
	public void setImageDrawable(final PhotoModel photo) {
		this.photo = photo;
		// You may need this setting form some custom ROM(s)
		/*
		 * new Handler().postDelayed(new Runnable() {
		 * 
		 * @Override public void run() { ImageLoader.getInstance().displayImage(
		 * "file://" + photo.getOriginalPath(), ivPhoto); } }, new
		 * Random().nextInt(10));
		 */
		ivPhoto.setRotation(photo.getRotation());
		ImageLoader.getInstance().displayImage(
				"file://" + photo.getOriginalPath(), ivPhoto);
	}

	private void setDrawingable() {
		ivPhoto.setDrawingCacheEnabled(true);
		ivPhoto.buildDrawingCache();
	}

	@Override
	public void setSelected(boolean selected) {
		if (photo == null) {
			return;
		}
		isCheckAll = true;
		cbPhoto.setChecked(selected);
		isCheckAll = false;
	}

	public void setOnClickListener(onItemClickListener l, int position) {
		this.l = l;
		this.position = position;
	}

	@Override
	public void onClick(View v) {

		if (v.getId() == fakeR.getId("id", "iv_photo_lpsi")){

			if (!photo.isChecked()) {
				listener.onCheckedChanged(photo, cbPhoto, true); // 璋冪敤涓荤晫闈㈠洖璋冨嚱鏁�

				setDrawingable();
				ivPhoto.setColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY);
				photo.setChecked(true);
				cbPhoto.setChecked(true);
			}
			else {
				listener.onCheckedChanged(photo, cbPhoto, false); // 璋冪敤涓荤晫闈㈠洖璋冨嚱鏁�

				ivPhoto.clearColorFilter();
				photo.setChecked(false);
				cbPhoto.setChecked(false);
			}
		}
	}

	// @Override
	// public void
	// onClick(View v) {
	// if (l != null)
	// l.onItemClick(position);
	// }

	/** 鍥剧墖Item閫変腑浜嬩欢鐩戝惉鍣� */
	public static interface onPhotoItemCheckedListener {
		public void onCheckedChanged(PhotoModel photoModel,
				CompoundButton buttonView, boolean isChecked);
	}

	/** 鍥剧墖鐐瑰嚮浜嬩欢 */
	public interface onItemClickListener {
		public void onItemClick(int position);
	}

	@Override
	public boolean onLongClick(View v) {
		if (l != null)
			l.onItemClick(position);
		return true;
	}

}

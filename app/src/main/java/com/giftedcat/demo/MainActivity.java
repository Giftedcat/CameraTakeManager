package com.giftedcat.demo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.giftedcat.cameratakelib.CameraTakeManager;
import com.giftedcat.cameratakelib.listener.CameraTakeListener;
import com.giftedcat.cameratakelib.utils.LogUtil;

import java.io.File;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import cn.finalteam.galleryfinal.permission.EasyPermissions;

public class MainActivity extends AppCompatActivity {

    Unbinder unbinder;
    private Context mContext;

    /** 权限相关*/
    private static final int GETLOCATION = 100;
    private String[] perms;
    private Handler permissionsHandler = new Handler();

    @BindView(R.id.surfaceview)
    SurfaceView previewView;
    @BindView(R.id.img_pic)
    ImageView imgPic;
    @BindView(R.id.tv_pic_dir)
    TextView tvPicDir;

    CameraTakeManager manager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        unbinder = ButterKnife.bind(this);

        mContext = this;
        perms = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA};
        checkPermission();
    }

    public void checkPermission() {
        //判断是否有相关权限，并申请权限
        if (EasyPermissions.hasPermissions(mContext, perms)) {
            permissionsHandler.post(new Runnable() {
                @Override
                public void run() {
                    init();
                }
            });
        } else {
            ActivityCompat.requestPermissions(MainActivity.this, perms, GETLOCATION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        init();
    }

    private void init(){
        manager = new CameraTakeManager(this, previewView, new CameraTakeListener() {
            @Override
            public void onSuccess(File bitmapFile, Bitmap mBitmap) {
                imgPic.setImageBitmap(mBitmap);
                tvPicDir.setText("图片路径：" + bitmapFile.getPath());
            }

            @Override
             public void onFail(String error) {
                LogUtil.e(error);
            }
        });
    }

    @OnClick({R.id.btn_take_photo})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_take_photo:
                /** 点击拍照获取照片*/
                manager.takePhoto();
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unbinder.unbind();
        manager.destroy();
    }
}

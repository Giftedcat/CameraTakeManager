package com.giftedcat.cameratakelib;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.view.Surface;
import android.view.SurfaceHolder;

import com.giftedcat.cameratakelib.listener.CameraTakeListener;
import com.giftedcat.cameratakelib.utils.FileUtil;
import com.giftedcat.cameratakelib.utils.LogUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;

import top.zibin.luban.OnCompressListener;

public class SurfaceViewCallback implements SurfaceHolder.Callback{

    private Activity activity;

    boolean previewing;

    private boolean hasSurface;

    Camera mCamera;

    int mCurrentCamIndex = 0;

    /** 为true时则开始捕捉照片*/
    boolean canTake;

    /** 拍照回调接口*/
    CameraTakeListener listener;

    public SurfaceViewCallback(Activity activity, CameraTakeListener listener) {
        previewing = false;
        hasSurface = false;

        this.activity = activity;
        this.listener = listener;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (!hasSurface) {
            hasSurface = true;
            mCamera = openFrontFacingCameraGingerbread();

            mCamera.setPreviewCallback(new Camera.PreviewCallback() {
                @Override
                public void onPreviewFrame(byte[] bytes, Camera camera) {
                    LogUtil.i("onPreviewFrame " + canTake);
                    if (canTake) {
                        getSurfacePic(bytes, camera);
                        canTake = false;
                    }
                }
            });
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (previewing) {
            mCamera.stopPreview();
            previewing = false;
        }

        try {
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
            previewing = true;
            setCameraDisplayOrientation(activity, mCurrentCamIndex, mCamera);
        } catch (Exception e) {
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (!previewing)
            return;
        holder.removeCallback(this);
        mCamera.setPreviewCallback(null);
        mCamera.stopPreview();
        mCamera.lock();
        mCamera.release();
        mCamera = null;
    }

    /**
     * 设置照相机播放的方向
     * */
    private void setCameraDisplayOrientation(Activity activity, int cameraId, Camera camera) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();

        /** 度图片顺时针旋转的角度。有效值为0、90、180和270*/
        /** 起始位置为0（横向）*/
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }
        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {
            /** 背面*/
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }

    /**
     * 打开摄像头面板
     * */
    private Camera openFrontFacingCameraGingerbread() {
        int cameraCount = 0;
        Camera cam = null;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        cameraCount = Camera.getNumberOfCameras();

        for (int camIdx = 0; camIdx < cameraCount; camIdx++) {
            Camera.getCameraInfo(camIdx, cameraInfo);
            try {
                cam = Camera.open(camIdx);
                mCurrentCamIndex = camIdx;
            } catch (RuntimeException e) {
                LogUtil.e("Camera failed to open: " + e.getLocalizedMessage());
            }
        }

        return cam;
    }

    /**
     * 获取照片
     * */
    public void getSurfacePic(byte[] data, Camera camera) {
        Camera.Size size = camera.getParameters().getPreviewSize();
        YuvImage image = new YuvImage(data, ImageFormat.NV21, size.width, size.height, null);
        if (image != null) {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            image.compressToJpeg(new Rect(0, 0, size.width, size.height), 80, stream);

            Bitmap bmp = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size());
            /** 因为图片会放生旋转，因此要对图片进行旋转到和手机在一个方向上*/
            rotateMyBitmap(bmp);
        }
    }

    /**
     * 旋转图片
     */
    public void rotateMyBitmap(Bitmap bmp) {
        Matrix matrix = new Matrix();
        matrix.postRotate(0);

        Bitmap nbmp2 = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);

        saveMyBitmap(FileUtil.compressImage(nbmp2));

    }

    /**
     * 保存图片
     */
    public void saveMyBitmap(final Bitmap mBitmap) {
        if (FileUtil.getAvailableSize() > 512) {
            final File filePic = FileUtil.saveBitmap(mBitmap);
            if (filePic == null){
                /** 图片保存失败*/
                listener.onFail("图片保存失败");
                return;
            }
            FileUtil.compressPic(activity, filePic, new OnCompressListener() {
                @Override
                public void onStart() {
                    // TODO 压缩开始前调用，可以在方法内启动 loading UI
                }

                @Override
                public void onSuccess(File file) {
                    // TODO 压缩成功后调用，返回压缩后的图片文件
                    FileUtil.deleteFile(filePic);
                    listener.onSuccess(filePic, mBitmap);
                }

                @Override
                public void onError(Throwable e) {
                    // TODO 当压缩过程出现问题时调用
                    LogUtil.e("compressPic error");
                }
            });
        }else {
            listener.onFail("存储空间小于512M，图片无法正常保存");
        }
    }

    /**
     * 获取相机当前的照片
     * */
    public void takePhoto() {
        this.canTake = true;
    }

    /**
     * 释放
     * */
    public void destroy() {
        hasSurface = false;
    }

}

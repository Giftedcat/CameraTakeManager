# CameraTakeManager
Android使用SurfaceView+Camera实现无卡顿拍照

```
	allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
```
```
	dependencies {
	        implementation 'com.github.Giftedcat:CameraTakeManager:1.0.0'
	}
```

# 一、前言

最近前同事兼好基友老戴问我要我之前那个可以无卡顿拍照的demo，翻了一翻我的demo项目文件夹，有点真实

![demo目录](//p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/a6d61335403c460a8b4034d9206e5b2d~tplv-k3u1fbpfcp-zoom-1.image)

加上程序员都是不喜欢看自己以前写的代码的特性，于是决定将这个功能封装一下，方便他人当然也是方便自己

这个功能的出处还是以前我们做的刷卡考勤机，考勤的时候需要取到考勤图片，所以需要进行拍照

我一开始只是使用常规的Camera的takePicture方法来获取照片，但是实际应用中会出现，拍照速度缓慢

当时我还去现场看了一下使用情况，负责人跟我抱怨说拍摄速度很慢，给我演示了一下，确实是有一个卡顿，当然这很好理解，我理直气壮的跟她解释说你用手机拍照不也是会停顿一下的吗，手机需要聚焦啊，这个本来就是这样的

而负责人跟我说了某某考勤机拍照没有停顿啊，非常快的，我第一反应是，应该是windows的机子

结果看到发现人家的也是android的机器，于是便陷入了沉思

我们别的我不知道，但是抄袭这一招可是铁打的，于是乎便开始了对android相机的探索

* * *

正如我标题写的，为了实现我卡顿的拍照，使用的是SurfaceView+Camera的方式，通过相机的预览到surfaceView上，然后通过Camera的setPreviewCallback函数的回调来当前帧的图片，便不会有任何的卡顿

# 二、效果图

点击拍照之后，可以获取到当前帧的图片的BitMap对象，以及保存至本地的路径

![效果图](//p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/043d55f3bacd45cfbf78aeb33bc3bfa9~tplv-k3u1fbpfcp-zoom-1.image)

# 三、功能实现

#### （一）如何使用

首先先看一下布局文件
一个SurfaceView用来实时显示相机的画面
文本框和ImageView用来显示保存图片的路径和显示图片
```
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:orientation="vertical"
    android:layout_height="match_parent"
    tools:context=".MainActivity">

    <SurfaceView
        android:id="@+id/surfaceview"
        android:layout_width="320dp"
        android:layout_height="240dp" />

    <LinearLayout
        android:orientation="vertical"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content">

        <Button
            android:id="@+id/btn_take_photo"
            android:text="拍照"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content" />

        <TextView
            android:id="@+id/tv_pic_dir"
            android:text="图片路径："
            android:textSize="14sp"
            android:textColor="#000000"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content" />

        <ImageView
            android:background="#000000"
            android:id="@+id/img_pic"
            android:layout_width="160dp"
            android:layout_height="120dp" />

    </LinearLayout>

</LinearLayout>
```
使用已封装的CameraTakeManager，传入三个参数分别为activity对象，surfaceView控件对象，一个自定义的回调

回调的两个函数onSuccess中返回以保存的图片和BitMap对象

onFail返回失败信息
```
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
```
通过点击按钮用来获取照片，进入CameraTakeManager的回调
```
    @OnClick({R.id.btn_take_photo})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_take_photo:
                /** 点击拍照获取照片*/
                manager.takePhoto();
                break;
        }
    }
```

#### （二）实现的代码

这边自定义了一个SurfaceViewCallback类来实现SurfaceHolder.Callback接口

先是在surfaceChanged回调中开启camera的预览

```
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
```

在surfaceCreated回调中实现Camera的setPreviewCallback函数来获取相机每一帧的回调

用canTake变量来判断当前是否需要拍照，为true时，则取当前帧的图像，生成bitmap同时压缩一份图片文件到本地保存，并把数据回调给接口

实现拍照功能

```
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (!hasSurface) {
            hasSurface = true;
            mCamera = openFrontFacingCameraGingerbread();

            if (mCamera == null){
                listener.onFail("没有可用的摄像头");
                return;
            }
            mCamera.setPreviewCallback(new Camera.PreviewCallback() {
                @Override
                public void onPreviewFrame(byte[] bytes, Camera camera) {
                    if (canTake) {
                        getSurfacePic(bytes, camera);
                        canTake = false;
                    }
                }
            });
        }
    }
```

# 四、结语

到这里就算是完成了，技艺不精，希望大家多提提意见，我也会第一时间改良，记得给我点赞哦

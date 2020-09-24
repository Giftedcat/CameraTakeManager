package com.giftedcat.cameratakelib.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.os.StatFs;
import android.text.TextUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import top.zibin.luban.CompressionPredicate;
import top.zibin.luban.Luban;
import top.zibin.luban.OnCompressListener;

public class FileUtil {

    /**
     * 计算Sdcard的剩余大小
     *
     * @return MB
     */
    public static long getAvailableSize() {
        //sd卡大小相关变量
        StatFs statFs;
        File file = Environment.getExternalStorageDirectory();
        statFs = new StatFs(file.getPath());
        //获得Sdcard上每个block的size
        long blockSize = statFs.getBlockSize();
        //获取可供程序使用的Block数量
        long blockavailable = statFs.getAvailableBlocks();
        //计算标准大小使用：1024，当然使用1000也可以
        long blockavailableTotal = blockSize * blockavailable / 1024 / 1024;
        return blockavailableTotal;
    }

    /**
     * SDCard 总容量大小
     *
     * @return MB
     */
    public static long getTotalSize() {
        StatFs statFs;
        File file = Environment.getExternalStorageDirectory();
        statFs = new StatFs(file.getPath());
        //获得sdcard上 block的总数
        long blockCount = statFs.getBlockCount();
        //获得sdcard上每个block 的大小
        long blockSize = statFs.getBlockSize();
        //计算标准大小使用：1024，当然使用1000也可以
        long bookTotalSize = blockCount * blockSize / 1024 / 1024;
        return bookTotalSize;
    }

    /**
     * 保存bitmap到本地
     *
     * @param bitmap
     * @return
     */
    public static File saveBitmap(Bitmap bitmap) {
        String savePath;
        File filePic;
        if (Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED)) {
            savePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                    .toString()
                    + File.separator;
        } else {
            LogUtil.d("saveBitmap: 1return");
            return null;
        }
        try {
            filePic = new File(savePath + "Pic_" + System.currentTimeMillis() + ".jpg");
            if (!filePic.exists()) {
                filePic.getParentFile().mkdirs();
                filePic.createNewFile();
            }
            FileOutputStream fos = new FileOutputStream(filePic);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
            LogUtil.d("saveBitmap: 2return");
            return null;
        }
        LogUtil.d("saveBitmap: " + filePic.getAbsolutePath());
        return filePic;
    }

    /**
     * 压缩图片
     *
     * @param image
     * @return
     */
    public static Bitmap compressImage(Bitmap image) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        /** 质量压缩方法，这里100表示不压缩，把压缩后的数据存放到baos中*/
        image.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        /** 把压缩后的数据baos存放到ByteArrayInputStream中*/
        ByteArrayInputStream isBm = new ByteArrayInputStream(baos.toByteArray());
        /** 把ByteArrayInputStream数据生成图片*/
        Bitmap bitmap = BitmapFactory.decodeStream(isBm, null, null);
        return bitmap;
    }

    /**
     * 文件夹删除
     * */
    public static void deleteFile(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (int i = 0; i < files.length; i++) {
                File f = files[i];
                deleteFile(f);
            }
            file.delete();//如要保留文件夹，只删除文件，请注释这行
        } else if (file.exists()) {
            file.delete();
        }
    }

    /**
     * 压缩图片文件
     * */
    public static void compressPic(Context context, final File picFile, OnCompressListener listener){
        String savePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                .toString()
                + File.separator;
        Luban.with(context)
                .load(picFile.getPath())
                .ignoreBy(100)
                .setTargetDir(savePath)
                .filter(new CompressionPredicate() {
                    @Override
                    public boolean apply(String path) {
                        return !(TextUtils.isEmpty(path) || path.toLowerCase().endsWith(".gif"));
                    }
                })
                .setCompressListener(listener).launch();
    }

}

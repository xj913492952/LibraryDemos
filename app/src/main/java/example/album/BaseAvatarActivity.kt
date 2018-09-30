package example.album;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.util.Log;

import com.style.app.ConfigUtil;
import com.style.app.MyAction;
import com.style.app.Skip;
import com.style.base.BaseTitleBarActivity;
import com.style.dialog.SelAvatarDialog;
import com.style.framework.R;
import com.style.utils.BitmapUtil;
import com.style.utils.DeviceInfoUtil;
import com.style.utils.FileUtil;
import com.style.utils.PictureUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public abstract class BaseAvatarActivity : BaseTitleBarActivity() {

    private var photoFile: File? = null;
    private var isFromCamera: Boolean = false;
    private var dialog: SelAvatarDialog? = null;
    private var uri2: Uri? = null;

    fun showSelPicPopupWindow() {
        if (dialog == null) {
            dialog = SelAvatarDialog(this, R.style.Dialog_General);
            dialog?.setOnItemClickListener(object : SelAvatarDialog.OnItemClickListener {
                override fun OnClickCamera() {
                    if (!DeviceInfoUtil.isSDcardWritable()) {
                        showToast("sd卡不可用");
                        return;
                    }
                    photoFile = Skip.takePhoto(getContext() as Activity?, ConfigUtil.DIR_APP_IMAGE_CAMERA, "${System.currentTimeMillis()}.jpg");
                }

                override fun OnClickPhoto() {
                    if (!DeviceInfoUtil.isSDcardWritable()) {
                        showToast("sd卡不可用");
                        return;
                    }
                    Skip.selectPhoto(getContext() as Activity?);
                }

                override fun OnClickCancel() {

                }
            });
        }
        dialog?.show();
    }

    fun onAvatarCropped(savePath: String) {
        var f = File(savePath);
        logE(TAG, "文件大小   " + f.length() / 1024);
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) when (requestCode) {
            Skip.CODE_TAKE_CAMERA -> {// 拍照
                isFromCamera = true;
                if (photoFile!!.exists()) {
                    DeviceInfoUtil.notifyUpdateGallary(this, photoFile);// 通知系统更新相册
                    dealPicture(photoFile);
                } else {
                    showToast(R.string.File_does_not_exist);
                }
            }
            Skip.CODE_TAKE_ALBUM -> // 本地
                if (data != null) {
                    isFromCamera = false;
                    var uri = data.getData();
                    var fromFile = FileUtil.UriToFile(this, uri);
                    dealPicture(fromFile);
                } else {
                    showToast(R.string.File_does_not_exist);
                }

            Skip.CODE_PHOTO_CROP ->// 裁剪头像返回
                if (data != null) {
                    var degree = 0;
                    if (isFromCamera) {
                        if (photoFile!!.exists()) {
                            degree = PictureUtils.readPictureDegree(photoFile?.getAbsolutePath());
                            logE("life", "拍照后的角度：" + degree);
                        } else {
                            showToast(R.string.File_does_not_exist);
                        }
                    }
                    try {
                        var bitmap = BitmapFactory.decodeStream(this.getContentResolver().openInputStream(uri2));
                        if (isFromCamera && degree != 0) {// 旋转图片 动作
                            bitmap = BitmapUtil.rotaingImageView(bitmap, 0);
                        }
                        var savePath = ConfigUtil.DIR_CACHE + "/" + System.currentTimeMillis() + ".image";
                        //压缩图片
                        var b = BitmapUtil.compressImage(bitmap, 100);
                        // 保存图片
                        BitmapUtil.saveBitmap(savePath, b);
                        Log.e("PersonAvatar", "图片的路径是--->" + savePath);
                        onAvatarCropped(savePath);
                    } catch (e: FileNotFoundException) {
                        e.printStackTrace();
                    } catch (e: IOException) {
                        e.printStackTrace();
                    }
                }
        }
    }

    fun dealPicture(fromFile: File?) {
        //需要把原文件复制一份，否则会在原文件上操作
        var f = File(ConfigUtil.DIR_CACHE, "${System.currentTimeMillis()}.image");
        if (f.exists()) {
            f.delete();
        }
        if (!f.getParentFile().exists()) {
            f.getParentFile().mkdirs();
        }
        var isCopy = FileUtil.copyfile(fromFile, f, true);
        if (isCopy) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                uri2 = FileProvider.getUriForFile(getContext(), ConfigUtil.FILE_PROVIDER_AUTHORITY, f);
            } else {
                uri2 = Uri.fromFile(f);
            }

            Log.e("PersonAvatar", "本地图片的路径-->" + uri2);
            var i = getCropImageIntent(uri2);
            this.startActivityForResult(i, Skip.CODE_PHOTO_CROP);
        } else {
            showToast("复制图片出错");
        }
    }

    /**
     * 获取跳到裁剪图片界面的意图
     *
     * @param uri
     */
    fun getCropImageIntent(uri: Uri?): Intent {
        var intent = Intent("com.android.camera.action.CROP");
        // 声明需要的零时权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        }
        intent.setDataAndType(uri, "image/*");
        intent.putExtra("crop", "true");
        //裁剪框大小
        intent.putExtra("aspectX", 200);
        intent.putExtra("aspectY", 200);
        //保存图片的大小,一定不能比 aspectX aspectY大，否则会闪退
        intent.putExtra("outputX", 160);
        intent.putExtra("outputY", 160);
        intent.putExtra("scale", true);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
        //为true直接返回bitmap数据，但在
        intent.putExtra("return-data", false);
        intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
        intent.putExtra("noFaceDetection", true); // no face detection
        return intent;
    }
    /*方法1：如果你将return-data设置为“true”，你将会获得一个与内部数据关联的Action，并且bitmap以此方式返回：(Bitmap)extras.getParcelable("data")。注意：如果你最终要获取的图片非常大，那么此方法会给你带来麻烦，所以你要控制outputX和outputY保持在较小的尺寸。鉴于此原因，在我的代码中没有使用此方法（(Bitmap)extras.getParcelable("data")）。

    下面是CropImage.java的源码片段：

            1
// Return the cropped image directly or save it to the specified URI.
            2
    Bundle myExtras = getIntent().getExtras();
3
        if (myExtras != null && (myExtras.getParcelable("data") != null|| myExtras.getBoolean("return-data")))
            4
    {
        5
        Bundle extras = new Bundle();
        6
        extras.putParcelable("data", croppedImage);
        7
        setResult(RESULT_OK,(new Intent()).setAction("inline-data").putExtras(extras));
        8
        finish();
        9
    }          方法2： 如果你将return-data设置为“false”，那么在onActivityResult的Intent数据中你将不会接收到任何Bitmap，相反，你需要将MediaStore.EXTRA_OUTPUT关联到一个Uri，此Uri是用来存放Bitmap的。
    但是还有一些条件，首先你需要有一个短暂的与此Uri相关联的文件地址，当然这不是个大问题（除非是那些没有sdcard的设备）。*/
}
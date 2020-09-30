package com.lxfly2000.utilities;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.provider.MediaStore;
import androidx.appcompat.app.AlertDialog;

import java.io.IOException;

public class AndroidUtility {
    //检查权限，有返回true，无返回false并显示提示信息并关闭当前Activity
    public static boolean CheckPermissionWithFinishOnDenied(final Activity activity, String permission, String deniedMessage){
        if(activity.checkCallingOrSelfPermission(permission)!= PackageManager.PERMISSION_GRANTED){
            new AlertDialog.Builder(activity)
                    .setMessage(deniedMessage)
                    .setPositiveButton(android.R.string.ok,null)
                    .setOnDismissListener(dialogInterface -> activity.finish()).show();
            return false;
        }
        return true;
    }

    public static void MessageBox(Context activity, String msg){
        MessageBox(activity,msg,null);
    }
    public static void MessageBox(Context activity, String msg, String title){
        new AlertDialog.Builder(activity)
                .setTitle(title)
                .setMessage(msg)
                .setPositiveButton(android.R.string.ok,null)
                .show();
    }

    public static void OpenUri(Context ctx,String uriString)throws ActivityNotFoundException {
        ctx.startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse(uriString)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }

    public static void KillProcess(Context ctx,String packageName){
        ((ActivityManager)ctx.getSystemService(Context.ACTIVITY_SERVICE)).killBackgroundProcesses(packageName);
    }

    public static void StartApplication(Context ctx,String packageName)throws NullPointerException {
        ctx.startActivity(ctx.getPackageManager().getLaunchIntentForPackage(packageName));
    }

    public static int GetMediaFileDuration(String path){
        MediaPlayer player=new MediaPlayer();
        try{
            player.setDataSource(path);
            player.prepare();
            return player.getDuration();
        }catch (IOException e){
            return 0;
        }
    }

    /**
     * Gets the content:// URI from the given corresponding path to a file
     *
     * @param context
     * @param imageFile
     * @return content Uri
     */
    public static Uri GetImageContentUri(Context context, java.io.File imageFile) {
        String filePath = imageFile.getAbsolutePath();
        Cursor cursor = context.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                new String[] { MediaStore.Images.Media._ID }, MediaStore.Images.Media.DATA + "=? ",
                new String[] { filePath }, null);
        if (cursor != null && cursor.moveToFirst()) {
            int id = cursor.getInt(cursor.getColumnIndex(MediaStore.MediaColumns._ID));
            Uri baseUri = Uri.parse("content://media/external/images/media");
            return Uri.withAppendedPath(baseUri, "" + id);
        } else {
            if (imageFile.exists()) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DATA, filePath);
                return context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            } else {
                return null;
            }
        }
    }
}

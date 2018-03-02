package com.lxfly2000.utilities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.support.v7.app.AppCompatActivity;

public class AndroidUtility {
    //检查权限，有返回true，无返回false并显示提示信息并关闭当前Activity
    public static boolean CheckPermissionWithFinishOnDenied(final Activity activity, String permission, String deniedMessage){
        if(activity.checkCallingOrSelfPermission(permission)!= PackageManager.PERMISSION_GRANTED){
            new AlertDialog.Builder(activity)
                    .setMessage(deniedMessage)
                    .setPositiveButton(android.R.string.ok,null)
                    .setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialogInterface) {
                            activity.finish();
                        }
                    }).show();
            return false;
        }
        return true;
    }

    //检查权限，有返回true，无返回false并显示提示信息并关闭当前Activity（AppCompat）
    public static boolean CheckPermissionWithFinishOnDenied(final AppCompatActivity activity, String permission, String deniedMessage){
        if(activity.checkCallingOrSelfPermission(permission)!=PackageManager.PERMISSION_GRANTED){
            new AlertDialog.Builder(activity)
                    .setMessage(deniedMessage)
                    .setPositiveButton(android.R.string.ok,null)
                    .setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialogInterface) {
                            activity.finish();
                        }
                    }).show();
            return false;
        }
        return true;
    }
}

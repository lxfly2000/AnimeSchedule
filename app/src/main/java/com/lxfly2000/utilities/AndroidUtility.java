package com.lxfly2000.utilities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;

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

    public static void MessageBox(Context activity, String msg){
        new AlertDialog.Builder(activity)
                .setMessage(msg)
                .setPositiveButton(android.R.string.ok,null)
                .show();
    }
}

package com.lxfly2000.animeschedule;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.view.WindowManager;
import android.widget.Toast;
import com.lxfly2000.utilities.AndroidDownloadFileTask;
import com.lxfly2000.utilities.AndroidUtility;
import com.lxfly2000.utilities.StreamUtility;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UpdateChecker {
    private String fileContentString;
    private boolean errorOccurred =false;
    private boolean updateOnlyReportNewVersion=false;
    private Context ctx;

    UpdateChecker(@NonNull Context context){
        ctx=context;
    }

    private void CheckForUpdateMain(){
        //https://stackoverflow.com/questions/5150637/networkonmainthreadexception
        AndroidDownloadFileTask task=new AndroidDownloadFileTask() {
            @Override
            public void OnReturnStream(ByteArrayInputStream stream, boolean success, Object extra) {
                try {
                    ((UpdateChecker) extra).OnReceiveData(success?StreamUtility.GetStringFromStream(stream):null);
                }catch (IOException e){
                    ((UpdateChecker)extra).OnReceiveData(null);
                }
            }
        };
        task.SetExtra(this);
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,Values.GetCheckUpdateURL());
    }

    private boolean IsError(){
        return errorOccurred;
    }

    private int remoteVersionCode;
    private SharedPreferences preferences;

    private void OnReceiveData(String data){
        fileContentString=data;
        if(fileContentString==null){
            errorOccurred=true;
        }else{
            remoteVersionCode=GetUpdateVersionCode();
            preferences=Values.GetPreference(ctx);
            if(updateOnlyReportNewVersion&&remoteVersionCode<=preferences.getInt(Values.keySkippedVersionCode,Values.vDefaultSkippedVersionCode))
                remoteVersionCode=0;
            if(remoteVersionCode>BuildConfig.VERSION_CODE){
                OnReceive(true);
                return;
            }
        }
        OnReceive(false);
    }

    private void OnReceive(boolean foundNewVersion){
        AlertDialog.Builder msgBox=new AlertDialog.Builder(ctx);//这里不能用getApplicationContext.
        msgBox.setPositiveButton(android.R.string.ok,null);
        msgBox.setTitle(R.string.menu_check_update);
        String msg;
        if (foundNewVersion) {
            msg = String.format(ctx.getString(R.string.message_new_version), BuildConfig.VERSION_NAME, GetUpdateVersionName());
            msgBox.setMessage(msg);
            msgBox.setIcon(android.R.drawable.ic_dialog_info);
            msgBox.setPositiveButton(android.R.string.ok, (dialogInterface, i) -> AndroidUtility.OpenUri(ctx,Values.urlAuthor));
            msgBox.setNegativeButton(android.R.string.cancel,null);
            msgBox.setNeutralButton(R.string.button_skip_new_version, (dialogInterface, i) -> preferences.edit().putInt(Values.keySkippedVersionCode,remoteVersionCode).apply());
        }else if (updateOnlyReportNewVersion){
            return;
        }else if (IsError()){
            msg=ctx.getString(R.string.error_check_update);
            msgBox.setMessage(msg);
            msgBox.setIcon(android.R.drawable.ic_dialog_alert);
        }else {
            msg=ctx.getString(R.string.message_no_update);
            msgBox.setMessage(msg);
        }
        AlertDialog msgBoxShow=msgBox.create();
        msgBoxShow.getWindow().setType(WindowManager.LayoutParams.TYPE_TOAST);
        try {
            msgBoxShow.show();
        }catch (WindowManager.BadTokenException e){
            Toast.makeText(ctx,msg,Toast.LENGTH_LONG).show();
            if (foundNewVersion)
                AndroidUtility.OpenUri(ctx,Values.urlAuthor);
        }
    }

    private int GetUpdateVersionCode(){
        String searchRegex="versionCode [0-9]*";
        Pattern p=Pattern.compile(searchRegex);
        Matcher m=p.matcher(fileContentString);
        if(m.find()){
            String subRegex="[0-9]*$";
            Pattern pSub=Pattern.compile(subRegex);
            String subFound=fileContentString.substring(m.start(),m.end());
            Matcher mSub=pSub.matcher(subFound);
            if(mSub.find()){
                return Integer.parseInt(subFound.substring(mSub.start(),mSub.end()));
            }
        }
        return 0;
    }

    private String GetUpdateVersionName(){
        String versionName="";
        String searchRegex="versionName \".*\"";
        Pattern p=Pattern.compile(searchRegex);
        Matcher m=p.matcher(fileContentString);
        if(m.find()){
            String subFound=fileContentString.substring(m.start(),m.end());
            //妈的老子用正则表达式怎么都匹配不上版本号，不用总行了吧？
            versionName=subFound.substring(subFound.indexOf('\"')+1,subFound.lastIndexOf('\"'));
        }
        return versionName;
    }

    void CheckForUpdate(boolean onlyReportNewVersion) {
        if (ctx.checkCallingOrSelfPermission("android.permission.INTERNET") != PackageManager.PERMISSION_GRANTED) {
            if (onlyReportNewVersion)
                return;
            AlertDialog.Builder msgBox = new AlertDialog.Builder(ctx)
                    .setTitle(R.string.menu_check_update)
                    .setPositiveButton(android.R.string.ok, null)
                    .setMessage(R.string.error_permission_network);
            msgBox.show();
        } else {
            updateOnlyReportNewVersion=onlyReportNewVersion;
            CheckForUpdateMain();
        }
    }
}

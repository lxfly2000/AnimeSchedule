package com.lxfly2000.animeschedule;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import com.lxfly2000.utilities.AndroidDownloadFileTask;
import com.lxfly2000.utilities.StreamUtility;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UpdateChecker {
    private String fileURL;
    private String fileContentString;
    private boolean errorOccurred =false;
    private ResultHandler resultHandler;
    public UpdateChecker SetCheckURL(String url){
        fileURL=url;
        return this;
    }

    public UpdateChecker SetResultHandler(ResultHandler handler){
        resultHandler=handler;
        return this;
    }

    public void CheckForUpdate(){
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
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,fileURL);
    }

    public boolean IsError(){
        return errorOccurred;
    }

    public void OnReceiveData(String data){
        fileContentString=data;
        if(fileContentString==null){
            errorOccurred=true;
        }else if(GetUpdateVersionCode()>BuildConfig.VERSION_CODE){
            resultHandler.OnReceive(true);
            return;
        }
        resultHandler.OnReceive(false);
    }

    public int GetUpdateVersionCode(){
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

    public String GetUpdateVersionName(){
        String versionName="";
        String searchRegex="versionName \"[0-9.]*\"";
        Pattern p=Pattern.compile(searchRegex);
        Matcher m=p.matcher(fileContentString);
        if(m.find()){
            String subFound=fileContentString.substring(m.start(),m.end());
            //妈的老子用正则表达式怎么都匹配不上版本号，不用总行了吧？
            versionName=subFound.substring(subFound.indexOf('\"')+1,subFound.lastIndexOf('\"'));
        }
        return versionName;
    }

    public static abstract class ResultHandler{
        private Context androidContext;
        public ResultHandler(Context context){
            androidContext=context;
        }
        private boolean onlyReportUpdate=true;
        public ResultHandler SetOnlyReportUpdate(boolean b){
            onlyReportUpdate=b;
            return this;
        }
        public boolean GetOnlyReportUpdate(){
            return onlyReportUpdate;
        }
        public void HandlerSendBroadcast(Intent intent){
            androidContext.sendBroadcast(intent);
        }
        protected abstract void OnReceive(boolean foundNewVersion);
    }
}

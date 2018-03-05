package com.lxfly2000.animeschedule;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
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
        DownloadFileTask task=new DownloadFileTask(this);
        task.execute(fileURL);
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

class DownloadFileTask extends AsyncTask<String,Integer,Boolean>{
    public String fileContentString;
    private UpdateChecker checker;
    public DownloadFileTask(UpdateChecker _checker){
        checker=_checker;
    }

    //http://blog.csdn.net/hanqunfeng/article/details/4364583
    private String GetStringFromStream(InputStream stream)throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        StringBuilder getString = new StringBuilder();
        String newString;
        while (true) {
            newString = reader.readLine();
            if (newString == null)
                break;
            getString.append(newString).append("\n");
        }
        return getString.toString();
    }
    @Override
    protected Boolean doInBackground(String... urls) {
        return DownloadFileToString(urls[0]);
    }

    private boolean DownloadFileToString(String url){
        //http://blog.csdn.net/pgmsoul/article/details/7181793
        URL jUrl;
        try{
            jUrl=new URL(url);
        }catch (MalformedURLException e){
            return false;
        }
        URLConnection connection;
        InputStream ins;
        try{
            connection=jUrl.openConnection();
            ins=connection.getInputStream();
            fileContentString=GetStringFromStream(ins);
            ins.close();
        }catch (IOException e){
            return false;
        }
        return true;
    }
    @Override
    protected void onPostExecute(Boolean result){
        if(result){
            checker.OnReceiveData(fileContentString);
        }else {
            checker.OnReceiveData(null);
        }
    }
}
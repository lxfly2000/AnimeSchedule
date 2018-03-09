package com.lxfly2000.utilities;

import android.os.AsyncTask;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public abstract class AndroidDownloadFileTask extends AsyncTask<String,Integer,Boolean> {
    private String fileContentString;
    private Object extraObject;

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
            OnReceiveSuccess(fileContentString,extraObject);
        }else {
            OnReceiveFail(extraObject);
        }
    }

    public void SetExtra(Object extra){
        extraObject=extra;
    }

    public abstract void OnReceiveSuccess(String data,Object extra);
    public abstract void OnReceiveFail(Object extra);
}
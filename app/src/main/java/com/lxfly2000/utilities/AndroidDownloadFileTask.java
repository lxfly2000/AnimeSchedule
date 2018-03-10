package com.lxfly2000.utilities;

import android.os.AsyncTask;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public abstract class AndroidDownloadFileTask extends AsyncTask<String,Integer,Boolean> {
    private ByteArrayInputStream inStream;
    private Object extraObject;

    @Override
    protected Boolean doInBackground(String... params) {
        return DownloadFileToStream(params[0]);
    }

    private boolean DownloadFileToStream(String url){
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
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len;
            while ((len = ins.read(buffer)) > -1 ) {
                outputStream.write(buffer, 0, len);
            }
            outputStream.flush();
            // 打开一个新的输入流
            inStream=new ByteArrayInputStream(outputStream.toByteArray());
            ins.close();
        }catch (IOException e){
            return false;
        }
        return true;
    }
    @Override
    protected void onPostExecute(Boolean result){
        OnReturnStream(inStream,result,extraObject);
    }

    public void SetExtra(Object extra){
        extraObject=extra;
    }

    public abstract void OnReturnStream(ByteArrayInputStream stream, boolean success, Object extra);
}
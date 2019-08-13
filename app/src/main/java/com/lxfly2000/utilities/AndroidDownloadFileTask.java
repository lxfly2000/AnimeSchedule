package com.lxfly2000.utilities;

import android.os.AsyncTask;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public abstract class AndroidDownloadFileTask extends AsyncTask<String,Integer,Boolean> {
    private ByteArrayInputStream inStream;
    private int responseCode=0;
    private int connectTimeOut=0,readTimeOut=0;
    private boolean downloadFile=true;
    private Object extraObject,additionalReturnedObject;
    private String userAgent=null;
    private String cookie=null;
    private String contentType=null;
    private String acceptCharset=null;
    private String acceptEncoding=null;
    private String referer=null;
    private String accept=null;
    private String acceptLanguage=null;
    private URLConnection connection;

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
        InputStream ins;
        try{
            connection=jUrl.openConnection();
            if(userAgent!=null)
                connection.setRequestProperty("User-Agent",userAgent);
            if(cookie!=null)
                connection.setRequestProperty("Cookie",cookie);
            if(contentType!=null)
                connection.setRequestProperty("Content-Type",contentType);
            if(acceptCharset!=null)
                connection.setRequestProperty("Accept-Charset",acceptCharset);
            if(acceptEncoding!=null)
                connection.setRequestProperty("Accept-Encoding",acceptEncoding);
            if(referer!=null)
                connection.setRequestProperty("Referer",referer);
            if(accept!=null)
                connection.setRequestProperty("Accept",accept);
            if(acceptLanguage!=null)
                connection.setRequestProperty("Accept-Language",acceptLanguage);
            if(connectTimeOut>0)
                connection.setConnectTimeout(connectTimeOut);
            if(readTimeOut>0)
                connection.setReadTimeout(readTimeOut);
            if(url.toLowerCase().startsWith("http"))
                responseCode=((HttpURLConnection)connection).getResponseCode();
            if(responseCode==301||responseCode==302)
                additionalReturnedObject=connection.getHeaderField("Location");
            if(downloadFile) {
                ins = connection.getInputStream();
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int len;
                while ((len = ins.read(buffer)) > -1) {
                    outputStream.write(buffer, 0, len);
                }
                outputStream.flush();
                // 打开一个新的输入流
                inStream = new ByteArrayInputStream(outputStream.toByteArray());
                ins.close();
            }else{
                inStream=null;
            }
        }catch (IOException e){
            //安卓P中默认设置不允许明文HTTP传输，否则会报错
            //解决办法1：将http改为https协议，2：在清单文件中添加android:usesCleartextTraffic="true"
            if(url.startsWith("http:"))//这里在子线程中调用Toast会报告应用程序事件顺序不对，因此无法使用Toast。
                return DownloadFileToStream(url.replaceFirst("http:","https:"));
            return false;
        }
        return true;
    }
    @Override
    protected void onPostExecute(Boolean result){
        OnReturnStream(inStream,result,responseCode,extraObject,connection);
    }

    public void SetDownloadFile(boolean _downloadFile){
        downloadFile=_downloadFile;
    }

    public void SetConnectTimeOut(int ms){
        connectTimeOut=ms;
    }

    public void SetReadTimeOut(int ms){
        readTimeOut=ms;
    }

    public void SetReferer(String _referer){
        referer=_referer;
    }

    public void SetExtra(Object extra){
        extraObject=extra;
    }

    public void SetUserAgent(String ua){
        userAgent=ua;
    }

    public void SetCookie(String _cookie){
        cookie=_cookie;
    }

    public void SetContentType(String _content){
        contentType=_content;
    }

    public void SetAcceptCharset(String charset){
        acceptCharset=charset;
    }

    public void SetAcceptEncoding(String encoding){
        acceptEncoding=encoding;
    }

    public void SetAccept(String _accept){
        accept=_accept;
    }

    public void SetAcceptLanguage(String _al){
        acceptLanguage=_al;
    }

    public abstract void OnReturnStream(ByteArrayInputStream stream, boolean success, int response, Object extra, URLConnection connection);
}
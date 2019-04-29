package com.lxfly2000.utilities;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;

public class AndroidSysDownload {
    private Context ctx;
    private DownloadManager dm;
    public AndroidSysDownload(@NonNull Context context){
        ctx=context;
        dm=(DownloadManager)ctx.getSystemService(Context.DOWNLOAD_SERVICE);
    }

    private String userAgent=null;
    private String cookie=null;
    private String contentType=null;
    private String acceptCharset=null;
    private String acceptEncoding=null;
    private String referer=null;
    private HashMap<String,String>headersEtc=new HashMap<>();

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

    public void SetReferer(String _referer){
        referer=_referer;
    }

    public void SetOtherHeader(String header,String value){
        headersEtc.put(header,value);
    }

    public long StartDownloadFile(String urlString,String localPath){
        return StartDownloadFile(urlString,localPath,null);
    }

    public long StartDownloadFile(String urlString,String localPath,String notifyTitle){
        return StartDownloadFile(urlString,localPath,notifyTitle,null);
    }

    private long downloadId;
    //https://blog.csdn.net/lu1024188315/article/details/51785161
    public long StartDownloadFile(String urlString,String localPath,String notifyTitle,String notifyDesc){
        RegisterCompleteReceiver();
        DownloadManager.Request request=new DownloadManager.Request(Uri.parse(urlString));
        if(localPath.startsWith("/"))
            localPath="file://"+localPath;
        request.setDestinationUri(Uri.parse(localPath));
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        if(notifyTitle!=null)
            request.setTitle(notifyTitle);
        if(notifyDesc!=null)
            request.setDescription(notifyDesc);

        if(userAgent!=null)
            request.addRequestHeader("User-Agent",userAgent);
        if(cookie!=null)
            request.addRequestHeader("Cookie",cookie);
        if(contentType!=null)
            request.addRequestHeader("Content-Type",contentType);
        if(acceptCharset!=null)
            request.addRequestHeader("Accept-Charset",acceptCharset);
        if(acceptEncoding!=null)
            request.addRequestHeader("Accept-Encoding",acceptEncoding);
        if(referer!=null)
            request.addRequestHeader("Referer",referer);
        if(headersEtc.size()>0){
            for (HashMap.Entry<String,String> e : headersEtc.entrySet()) {
                request.addRequestHeader(e.getKey(), e.getValue());
            }
        }

        downloadId=dm.enqueue(request);
        return downloadId;
    }

    public boolean IsDownloadIdExists(long downloadId){
        DownloadManager.Query query=new DownloadManager.Query().setFilterById(downloadId);
        Cursor c=dm.query(query);
        if(c!=null){
            c.close();
            return true;
        }else {
            return false;
        }
    }

    public boolean IsDownloadIdSuccess(long downloadId){
        DownloadManager.Query query=new DownloadManager.Query().setFilterById(downloadId);
        Cursor c=dm.query(query);
        boolean s=false;
        if(c!=null&&c.moveToFirst())
            s=c.getColumnIndex(DownloadManager.COLUMN_STATUS)==DownloadManager.STATUS_SUCCESSFUL;
        return s;
    }

    public int GetDownloadIdDownloadedBytes(long downloadId){
        DownloadManager.Query query=new DownloadManager.Query().setFilterById(downloadId);
        Cursor c=dm.query(query);
        int b=0;
        if(c!=null&&c.moveToFirst())
            b=c.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR);
        return b;
    }

    public int GetDownloadIdReturnedBytes(long downloadId){
        DownloadManager.Query query=new DownloadManager.Query().setFilterById(downloadId);
        Cursor c=dm.query(query);
        int b=0;
        if(c!=null&&c.moveToFirst())
            b=c.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES);
        return b;
    }

    private BroadcastReceiver receiver=null;
    private Object paramExtra;
    private OnDownloadCompleteFunction completeFunction=null;
    public void SetOnDownloadFinishReceiver(OnDownloadCompleteFunction f,Object extra){
        paramExtra=extra;
        completeFunction=f;
    }

    private void RegisterCompleteReceiver(){
        if(completeFunction==null)
            return;
        UnregisterCompleteReceiver();
        receiver=new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(!DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(intent.getAction()))
                    return;
                long did=intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID,-1);
                if(did!=downloadId)
                    return;
                UnregisterCompleteReceiver();
                completeFunction.OnDownloadComplete(did,IsDownloadIdSuccess(did),GetDownloadIdDownloadedBytes(did),
                        GetDownloadIdReturnedBytes(did),paramExtra);
            }
        };
        ctx.getApplicationContext().registerReceiver(receiver,new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    private void UnregisterCompleteReceiver(){
        if(receiver!=null) {
            ctx.getApplicationContext().unregisterReceiver(receiver);
            receiver=null;
        }
    }

    public static abstract class OnDownloadCompleteFunction{
        public abstract void OnDownloadComplete(long downloadId, boolean success,int downloadedSize,int returnedFileSize, Object extra);
    }
}

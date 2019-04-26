package com.lxfly2000.utilities;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import androidx.annotation.NonNull;

public class AndroidSysDownload {
    private Context ctx;
    private DownloadManager dm;
    public AndroidSysDownload(@NonNull Context context){
        ctx=context;
        dm=(DownloadManager)ctx.getSystemService(Context.DOWNLOAD_SERVICE);
    }

    public long StartDownloadFile(String urlString,String localPath){
        return StartDownloadFile(urlString,localPath,null);
    }

    public long StartDownloadFile(String urlString,String localPath,String notifyTitle){
        return StartDownloadFile(urlString,localPath,notifyTitle,null);
    }

    //https://blog.csdn.net/lu1024188315/article/details/51785161
    public long StartDownloadFile(String urlString,String localPath,String notifyTitle,String notifyDesc){
        DownloadManager.Request request=new DownloadManager.Request(Uri.parse(urlString));
        if(localPath.startsWith("/"))
            localPath="file://"+localPath;
        request.setDestinationUri(Uri.parse(localPath));
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        if(notifyTitle!=null)
            request.setTitle(notifyTitle);
        if(notifyDesc!=null)
            request.setDescription(notifyDesc);
        return dm.enqueue(request);
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

    private BroadcastReceiver receiver;
    public void SetOnDownloadFinishReceiver(OnDownloadCompleteFunction f){
        receiver=new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(!DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(intent.getAction()))
                    return;
                f.OnDownloadComplete(intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID,-1));
                ctx.unregisterReceiver(receiver);
            }
        };
        ctx.registerReceiver(receiver,new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    public static abstract class OnDownloadCompleteFunction{
        public abstract void OnDownloadComplete(long downloadId);
    }
}

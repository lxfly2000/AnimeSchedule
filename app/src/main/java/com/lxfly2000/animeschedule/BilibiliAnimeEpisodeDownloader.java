package com.lxfly2000.animeschedule;

import android.content.Context;
import android.widget.Toast;
import androidx.annotation.NonNull;

public class BilibiliAnimeEpisodeDownloader {
    private Context ctx;
    public BilibiliAnimeEpisodeDownloader(@NonNull Context context){
        ctx=context;
    }

    public void DownloadEpisode(String savePath,int ssid,int epid,int videoQuality,int avid,String notifyTitle){
        //TODO
        Toast.makeText(ctx,"TODO:该功能正在制作中。\nPath:"+savePath+"\nSSID:"+ssid+" EPID:"+epid+" VQ:"+videoQuality+" AVID:"+avid+"\nNotify:"+notifyTitle,Toast.LENGTH_LONG).show();
    }
}

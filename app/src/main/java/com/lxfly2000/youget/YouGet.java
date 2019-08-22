package com.lxfly2000.youget;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;

public abstract class YouGet {
    protected Context ctx;
    YouGet(@NonNull Context context){
        ctx=context;
    }

    //saveDirPath指要存放的文件夹路径，不包括文件名
    public abstract void DownloadBangumi(String url,int episodeToDownload_fromZero,int quality,String saveDirPath);

    public static class VideoQuality{
        int index;
        String qualityName;
        public VideoQuality(int _index,String _qualityName){
            index=_index;
            qualityName=_qualityName;
        }
    }

    public static abstract class OnReturnVideoQualityFunction{
        public abstract void OnReturnVideoQuality(boolean success, ArrayList<VideoQuality>qualities);
    }

    public abstract void QueryQualities(String url,int episodeToDownload_fromZero,OnReturnVideoQualityFunction f);

    public static abstract class OnFinishFunction {
        public abstract void OnFinish(boolean success, @Nullable String bangumiPath, @Nullable String danmakuPath, @Nullable String msg);
    }

    protected OnFinishFunction onFinishFunction;

    public void SetOnFinish(OnFinishFunction f){
        onFinishFunction =f;
    }
}

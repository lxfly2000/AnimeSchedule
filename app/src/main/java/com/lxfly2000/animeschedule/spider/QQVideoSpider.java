package com.lxfly2000.animeschedule.spider;

import android.content.Context;

public class QQVideoSpider extends Spider {
    public QQVideoSpider(Context ctx){
        super(ctx);
    }

    @Override
    public void Execute(String url){
        onReturnDataFunction.OnReturnData(null,STATUS_FAILED,"非常抱歉，针对腾讯视频的信息获取功能还在制作中，请手动填写番剧信息。");
        //TODO：对给出的播放页URL获取信息
    }
}
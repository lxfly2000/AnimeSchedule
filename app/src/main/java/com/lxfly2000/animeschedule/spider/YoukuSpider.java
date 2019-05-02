package com.lxfly2000.animeschedule.spider;

import android.content.Context;

public class YoukuSpider extends Spider {
    public YoukuSpider(Context ctx){
        super(ctx);
    }

    @Override
    public void Execute(String url){
        //优酷网址格式：
        //应用&移动端：m.youku.com/video/id_<VID>.html
        //网页端视频播放页：v.youku.com/v_show/id_<VID>.html
        //网页端剧集简介：list.youku.com/show/id_<ListID>.html
        onReturnDataFunction.OnReturnData(null,STATUS_FAILED,"非常抱歉，针对优酷的信息获取功能还在制作中，请手动填写番剧信息。\n如果您是开发者，欢迎到Github帮我完善这个功能。");
        //TODO：对给出的播放页URL获取信息
    }
}

package com.lxfly2000.animeschedule.spider;

import android.content.Context;

public class QQVideoSpider extends Spider {
    public QQVideoSpider(Context ctx){
        super(ctx);
    }

    @Override
    public void Execute(String url){
        //腾讯网址格式：
        //应用&移动端：m.v.qq.com/play/play.html?coverid=<CoverID>&vid=<VID>&vuid=<VUID>&(后面的参数可以不用管)
        //                                       ~~~~~~~也称为cid，其实只管这个CoverID就可以了
        //网页端：①v.qq.com/x/cover/<CoverID>.html
        //　　　　②v.qq.com/x/cover/<CoverID>/<EpID>.html
        //                                     ~~~~~~同样也只需要管CoverID就行了
        onReturnDataFunction.OnReturnData(null,STATUS_FAILED,"非常抱歉，针对腾讯视频的信息获取功能还在制作中，请手动填写番剧信息。\n如果您是开发者，欢迎到Github帮我完善这个功能。");
        //TODO：对给出的播放页URL获取信息
    }
}

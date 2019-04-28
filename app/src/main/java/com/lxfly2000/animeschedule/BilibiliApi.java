package com.lxfly2000.animeschedule;

public class BilibiliApi {
    /**
     * 返回查询视频的API链接
     * @param videoFormat 视频类型，0为mp4，1为FLV
     * @param avid 视频AV号
     * @param cid 视频弹幕号
     * @param qn APP中使用的清晰度代码
     * @return 视频查询链接
     */
    public static String GetVideoLink(int videoFormat,int apiType,String avid,String cid,int qn){
        //https://github.com/nICEnnnnnnnLee/BilibiliDown/blob/43f514440f4ec93fda50fa50bf7e039cf5135f9c/src/nicelee/bilibili/parsers/impl/AbstractBaseParser.java#L238
        String[]mp4Urls={
                "https://api.bilibili.com/x/player/playurl?fnval=16&fnver=0&type=&otype=json&avid=%s&cid=%s&qn=%d",
                "https://api.bilibili.com/pgc/player/web/playurl?fnval=16&fnver=0&player=1&otype=json&avid=%s&cid=%s&qn=%d"
        };
        //https://github.com/nICEnnnnnnnLee/BilibiliDown/blob/43f514440f4ec93fda50fa50bf7e039cf5135f9c/src/nicelee/bilibili/parsers/impl/AbstractBaseParser.java#L200
        String[]flvUrls={
                "https://api.bilibili.com/x/player/playurl?fnval=2&fnver=0&player=1&otype=json&avid=%s&cid=%s&qn=%d",
                "https://api.bilibili.com/pgc/player/web/playurl?fnval=2&fnver=0&player=1&otype=json&avid=%s&cid=%s&qn=%d"
        };
        if(videoFormat==0){
            return String.format(mp4Urls[apiType],avid,cid,qn);
        }else {
            return String.format(flvUrls[apiType],avid,cid,qn);
        }
    }
}

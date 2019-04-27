package xiaoyaocz.BiliAnimeDownload.Helpers;

import com.lxfly2000.utilities.HashUtility;

import java.util.Arrays;

//全部截取自：https://github.com/xiaoyaocz/BiliAnimeDownload/blob/master/BiliAnimeDownload/BiliAnimeDownload/Helpers/Api.cs
public class Api {
    public static final String _appSecret = "560c52ccd288fed045859ed18bffd973";
    public static final String _appKey = "1d8b6e7d45233436";
    public static final String _appSecret_VIP = "9b288147e5474dd2aa67085f716c560d";
    public static final String _appSecret_PlayUrl = "1c15888dc316e05a15fdd0a02ed6584f";
    public static String GetSign(String url)
    {
        String result;
        String str = url.substring(url.indexOf("?", 4) + 1);
        String[]list = str.split("&");
        Arrays.sort(list);
        StringBuilder stringBuilder = new StringBuilder();
        for (String str1 : list) {
            stringBuilder.append((stringBuilder.length() > 0 ? "&" : ""));
            stringBuilder.append(str1);
        }
        stringBuilder.append(_appSecret);
        result = HashUtility.GetStringMD5(stringBuilder.toString()).toLowerCase();
        return result;
    }
    public static String  GetSign_VIP(String url)
    {
        String result;
        String str = url.substring(url.indexOf("?", 4) + 1);
        String[]list = str.split("&");
        Arrays.sort(list);
        StringBuilder stringBuilder = new StringBuilder();
        for (String str1 : list) {
            stringBuilder.append((stringBuilder.length() > 0 ? "&" : ""));
            stringBuilder.append(str1);
        }
        stringBuilder.append(_appSecret_VIP);
        result = HashUtility.GetStringMD5(stringBuilder.toString()).toLowerCase();
        return result;
    }

    public static String GetSign_PlayUrl(String url)
    {
        String result;
        String str = url.substring(url.indexOf("?", 4) + 1);
        String[]list = str.split("&");
        Arrays.sort(list);
        StringBuilder stringBuilder = new StringBuilder();
        for (String str1 : list) {
            stringBuilder.append((stringBuilder.length() > 0 ? "&" : ""));
            stringBuilder.append(str1);
        }
        stringBuilder.append(_appSecret_PlayUrl);
        result = HashUtility.GetStringMD5(stringBuilder.toString()).toLowerCase();
        return result;
    }


    public static long GetTimeSpan(){
        return System.currentTimeMillis()/1000;
    }
    public static long GetTimeSpan_2(){
        return System.currentTimeMillis();
    }


    public static String _BanInfoApi(String sid)
    {

        String uri = String.format("http://bangumi.bilibili.com/api/season_v3?_device=android&_ulv=10000&build=411005&platform=android&appkey=1d8b6e7d45233436&ts=%d000&type=bangumi&season_id=%s", GetTimeSpan(), sid);
        uri += "&sign=" + GetSign(uri);
        return uri;

    }

    public static String _VideoInfoApi(String aid)
    {
        //这个API不用sign也可以访问，以防万一，还是加上...
        String uri = String.format("http://app.bilibili.com/x/view?aid=%s&appkey=1d8b6e7d45233436&build=521000&ts=%d", aid, GetTimeSpan());
        uri += "&sign=" + GetSign(uri);
        return uri;

    }


    public static String _BanInfoApiJSONP(String sid)
    {

        String uri = String.format("http://bangumi.bilibili.com/jsonp/seasoninfo/%s.ver?callback=seasonListCallback&jsonp=jsonp&_=%d", sid, GetTimeSpan());
        //https://bangumi.bilibili.com/view/web_api/season?season_id=4187
        return uri;

    }
    public static String _BanInfoApi2(String sid)
    {

        String uri = String.format("https://bangumi.bilibili.com/view/web_api/season?season_id=%s", sid);
        //
        return uri;

    }

    public static String _commentApi(String aid)
    {

        String uri = String.format("https://api.bilibili.com/x/v2/reply?access_key=&appkey=%s&build=511000&mobi_app=android&oid=%s&plat=2&platform=android&pn=1&ps=20&sort=0&ts=%d&type=1", _appKey, aid, GetTimeSpan());
        uri += "&sign=" + GetSign(uri);
        return uri;

    }

    public static String _playurlApi(String cid) {
        return _playurlApi(cid, 4);
    }
    public static String _playurlApi(String cid, int quality)
    {


        int qn = 80;
        switch (quality)
        {
            case 1:
                qn = 32;
                break;
            case 2:
                qn = 64;
                break;
            case 3:
                qn = 80;
                break;
            case 4:
                qn = 112;
                break;
            default:
                break;
        }

        String url = String.format("http://interface.bilibili.com/v2/playurl?cid=%s&player=1&quality=%d&qn=%d&ts=%d", cid, qn,qn, GetTimeSpan());
        url += "&sign=" + GetSign_PlayUrl(url);

        return url;
    }

    public static String _playurlApi2(String cid) {
        return _playurlApi2(cid, 4);
    }
    public static String _playurlApi2(String cid, int quality)
    {
        String url = String.format("https://bangumi.bilibili.com/player/web_api/playurl/?cid=%s&module=bangumi&player=1&otype=json&type=flv&quality=%d&ts=%d", cid, quality, GetTimeSpan_2());
        url += "&sign=" + GetSign_VIP(url);

        //https://www.biliplus.com/BPplayurl.php?cid=30751001|bangumi&otype=json&type=&quality=112&module=bangumi&season_type=1&qn=112


        return url;
    }

    public static String _playurlApi3(String bangumiId, int index)
    {
        //从自建服务器上读取
        String url = String.format("http://120.92.50.146/api/BiliToOther?id=%s&index=%d", bangumiId, index);
        return url;
    }
    public static String _playurlApi4(String bangumiId, String cid,String epid)
    {
        //从自建服务器上读取
        String url = String.format("https://moe.nsapps.cn/api/v1/BiliAnimeUrl?animeid=%s&cid=%s&epid=%s", bangumiId, cid, epid);
        return url;
    }

}

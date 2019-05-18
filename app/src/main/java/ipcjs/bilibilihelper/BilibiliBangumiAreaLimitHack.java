package ipcjs.bilibilihelper;

public class BilibiliBangumiAreaLimitHack {
    public static String balh_api_plus_playurl_biliplus_ipcjs_top(int cid, int qn, boolean bangumi){
        String bangumiString=bangumi?"&module=bangumi":"";
        return "https://biliplus.ipcjs.top/BPplayurl.php?otype=json&cid="+cid+bangumiString+"&qn="+qn+"&src=vupload&vid=vupload_"+cid;
    }
    public static String balh_api_plus_playurl_www_biliplus_com(int cid, int qn, boolean bangumi){
        String bangumiString=bangumi?"&module=bangumi":"";
        return "https://www.biliplus.com/BPplayurl.php?otype=json&cid="+cid+bangumiString+"&qn="+qn+"&src=vupload&vid=vupload_"+cid;
    }
}

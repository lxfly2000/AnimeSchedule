package com.lxfly2000.youget;

import android.content.Context;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.lxfly2000.animeschedule.R;
import com.lxfly2000.animeschedule.Values;
import com.lxfly2000.utilities.AndroidDownloadFileTask;
import com.lxfly2000.utilities.FileUtility;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;

public class YoukuGet extends YouGet {
    public YoukuGet(@NonNull Context context) {
        super(context);
    }

    @Override
    public void DownloadBangumi(String url, int episodeToDownload_fromZero, int quality, String saveDirPath) {

    }

    @Override
    public void QueryQualities(String url, int episodeToDownload_fromZero, OnReturnVideoQualityFunction f) {

    }

    String QuoteCNA(String val){
        if(val.contains("%"))
            return val;
        try {
            return URLEncoder.encode(val, "UTF-8");
        }catch (UnsupportedEncodingException e){
            return val;
        }
    }

    String cookiePath= Values.GetRepositoryPathOnLocal()+"/cookie_youku.txt";

    static abstract class OnReturnCNAFunction{
        public abstract void OnReturnCNA(@Nullable String cna);
    }

    void FetchCNA(OnReturnCNAFunction f){
        String cookies= FileUtility.ReadFile(cookiePath);
        if(cookies!=null){
            HashMap<String,String>mapCookie=new HashMap<>();
            String[]sp=cookies.split(";");
            for (String c : sp) {
                String[] sp2 = c.split("=");
                if (sp2.length > 1)
                    mapCookie.put(sp2[0], sp2[1]);
            }
            String domain=mapCookie.get("domain");
            if(domain==null)
                domain=mapCookie.get("DOMAIN");
            String cna=mapCookie.get("cna");
            if(cna==null)
                cna=mapCookie.get("CNA");
            if(cna!=null&&domain!=null){
                if(domain.equals(".youku.com")){
                    //找到CNA信息
                    f.OnReturnCNA(QuoteCNA(cna));
                }
            }
        }
        String url="https://log.mmstat.com/eg.js";
        AndroidDownloadFileTask task=new AndroidDownloadFileTask() {
            @Override
            public void OnReturnStream(ByteArrayInputStream stream, boolean success, int response, Object extra, URLConnection connection) {
                if(success){
                    String cookie=connection.getHeaderField("Set-Cookie");
                    String[]sp=cookie.split(";");
                    for (String e : sp) {
                        String[] sp2 = e.split("=");
                        if(sp2[0].toLowerCase().equals("cna")){
                            f.OnReturnCNA(QuoteCNA(sp2[1]));
                            return;
                        }
                    }
                    Toast.makeText(ctx, R.string.message_fetch_cna_failed,Toast.LENGTH_LONG).show();
                    f.OnReturnCNA(QuoteCNA("DOG4EdW4qzsCAbZyXbU+t7Jt"));
                }
            }
        };
        task.SetDownloadFile(false);
        task.execute(url);
    }

    static class StreamType{
        public String id,container,videoProfile;
        public StreamType(String id,String container,String videoProfile){
            this.id=id;
            this.container=container;
            this.videoProfile=videoProfile;
        }
    }

    class Youku{
        String mobileUA="Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.101 Safari/537.36";
        String dispatcherURL="vali.cp31.ott.cibntv.net";
        ArrayList<StreamType>streamTypes;

        String ua = mobileUA;
        String referer = "https://v.youku.com";

        String page;
        String video_list;
        String video_next;
        String password;
        String api_data;
        String api_error_code;
        String api_error_msg;

        String ccode = "0519";
        //Found in http://g.alicdn.com/player/ykplayer/0.5.64/youku-player.min.js
        //grep -oE '"[0-9a-zA-Z+/=]{256}"' youku-player.min.js
        String ckey = "DIl58SLFxFNndSV1GFNnMQVYkx1PP5tKe1siZu/86PR1u/Wh1Ptd+WOZsHHWxysSfAOhNJpdVWsdVJNsfJ8Sxd8WKVvNfAS8aS8fAOzYARzPyPc3JvtnPHjTdKfESTdnuTW6ZPvk2pNDh4uFzotgdMEFkzQ5wZVXl2Pf1/Y6hLK0OnCNxBj3+nb0v72gZ6b0td+WOZsHHWxysSo/0y9D2K42SaB8Y/+aD2K42SaB8Y/+ahU+WOZsHcrxysooUeND";
        String utid;

        public Youku() {
            streamTypes = new ArrayList<>();
            streamTypes.add(new StreamType("hd3", "flv", "1080P"));
            streamTypes.add(new StreamType("hd3v2", "flv", "1080P"));
            streamTypes.add(new StreamType("mp4hd3", "mp4", "1080P"));
            streamTypes.add(new StreamType("mp4hd3v2", "mp4", "1080P"));

            streamTypes.add(new StreamType("hd2", "flv", "超清"));
            streamTypes.add(new StreamType("hd2v2", "flv", "超清"));
            streamTypes.add(new StreamType("mp4hd2", "mp4", "超清"));
            streamTypes.add(new StreamType("mp4hd2v2", "mp4", "超清"));

            streamTypes.add(new StreamType("mp4hd", "mp4", "高清"));
            //not really equivalent to mp4hd
            streamTypes.add(new StreamType("flvhd", "flv", "渣清"));
            streamTypes.add(new StreamType("3gphd", "mp4", "渣清"));

            streamTypes.add(new StreamType("mp4sd", "mp4", "标清"));
            //obsolete?
            streamTypes.add(new StreamType("flv", "flv", "标清"));
            streamTypes.add(new StreamType("mp4", "mp4", "标清"));
        }
    }
}

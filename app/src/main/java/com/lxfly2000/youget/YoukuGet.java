package com.lxfly2000.youget;

import android.content.Context;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.lxfly2000.animeschedule.R;
import com.lxfly2000.animeschedule.Values;
import com.lxfly2000.utilities.AndroidDownloadFileTask;
import com.lxfly2000.utilities.FileUtility;
import com.lxfly2000.utilities.StreamUtility;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

public class YoukuGet extends YouGet {
    private String paramPlayUrl,paramSavePath, fileNameWithoutExt;
    private String htmlString,title;
    private String videoId;
    String cookiePath= Values.GetRepositoryPathOnLocal()+"/cookie_youku.txt";
    public YoukuGet(@NonNull Context context) {
        super(context);
    }

    @Override
    public void DownloadBangumi(String url, int episodeToDownload_fromZero, int quality, String saveDirPath) {
        paramPlayUrl=url;
        paramSavePath=saveDirPath;
    }

    @Override
    public void QueryQualities(String url, int episodeToDownload_fromZero, OnReturnVideoQualityFunction f) {

    }

    String quote_cna(String val){
        if(val.contains("%"))
            return val;
        try {
            return URLEncoder.encode(val, "UTF-8");
        }catch (UnsupportedEncodingException e){
            return val;
        }
    }

    static abstract class OnReturnCNAFunction{
        public abstract void OnReturnCNA(@Nullable String cna);
    }

    void fetch_cna(OnReturnCNAFunction f){
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
                    f.OnReturnCNA(quote_cna(cna));
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
                            f.OnReturnCNA(quote_cna(sp2[1]));
                            return;
                        }
                    }
                    Toast.makeText(ctx, R.string.message_fetch_cna_failed,Toast.LENGTH_LONG).show();
                    f.OnReturnCNA(quote_cna("DOG4EdW4qzsCAbZyXbU+t7Jt"));
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
        String mobile_ua ="Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.101 Safari/537.36";
        String dispatcher_url ="vali.cp31.ott.cibntv.net";
        ArrayList<StreamType> stream_types;

        String ua = mobile_ua;
        String referer = "https://v.youku.com";

        String page;
        JSONArray video_list;
        JSONArray video_next;
        String password;
        JSONObject api_data;
        int api_error_code;
        String api_error_msg;

        String ccode = "0519";
        //Found in http://g.alicdn.com/player/ykplayer/0.5.64/youku-player.min.js
        //grep -oE ""[0-9a-zA-Z+/=]{256}"" youku-player.min.js
        String ckey = "DIl58SLFxFNndSV1GFNnMQVYkx1PP5tKe1siZu/86PR1u/Wh1Ptd+WOZsHHWxysSfAOhNJpdVWsdVJNsfJ8Sxd8WKVvNfAS8aS8fAOzYARzPyPc3JvtnPHjTdKfESTdnuTW6ZPvk2pNDh4uFzotgdMEFkzQ5wZVXl2Pf1/Y6hLK0OnCNxBj3+nb0v72gZ6b0td+WOZsHHWxysSo/0y9D2K42SaB8Y/+aD2K42SaB8Y/+ahU+WOZsHcrxysooUeND";
        String utid;

        public Youku() {
            stream_types = new ArrayList<>();
            stream_types.add(new StreamType("hd3", "flv", "1080P"));
            stream_types.add(new StreamType("hd3v2", "flv", "1080P"));
            stream_types.add(new StreamType("mp4hd3", "mp4", "1080P"));
            stream_types.add(new StreamType("mp4hd3v2", "mp4", "1080P"));

            stream_types.add(new StreamType("hd2", "flv", "超清"));
            stream_types.add(new StreamType("hd2v2", "flv", "超清"));
            stream_types.add(new StreamType("mp4hd2", "mp4", "超清"));
            stream_types.add(new StreamType("mp4hd2v2", "mp4", "超清"));

            stream_types.add(new StreamType("mp4hd", "mp4", "高清"));
            //not really equivalent to mp4hd
            stream_types.add(new StreamType("flvhd", "flv", "渣清"));
            stream_types.add(new StreamType("3gphd", "mp4", "渣清"));

            stream_types.add(new StreamType("mp4sd", "mp4", "标清"));
            //obsolete?
            stream_types.add(new StreamType("flv", "flv", "标清"));
            stream_types.add(new StreamType("mp4", "mp4", "标清"));
        }

        String vid;
        boolean password_protected;

        public void youku_ups(){
            String url = String.format("https://ups.youku.com/ups/get.json?vid=%s&ccode=%s",vid,ccode);
            url += "&client_ip=192.168.1.1";
            url += "&utid=" + utid;
            url += "&client_ts=" + (System.currentTimeMillis()/1000);
            try {
                url += "&ckey=" + URLEncoder.encode(ckey, "UTF-8");
            }catch (UnsupportedEncodingException e){/*Ignore*/}
            if (password_protected)
                url += "&password=" + password;
            AndroidDownloadFileTask task=new AndroidDownloadFileTask() {
                @Override
                public void OnReturnStream(ByteArrayInputStream stream, boolean success, int response, Object extra, URLConnection connection) {
                    if(onFinishFunction ==null)
                        return;
                    if(!success){
                        onFinishFunction.OnFinish(false,paramPlayUrl,null,ctx.getString(R.string.message_unable_to_fetch_anime_info));
                        return;
                    }
                    try{
                        String enc=connection.getHeaderField("Content-Encoding");
                        InputStream iStream=stream;
                        if("gzip".equals(enc))//判断输入流是否是压缩的，并获取压缩算法
                            iStream=new GZIPInputStream(stream);
                        else if("deflate".equals(enc))
                            iStream=new InflaterInputStream(stream,new Inflater(true));
                        JSONObject api_meta=new JSONObject(StreamUtility.GetStringFromStream(iStream));
                        api_data=api_meta.getJSONObject("data");
                        if(api_data.has("error")){
                            JSONObject data_error=api_data.getJSONObject("error");
                            api_error_code=data_error.getInt("code");
                            api_error_msg=data_error.getString("note");
                        }
                        if(api_data.has("videos")){
                            JSONObject videos=api_data.getJSONObject("videos");
                            if(videos.has("list"))
                                video_list=videos.getJSONArray("list");
                            if(videos.has("next"))
                                video_next=videos.getJSONArray("next");
                        }
                    }catch (IOException e){
                        onFinishFunction.OnFinish(false,paramPlayUrl,null,ctx.getString(R.string.message_error_on_reading_stream,e.getLocalizedMessage()));
                    }catch (JSONException e){
                        onFinishFunction.OnFinish(false,paramPlayUrl,null,ctx.getString(R.string.message_json_exception,e.getLocalizedMessage()));
                    }
                }
            };
            Common.SetYouGetHttpHeader(task);
            task.SetUserAgent(ua);
            task.SetReferer(referer);
            task.execute(url);
        }

        public void change_cdn(String cls,String url){
            //TODO
        }
    }
}

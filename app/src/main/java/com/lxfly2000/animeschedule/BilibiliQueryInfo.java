package com.lxfly2000.animeschedule;

import android.content.Context;
import com.lxfly2000.utilities.AndroidDownloadFileTask;
import com.lxfly2000.utilities.StreamUtility;
import ipcjs.bilibilihelper.BilibiliBangumiAreaLimitHack;
import org.json.JSONException;
import org.json.JSONObject;
import xiaoyaocz.BiliAnimeDownload.Helpers.Api;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URLConnection;

public class BilibiliQueryInfo {
    private Context ctx;
    private static final int toastCutLength=100;
    private static String ToastStringCut(String src){
        if(src.length()>toastCutLength)
            return src.substring(0,toastCutLength).concat("...");
        else while(src.endsWith("\n"))
            src=src.substring(0,src.length()-1);
        return src;
    }
    public BilibiliQueryInfo(Context context){
        ctx=context;
        episodeInfo=new EpisodeInfo();
    }

    public class EpisodeInfo{
        public String ssidString,epidString,cidString,avidString;
        public int videoQuality=0;

        public int parts=0;
        public String[][]urls;
        public int[]downloadBytes;

        public int queryResult=0;
        public String resultMessage="(Empty)";
        public int GetDownloadBytesSum(){
            int s=0;
            for (int a : downloadBytes) {
                s += a;
            }
            return s;
        }
        public void SetResult(int resultCode,String message){
            queryResult=resultCode;
            resultMessage=message;
            if("".equals(resultMessage))
                resultMessage=String.valueOf(queryResult);
        }
    }

    private EpisodeInfo episodeInfo;

    public void SetParam(String ssidString, String epidString, String avidString, String cidString, int videoQuality){
        episodeInfo.ssidString=ssidString;
        episodeInfo.epidString=epidString;
        episodeInfo.avidString=avidString;
        episodeInfo.cidString=cidString;
        episodeInfo.videoQuality=videoQuality;
    }

    public static abstract class OnReturnEpisodeInfoFunction{
        public abstract void OnReturnEpisodeInfo(EpisodeInfo info,boolean success);
    }
    private OnReturnEpisodeInfoFunction returnEpisodeInfoFunction;

    public void SetOnReturnEpisodeInfoFunction(OnReturnEpisodeInfoFunction f){
        returnEpisodeInfoFunction=f;
    }

    private void FinishQuery(boolean success){
        returnEpisodeInfoFunction.OnReturnEpisodeInfo(episodeInfo,success);
    }

    static final int queryMethodCount=6;
    //method:0~3
    public void Query(int method){
        switch (method){
            default:Query();break;
            case 1:Query2();break;
            case 2:Query3();break;
            case 3:Query4();break;
            case 4:Query5();break;
            case 5:Query6();break;
        }
    }
    public void Query(){
        //获取URL
        AndroidDownloadFileTask task=new AndroidDownloadFileTask() {
            @Override
            public void OnReturnStream(ByteArrayInputStream stream, boolean success, int response, Object extra, URLConnection connection) {
                if(success){
                    String jsonString=Values.vDefaultString;
                    try {
                        jsonString=StreamUtility.GetStringFromStream(stream);
                        JSONObject json = new JSONObject(jsonString);
                        episodeInfo.parts=json.getJSONArray("durl").length();
                        episodeInfo.downloadBytes=new int[episodeInfo.parts];
                        episodeInfo.urls=new String[episodeInfo.parts][];
                        for(int i=0;i<episodeInfo.parts;i++) {
                            //https://github.com/xiaoyaocz/BiliAnimeDownload/blob/852eb5b4fb3fdbd9801be2c6e98f69e3ed4d427a/BiliAnimeDownload/BiliAnimeDownload/Helpers/Util.cs#L86
                            episodeInfo.downloadBytes[i]=json.getJSONArray("durl").getJSONObject(i).getInt("size");
                            int urlCount=1;
                            if(json.getJSONArray("durl").getJSONObject(i).has("backup_url")&&
                                    !json.getJSONArray("durl").getJSONObject(i).isNull("backup_url")){
                                urlCount+=json.getJSONArray("durl").getJSONObject(i).getJSONArray("backup_url").length();
                            }
                            episodeInfo.urls[i]=new String[urlCount];
                            episodeInfo.urls[i][0]=json.getJSONArray("durl").getJSONObject(i).getString("url");
                            for(int j=1;j<urlCount;j++) {
                                episodeInfo.urls[i][j] = json.getJSONArray("durl").getJSONObject(i).getJSONArray("backup_url").getString(j - 1);
                            }
                        }
                        episodeInfo.SetResult(0,"-");
                        FinishQuery(true);
                        return;
                    }catch (IOException e){
                        episodeInfo.SetResult(1,e.getLocalizedMessage());
                    }catch (JSONException e){
                        episodeInfo.SetResult(2,e.getMessage()+"\n"+ToastStringCut(jsonString));
                    }
                }
                FinishQuery(false);
            }
        };
        task.execute(Api._playurlApi2(episodeInfo.cidString));
    }

    public void Query5(){
        //获取URL
        AndroidDownloadFileTask task=new AndroidDownloadFileTask() {
            @Override
            public void OnReturnStream(ByteArrayInputStream stream, boolean success, int response, Object extra,URLConnection connection) {
                if(success){
                    String jsonString=Values.vDefaultString;
                    try {
                        jsonString=StreamUtility.GetStringFromStream(stream);
                        JSONObject json = new JSONObject(jsonString);
                        episodeInfo.parts=json.getJSONArray("durl").length();
                        episodeInfo.downloadBytes=new int[episodeInfo.parts];
                        episodeInfo.urls=new String[episodeInfo.parts][];
                        for(int i=0;i<episodeInfo.parts;i++) {
                            //https://github.com/xiaoyaocz/BiliAnimeDownload/blob/852eb5b4fb3fdbd9801be2c6e98f69e3ed4d427a/BiliAnimeDownload/BiliAnimeDownload/Helpers/Util.cs#L86
                            episodeInfo.downloadBytes[i]=json.getJSONArray("durl").getJSONObject(i).getInt("size");
                            int urlCount=1;
                            if(json.getJSONArray("durl").getJSONObject(i).has("backup_url")&&
                                    !json.getJSONArray("durl").getJSONObject(i).isNull("backup_url")){
                                urlCount+=json.getJSONArray("durl").getJSONObject(i).getJSONArray("backup_url").length();
                            }
                            episodeInfo.urls[i]=new String[urlCount];
                            episodeInfo.urls[i][0]=json.getJSONArray("durl").getJSONObject(i).getString("url");
                            for(int j=1;j<urlCount;j++) {
                                episodeInfo.urls[i][j] = json.getJSONArray("durl").getJSONObject(i).getJSONArray("backup_url").getString(j - 1);
                            }
                        }
                        episodeInfo.SetResult(0,"-");
                        FinishQuery(true);
                        return;
                    }catch (IOException e){
                        episodeInfo.SetResult(1,e.getLocalizedMessage());
                    }catch (JSONException e){
                        episodeInfo.SetResult(2,e.getMessage()+"\n"+ToastStringCut(jsonString));
                    }
                }
                FinishQuery(false);
            }
        };
        task.execute(BilibiliBangumiAreaLimitHack.balh_api_plus_playurl_biliplus_ipcjs_top(Integer.parseInt(episodeInfo.cidString),episodeInfo.videoQuality,true));
    }

    public void Query6(){
        //获取URL
        AndroidDownloadFileTask task=new AndroidDownloadFileTask() {
            @Override
            public void OnReturnStream(ByteArrayInputStream stream, boolean success, int response, Object extra,URLConnection connection) {
                if(success){
                    String jsonString=Values.vDefaultString;
                    try {
                        jsonString=StreamUtility.GetStringFromStream(stream);
                        JSONObject json = new JSONObject(jsonString);
                        episodeInfo.parts=json.getJSONArray("durl").length();
                        episodeInfo.downloadBytes=new int[episodeInfo.parts];
                        episodeInfo.urls=new String[episodeInfo.parts][];
                        for(int i=0;i<episodeInfo.parts;i++) {
                            //https://github.com/xiaoyaocz/BiliAnimeDownload/blob/852eb5b4fb3fdbd9801be2c6e98f69e3ed4d427a/BiliAnimeDownload/BiliAnimeDownload/Helpers/Util.cs#L86
                            episodeInfo.downloadBytes[i]=json.getJSONArray("durl").getJSONObject(i).getInt("size");
                            int urlCount=1;
                            if(json.getJSONArray("durl").getJSONObject(i).has("backup_url")&&
                                    !json.getJSONArray("durl").getJSONObject(i).isNull("backup_url")){
                                urlCount+=json.getJSONArray("durl").getJSONObject(i).getJSONArray("backup_url").length();
                            }
                            episodeInfo.urls[i]=new String[urlCount];
                            episodeInfo.urls[i][0]=json.getJSONArray("durl").getJSONObject(i).getString("url");
                            for(int j=1;j<urlCount;j++) {
                                episodeInfo.urls[i][j] = json.getJSONArray("durl").getJSONObject(i).getJSONArray("backup_url").getString(j - 1);
                            }
                        }
                        episodeInfo.SetResult(0,"-");
                        FinishQuery(true);
                        return;
                    }catch (IOException e){
                        episodeInfo.SetResult(1,e.getLocalizedMessage());
                    }catch (JSONException e){
                        episodeInfo.SetResult(2,e.getMessage()+"\n"+ToastStringCut(jsonString));
                    }
                }
                FinishQuery(false);
            }
        };
        task.execute(BilibiliBangumiAreaLimitHack.balh_api_plus_playurl_www_biliplus_com(Integer.parseInt(episodeInfo.cidString),episodeInfo.videoQuality,true));
    }

    public void Query2(){
        AndroidDownloadFileTask task=new AndroidDownloadFileTask() {
            @Override
            public void OnReturnStream(ByteArrayInputStream stream, boolean success, int response, Object extra,URLConnection connection) {
                if(success) {
                    String jsonString=Values.vDefaultString;
                    try {
                        jsonString=StreamUtility.GetStringFromStream(stream);
                        JSONObject json = new JSONObject(jsonString);
                        episodeInfo.parts=json.getJSONArray("data").length();
                        episodeInfo.downloadBytes=new int[episodeInfo.parts];
                        episodeInfo.urls=new String[episodeInfo.parts][];
                        for(int i=0;i<episodeInfo.parts;i++){
                            episodeInfo.downloadBytes[i]=Integer.parseInt(json.getJSONArray("data").getJSONObject(i).getString("size"));
                            episodeInfo.urls[i]=new String[]{json.getJSONArray("data").getJSONObject(i).getString("url")};
                        }
                        episodeInfo.SetResult(0,"-");
                        FinishQuery(true);
                        return;
                    }catch (IOException e){
                        episodeInfo.SetResult(1,e.getLocalizedMessage());
                    }catch (JSONException e){
                        episodeInfo.SetResult(2,e.getMessage()+"\n"+ToastStringCut(jsonString));
                    }
                }
                FinishQuery(false);
            }
        };
        task.execute(Api._playurlApi4(episodeInfo.ssidString,episodeInfo.cidString,episodeInfo.epidString));
    }

    public void Query3(){
        AndroidDownloadFileTask task=new AndroidDownloadFileTask() {
            @Override
            public void OnReturnStream(ByteArrayInputStream stream, boolean success, int response, Object extra,URLConnection connection) {
                if(success){
                    String jsonString=Values.vDefaultString;
                    try {
                        jsonString=StreamUtility.GetStringFromStream(stream);
                        JSONObject json = new JSONObject(jsonString).getJSONObject("data");
                        episodeInfo.parts=json.getJSONArray("durl").length();
                        episodeInfo.downloadBytes=new int[episodeInfo.parts];
                        episodeInfo.urls=new String[episodeInfo.parts][];
                        for(int i=0;i<episodeInfo.parts;i++) {
                            //https://github.com/xiaoyaocz/BiliAnimeDownload/blob/852eb5b4fb3fdbd9801be2c6e98f69e3ed4d427a/BiliAnimeDownload/BiliAnimeDownload/Helpers/Util.cs#L86
                            episodeInfo.downloadBytes[i]=json.getJSONArray("durl").getJSONObject(i).getInt("size");
                            int urlCount=1;
                            if(json.getJSONArray("durl").getJSONObject(i).has("backup_url")&&
                                    !json.getJSONArray("durl").getJSONObject(i).isNull("backup_url")){
                                urlCount+=json.getJSONArray("durl").getJSONObject(i).getJSONArray("backup_url").length();
                            }
                            episodeInfo.urls[i]=new String[urlCount];
                            episodeInfo.urls[i][0]=json.getJSONArray("durl").getJSONObject(i).getString("url");
                            for(int j=1;j<urlCount;j++) {
                                episodeInfo.urls[i][j] = json.getJSONArray("durl").getJSONObject(i).getJSONArray("backup_url").getString(j - 1);
                            }
                        }
                        episodeInfo.SetResult(0,"-");
                        FinishQuery(true);
                        return;
                    }catch (IOException e){
                        episodeInfo.SetResult(1,e.getLocalizedMessage());
                    }catch (JSONException e){
                        episodeInfo.SetResult(2,e.getMessage()+"\n"+ToastStringCut(jsonString));
                    }
                }
                FinishQuery(false);
            }
        };
        task.execute(BilibiliApi.GetVideoLink(1,0,episodeInfo.avidString,episodeInfo.cidString,
                episodeInfo.videoQuality));
    }

    public void Query4(){
        AndroidDownloadFileTask task=new AndroidDownloadFileTask() {
            @Override
            public void OnReturnStream(ByteArrayInputStream stream, boolean success, int response, Object extra,URLConnection connection) {
                if(success){
                    String jsonString=Values.vDefaultString;
                    try {
                        jsonString=StreamUtility.GetStringFromStream(stream);
                        JSONObject json = new JSONObject(jsonString).getJSONObject("result");
                        episodeInfo.parts=json.getJSONArray("durl").length();
                        episodeInfo.downloadBytes=new int[episodeInfo.parts];
                        episodeInfo.urls=new String[episodeInfo.parts][];
                        for(int i=0;i<episodeInfo.parts;i++) {
                            //https://github.com/xiaoyaocz/BiliAnimeDownload/blob/852eb5b4fb3fdbd9801be2c6e98f69e3ed4d427a/BiliAnimeDownload/BiliAnimeDownload/Helpers/Util.cs#L86
                            episodeInfo.downloadBytes[i]=json.getJSONArray("durl").getJSONObject(i).getInt("size");
                            int urlCount=1;
                            if(json.getJSONArray("durl").getJSONObject(i).has("backup_url")&&
                                    !json.getJSONArray("durl").getJSONObject(i).isNull("backup_url")){
                                urlCount+=json.getJSONArray("durl").getJSONObject(i).getJSONArray("backup_url").length();
                            }
                            episodeInfo.urls[i]=new String[urlCount];
                            episodeInfo.urls[i][0]=json.getJSONArray("durl").getJSONObject(i).getString("url");
                            for(int j=1;j<urlCount;j++) {
                                episodeInfo.urls[i][j] = json.getJSONArray("durl").getJSONObject(i).getJSONArray("backup_url").getString(j - 1);
                            }
                        }
                        episodeInfo.SetResult(0,"-");
                        FinishQuery(true);
                        return;
                    }catch (IOException e){
                        episodeInfo.SetResult(1,e.getLocalizedMessage());
                    }catch (JSONException e){
                        episodeInfo.SetResult(2,e.getMessage()+"\n"+ToastStringCut(jsonString));
                    }
                }
                FinishQuery(false);
            }
        };
        task.execute(BilibiliApi.GetVideoLink(1,1,episodeInfo.avidString,episodeInfo.cidString,
                episodeInfo.videoQuality));
    }
}

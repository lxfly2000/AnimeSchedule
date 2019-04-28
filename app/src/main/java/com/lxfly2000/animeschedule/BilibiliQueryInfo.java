package com.lxfly2000.animeschedule;

import android.content.Context;
import com.lxfly2000.utilities.AndroidDownloadFileTask;
import com.lxfly2000.utilities.StreamUtility;
import org.json.JSONException;
import org.json.JSONObject;
import xiaoyaocz.BiliAnimeDownload.Helpers.Api;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public class BilibiliQueryInfo {
    private Context ctx;
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
        public String resultMessage="";
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

    //method:0~4
    public void Query(int method){
        switch (method){
            default:Query();break;
            case 2:Query2();break;
            case 3:Query3();break;
            case 4:Query4();break;
        }
    }
    public void Query(){
        //获取URL
        AndroidDownloadFileTask task=new AndroidDownloadFileTask() {
            @Override
            public void OnReturnStream(ByteArrayInputStream stream, boolean success, Object extra) {
                if(success){
                    try {
                        JSONObject json = new JSONObject(StreamUtility.GetStringFromStream(stream));
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
                        episodeInfo.SetResult(0,"");
                        FinishQuery(true);
                        return;
                    }catch (IOException e){
                        episodeInfo.SetResult(1,e.getLocalizedMessage());
                    }catch (JSONException e){
                        episodeInfo.SetResult(2,e.getMessage());
                    }
                }
                Query2();
            }
        };
        task.execute(Api._playurlApi2(episodeInfo.cidString));
    }

    void Query2(){
        AndroidDownloadFileTask task=new AndroidDownloadFileTask() {
            @Override
            public void OnReturnStream(ByteArrayInputStream stream, boolean success, Object extra) {
                if(success) {
                    try {
                        JSONObject json = new JSONObject(StreamUtility.GetStringFromStream(stream));
                        episodeInfo.parts=json.getJSONArray("data").length();
                        episodeInfo.downloadBytes=new int[episodeInfo.parts];
                        episodeInfo.urls=new String[episodeInfo.parts][];
                        for(int i=0;i<episodeInfo.parts;i++){
                            episodeInfo.downloadBytes[i]=Integer.parseInt(json.getJSONArray("data").getJSONObject(i).getString("size"));
                            episodeInfo.urls[i]=new String[]{json.getJSONArray("data").getJSONObject(i).getString("url")};
                        }
                        episodeInfo.SetResult(0,"");
                        FinishQuery(true);
                        return;
                    }catch (IOException e){
                        episodeInfo.SetResult(1,e.getLocalizedMessage());
                    }catch (JSONException e){
                        episodeInfo.SetResult(2,e.getMessage());
                    }
                }
                Query3();
            }
        };
        task.execute(Api._playurlApi4(episodeInfo.ssidString,episodeInfo.cidString,episodeInfo.epidString));
    }

    void Query3(){
        AndroidDownloadFileTask task=new AndroidDownloadFileTask() {
            @Override
            public void OnReturnStream(ByteArrayInputStream stream, boolean success, Object extra) {
                if(success){
                    try {
                        JSONObject json = new JSONObject(StreamUtility.GetStringFromStream(stream)).getJSONObject("data");
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
                        episodeInfo.SetResult(0,"");
                        FinishQuery(true);
                        return;
                    }catch (IOException e){
                        episodeInfo.SetResult(1,e.getLocalizedMessage());
                    }catch (JSONException e){
                        episodeInfo.SetResult(2,e.getMessage());
                    }
                }
                Query4();
            }
        };
        task.execute(BilibiliApi.GetVideoLink(1,0,episodeInfo.avidString,episodeInfo.cidString,
                episodeInfo.videoQuality));
    }

    void Query4(){
        AndroidDownloadFileTask task=new AndroidDownloadFileTask() {
            @Override
            public void OnReturnStream(ByteArrayInputStream stream, boolean success, Object extra) {
                if(success){
                    try {
                        JSONObject json = new JSONObject(StreamUtility.GetStringFromStream(stream)).getJSONObject("result");
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
                        episodeInfo.SetResult(0,"");
                        FinishQuery(true);
                        return;
                    }catch (IOException e){
                        episodeInfo.SetResult(1,e.getLocalizedMessage());
                    }catch (JSONException e){
                        episodeInfo.SetResult(2,e.getMessage());
                    }
                }
                FinishQuery(false);
            }
        };
        task.execute(BilibiliApi.GetVideoLink(1,1,episodeInfo.avidString,episodeInfo.cidString,
                episodeInfo.videoQuality));
    }
}

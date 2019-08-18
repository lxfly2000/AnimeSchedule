package com.lxfly2000.youget;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.widget.Toast;
import androidx.annotation.NonNull;
import com.lxfly2000.animeschedule.R;
import com.lxfly2000.animeschedule.Values;
import com.lxfly2000.utilities.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

public class AcFunGet extends YouGet{
    private String paramPlayUrl,paramSavePath, fileNameWithoutExt;
    private boolean paramDownloadDanmaku;
    private String htmlString;
    private String videoId;
    private SharedPreferences preferences;
    public AcFunGet(@NonNull Context context){
        super(context);
        preferences= Values.GetPreference(ctx);
    }

    private String GetDanmakuUrl(String videoId){
        return "https://danmu.aixifan.com/V2/"+videoId;
    }

    static class YoukuStreamType{
        ArrayList<String> segUrl;
        int totalSize;
        YoukuStreamType(ArrayList<String> segUrl, int totalSize){
            this.segUrl=segUrl;
            this.totalSize=totalSize;
        }
    }

    private void YoukuAcFunProxy(String vid,String sign,String ref){
        String url=String.format("https://player.acfun.cn/flash_data?vid=%s&ct=85&ev=3&sign=%s&time=%s",vid,sign,System.currentTimeMillis());
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
                    String jsonData=new JSONObject(StreamUtility.GetStringFromStream(iStream,false)).getString("data");
                    byte[]encBytes=Base64.decode(jsonData,Base64.DEFAULT);
                    String decText=new String(Common.RC4("8bdc7e1a".getBytes(),encBytes), StandardCharsets.UTF_8);
                    JSONObject jsonYouku=new JSONObject(decText);

                    HashMap<String,YoukuStreamType> mapYoukuStream=new HashMap<>();
                    JSONArray jArray=jsonYouku.getJSONArray("stream");
                    for(int i=0;i<jArray.length();i++){
                        JSONObject jStream=jArray.getJSONObject(i);
                        String tp=jStream.getString("stream_type");
                        mapYoukuStream.put(tp, new YoukuStreamType(new ArrayList<>(), jStream.getInt("total_size")));
                        if(jStream.has("segs")){
                            JSONArray segs=jStream.getJSONArray("segs");
                            for(int j=0;j<segs.length();j++){
                                JSONObject seg=segs.getJSONObject(j);
                                mapYoukuStream.get(tp).segUrl.add(seg.getString("url"));
                            }
                        }else{
                            mapYoukuStream.put(tp, new YoukuStreamType(JSONUtility.JSONArrayToStringArray(jStream.getJSONArray("m3u8")), jStream.getInt("total_size")));
                        }
                    }
                    AcFunDownloadByVid_Async1(mapYoukuStream);
                }catch (IOException e){
                    onFinishFunction.OnFinish(false,paramPlayUrl,null,ctx.getString(R.string.message_error_on_reading_stream,e.getLocalizedMessage()));
                }catch (JSONException e){
                    onFinishFunction.OnFinish(false,paramPlayUrl,null,ctx.getString(R.string.message_json_exception,e.getLocalizedMessage()));
                }
            }
        };
        Common.SetYouGetHttpHeader(task);
        task.SetReferer(ref);
        task.execute(url);
    }

    private int redirectCount=0;

    private void AcFunDownloadByVid(String vidUrl){
        AndroidDownloadFileTask taskGetVideo=new AndroidDownloadFileTask() {
            @Override
            public void OnReturnStream(ByteArrayInputStream stream, boolean success, int response, Object extra, URLConnection connection) {
                if(onFinishFunction ==null)
                    return;
                if(!success){
                    onFinishFunction.OnFinish(false,paramPlayUrl,null,ctx.getString(R.string.message_unable_to_fetch_anime_info));
                    return;
                }
                if(response==301||response==302){
                    redirectCount++;
                    if(redirectCount>preferences.getInt(ctx.getString(R.string.key_redirect_max_count),Values.vDefaultRedirectMaxCount)){
                        onFinishFunction.OnFinish(false,null,null,ctx.getString(R.string.message_too_many_redirect));
                        return;
                    }
                    AcFunDownloadByVid(connection.getHeaderField("Location"));
                    return;
                }
                try{
                    String enc=connection.getHeaderField("Content-Encoding");
                    InputStream iStream=stream;
                    if("gzip".equals(enc))//判断输入流是否是压缩的，并获取压缩算法
                        iStream=new GZIPInputStream(stream);
                    else if("deflate".equals(enc))
                        iStream=new InflaterInputStream(stream,new Inflater(true));
                    JSONObject jsonInfo=new JSONObject(StreamUtility.GetStringFromStream(iStream,false));
                    String sourceType=jsonInfo.getString("sourceType");
                    if(!sourceType.equals("zhuzhan")){
                        onFinishFunction.OnFinish(false,paramPlayUrl,null,ctx.getString(R.string.message_not_supported_source_type,sourceType));
                        return;
                    }
                    YoukuAcFunProxy(jsonInfo.getString("sourceId"),jsonInfo.getString("encode"),"https://www.acfun.cn/v/ac"+videoId);
                }catch (IOException e){
                    onFinishFunction.OnFinish(false,paramPlayUrl,null,ctx.getString(R.string.message_error_on_reading_stream,e.getLocalizedMessage()));
                }catch (JSONException e){
                    onFinishFunction.OnFinish(false,paramPlayUrl,null,ctx.getString(R.string.message_json_exception,e.getLocalizedMessage()));
                }
            }
        };
        Common.SetYouGetHttpHeader(taskGetVideo);
        taskGetVideo.execute(vidUrl);
    }

    private String videoSavePath;

    private void AcFunDownloadByVid_Async1(HashMap<String,YoukuStreamType> mapYouku){
        String[]seq={"mp4hd3", "mp4hd2", "mp4hd", "flvhd"};
        YoukuStreamType preferred=null;
        for (String t : seq) {
            if (mapYouku.containsKey(t)) {
                preferred = mapYouku.get(t);
                break;
            }
        }
        if(preferred==null)
            return;
        //获取所有链接大小总和的那段代码就不往这加了
        String ext;
        if(Pattern.compile("fid=[0-9A-Z\\-]*.flv").matcher(preferred.segUrl.get(0)).find())
            ext="flv";
        else
            ext="mp4";

        for (int i=0;i< preferred.segUrl.size();i++) {
            AndroidSysDownload sysDownload = new AndroidSysDownload(ctx);
            sysDownload.SetUserAgent(Values.userAgentChromeWindows);
            String cookiePath=Values.GetRepositoryPathOnLocal()+"/cookie_acfun.txt";
            if(FileUtility.IsFileExists(cookiePath))
                sysDownload.SetCookie(FileUtility.ReadFile(cookiePath));
            videoSavePath=paramSavePath+"/"+fileNameWithoutExt;
            if(preferred.segUrl.size()>1)
                videoSavePath+=" ["+i+"]";
            videoSavePath+="."+ext;
            sysDownload.SetOnDownloadFinishReceiver(new AndroidSysDownload.OnDownloadCompleteFunction() {
                @Override
                public void OnDownloadComplete(long downloadId, boolean success, int downloadedSize, int returnedFileSize, Object extra) {
                    Toast.makeText(ctx,ctx.getString(R.string.message_download_finish,videoSavePath),Toast.LENGTH_LONG).show();
                    //TODO:考虑多段的情况
                }
            },videoSavePath);
            sysDownload.StartDownloadFile(preferred.segUrl.get(i),videoSavePath,fileNameWithoutExt);
        }
        if(preferred.segUrl.size()>1){
            Toast.makeText(ctx,"这个视频是多段的，请在下载完成后手动合并。\n自动合并功能尚未制作。",Toast.LENGTH_LONG).show();
        }

        if(paramDownloadDanmaku){
            AndroidDownloadFileTask task=new AndroidDownloadFileTask() {
                @Override
                public void OnReturnStream(ByteArrayInputStream stream, boolean success, int response, Object extra, URLConnection connection) {
                    if(onFinishFunction ==null)
                        return;
                    String enc=connection.getHeaderField("Content-Encoding");
                    InputStream iStream=stream;
                    try{
                        if("gzip".equals(enc))//判断输入流是否是压缩的，并获取压缩算法
                            iStream=new GZIPInputStream(stream);
                        else if("deflate".equals(enc))
                            iStream=new InflaterInputStream(stream,new Inflater(true));
                    }catch (IOException e){/*Ignore*/}
                    String savePath=paramSavePath+"/"+fileNameWithoutExt+".cmt.json";
                    if(!success){
                        onFinishFunction.OnFinish(false,null,savePath,ctx.getString(R.string.message_download_failed,(String)extra));
                        return;
                    }
                    //TODO：为什么下载的弹幕有杂乱内容？
                    if(!FileUtility.WriteStreamToFile(savePath,iStream,false))
                        onFinishFunction.OnFinish(false,null,savePath,ctx.getString(R.string.message_download_failed,(String)extra));
                    else
                        onFinishFunction.OnFinish(true,null,savePath,null);
                }
            };
            Common.SetYouGetHttpHeader(task);
            String danmakuUrl=GetDanmakuUrl(videoId);
            task.SetExtra(danmakuUrl);
            task.execute(danmakuUrl);
        }
    }

    @Override
    public void DownloadBangumi(String url,int episodeToDownload_fromZero,int quality,String saveDirPath){
        DownloadBangumi(url, episodeToDownload_fromZero,0, saveDirPath,true);
    }

    @Override
    public void QueryQualities(String url, int episodeToDownload_fromZero, OnReturnVideoQualityFunction f) {
        ArrayList<VideoQuality>vqs=new ArrayList<>();
        VideoQuality vq=new VideoQuality(0,"");
        vqs.add(vq);
        f.OnReturnVideoQuality(true,vqs);
    }

    public void DownloadBangumi(String url,int episodeToDownload_fromZero,int quality,String saveDirPath,boolean downloadDanmaku){
        DownloadBangumi(url,episodeToDownload_fromZero,quality,saveDirPath,downloadDanmaku,false);
    }

    //注意episodeToDownload是从0数起的
    private void DownloadBangumi(String url,int episodeToDownload_fromZero,int quality,String saveDirPath,boolean downloadDanmaku,boolean episodeFound){
        //参考：https://github.com/soimort/you-get/blob/develop/src/you_get/extractors/acfun.py#L111
        paramPlayUrl=url;
        paramSavePath=saveDirPath;
        paramDownloadDanmaku=downloadDanmaku;
        Matcher mUrl= Pattern.compile("https?://[^\\.]*\\.*acfun\\.[^\\.]+/bangumi/a[ab](\\d+)").matcher(url);
        if(!mUrl.find()){
            if(onFinishFunction !=null)
                onFinishFunction.OnFinish(false,paramPlayUrl,null,ctx.getString(R.string.message_not_supported_url));
            return;
        }
        mUrl=Pattern.compile("/bangumi/a[ab]\\d+").matcher(url);
        if(!mUrl.find()){
            if(onFinishFunction !=null)
                onFinishFunction.OnFinish(false,paramPlayUrl,null,ctx.getString(R.string.message_not_supported_url));
            return;
        }
        paramPlayUrl=url=paramPlayUrl.replaceFirst("/bangumi/aa","/bangumi/ab");
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
                    htmlString= StreamUtility.GetStringFromStream(iStream,false);
                    if(!episodeFound){
                        Matcher mEpi=Pattern.compile("/bangumi/ab\\d+").matcher(paramPlayUrl);
                        if(mEpi.find()){
                            String re="data-href=\""+paramPlayUrl.substring(mEpi.start(),mEpi.end())+"_\\d+_\\d+";
                            ArrayList<String> urlEpisodes=new ArrayList<>();
                            String tempHtml=htmlString;
                            Pattern p=Pattern.compile(re);
                            for(mEpi=p.matcher(tempHtml);mEpi.find();mEpi=p.matcher(tempHtml)){
                                String foundUrl="https://www.acfun.cn"+tempHtml.substring(mEpi.start()+11,mEpi.end());
                                if(!urlEpisodes.contains(foundUrl))
                                    urlEpisodes.add(foundUrl);
                                tempHtml=tempHtml.substring(mEpi.start()+1);
                            }
                            DownloadBangumi(urlEpisodes.get(episodeToDownload_fromZero),episodeToDownload_fromZero,quality,saveDirPath,downloadDanmaku,true);
                        }
                        return;
                    }
                    Matcher m=Pattern.compile("<script>window\\.pageInfo([^<]+)</script>").matcher(htmlString);
                    if(!m.find()){
                        onFinishFunction.OnFinish(false,paramPlayUrl,null,ctx.getString(R.string.message_cannot_fetch_property,m.pattern().toString()));
                        return;
                    }
                    String tagScript=htmlString.substring(m.start(),m.end());
                    String jsonString=tagScript.substring(tagScript.indexOf("{"),tagScript.indexOf("};")+1);
                    JSONObject jsonData=new JSONObject(jsonString);
                    fileNameWithoutExt =FileUtility.ReplaceIllegalPathChar(jsonData.getString("bangumiTitle")+" "+jsonData.getString("episodeName")+" "+jsonData.getString("title"));
                    videoId=String.valueOf(jsonData.getInt("videoId"));
                    AcFunDownloadByVid("https://www.acfun.cn/video/getVideo.aspx?id="+videoId);
                }catch (IOException e){
                    onFinishFunction.OnFinish(false,paramPlayUrl,null,ctx.getString(R.string.message_error_on_reading_stream,e.getLocalizedMessage()));
                }catch (JSONException e){
                    onFinishFunction.OnFinish(false,paramPlayUrl,null,ctx.getString(R.string.message_json_exception,e.getLocalizedMessage()));
                }
            }
        };
        Common.SetYouGetHttpHeader(task);
        task.SetExtra(url);
        task.execute(url);
    }
}

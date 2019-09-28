package com.lxfly2000.youget;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.widget.Toast;
import androidx.annotation.NonNull;
import com.lxfly2000.animeschedule.R;
import com.lxfly2000.animeschedule.Values;
import com.lxfly2000.utilities.*;
import com.lxfly2000.youget.joiner.Joiner;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
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
    public static final String cookiePath= Values.GetRepositoryPathOnLocal()+"/cookie_acfun.txt";
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

    private void AcFunDownloadByM3U8(){
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
                    JSONObject jsonInfo=new JSONObject(StreamUtility.GetStringFromStream(stream,false));
                    DownloadM3U8AllQualities(jsonInfo.getJSONObject("playInfo").getJSONArray("streams").getJSONObject(0).getJSONArray("playUrls").getString(0));
                }catch (IOException e){
                    onFinishFunction.OnFinish(false,paramPlayUrl,null,ctx.getString(R.string.message_error_on_reading_stream,e.getLocalizedMessage()));
                }catch (JSONException e){
                    onFinishFunction.OnFinish(false,paramPlayUrl,null,ctx.getString(R.string.message_json_exception,e.getLocalizedMessage()));
                }
            }
        };
        //参考：https://blog.n1ce.top/blog/2019/07/28/java-spider-acfun-down-2/
        task.execute("https://www.acfun.cn/rest/pc-direct/play/playInfo/m3u8Auto?videoId="+videoId);
    }

    private void DownloadM3U8AllQualities(String m3u8url){
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
                    String m3u8data=StreamUtility.GetStringFromStream(stream,false);
                    String[]sp=m3u8data.substring(m3u8data.indexOf("#EXT-X-STREAM-INF:")+18).split("#EXT-X-STREAM-INF:");
                    ArrayList<VideoQuality>vqs=new ArrayList<>();
                    for (int i=0;i<sp.length;i++){
                        String s=sp[i];
                        String[]spEntry=s.split("\\n");
                        vqs.add(new VideoQuality(Integer.parseInt(Common.Match1(spEntry[0],"BANDWIDTH=(\\d+)")),Common.Match1(spEntry[0],"RESOLUTION=(\\d+x\\d+)"),spEntry[1]));
                    }
                    Collections.sort(vqs, (a, b) -> Integer.compare(b.index,a.index));
                    if(downloadQuality==-1){
                        onReturnQualities.OnReturnVideoQuality(true,vqs);
                        return;
                    }
                    String reqUrl=vqs.get(downloadQuality).url;
                    if(!reqUrl.startsWith("http")&&!reqUrl.startsWith("/"))//相对路径
                        reqUrl=m3u8url.substring(0,m3u8url.lastIndexOf("/")+1)+reqUrl;
                    DownloadM3U8File(reqUrl);
                }catch (IOException e){
                    onFinishFunction.OnFinish(false,paramPlayUrl,null,ctx.getString(R.string.message_error_on_reading_stream,e.getLocalizedMessage()));
                }
            }
        };
        task.execute(m3u8url);
    }

    private void DownloadM3U8File(String m3u8url){
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
                    String str=StreamUtility.GetStringFromStream(stream,false);
                    String[]lines=str.split("\n");
                    ArrayList<String>urlsToDownload=new ArrayList<>();
                    for (String line : lines) {
                        if (!line.startsWith("#")&&line.length()>0) {
                            if(!line.startsWith("http")&&!line.startsWith("/"))//相对路径
                                line=m3u8url.substring(0,m3u8url.lastIndexOf("/")+1)+line;
                            urlsToDownload.add(line);
                        }
                    }
                    DownloadM3U8Streams(urlsToDownload);
                }catch (IOException e){
                    onFinishFunction.OnFinish(false,paramPlayUrl,null,ctx.getString(R.string.message_error_on_reading_stream,e.getLocalizedMessage()));
                }
            }
        };
        task.execute(m3u8url);
    }

    private void DownloadM3U8Streams(ArrayList<String>urls){
        int finishedCount=0;
        downloadStatus=new ArrayList<>();
        for(int i=0;i<urls.size();i++){
            String ext=Common.Match1(urls.get(i),"\\.(\\w+)\\?");
            String path=paramSavePath+"/"+fileNameWithoutExt;
            if(urls.size()>1)
                path+="["+i+"]."+ext;
            videoSavePaths.add(path);
            downloadStatus.add(new DownloadEpisodeStatus(path));
            if(FileUtility.IsFileExists(videoSavePaths.get(i))){
                finishedCount++;
                downloadStatus.get(i).downloaded=true;
            }
        }
        if(finishedCount==videoSavePaths.size()&&finishedCount>1){
            MergeVideos();
            return;
        }
        try {
            for(int i=0;i<urls.size();i++) {
                if(!FileUtility.IsFileExists(videoSavePaths.get(i))) {
                    AndroidSysDownload sysDownload = new AndroidSysDownload(ctx);
                    sysDownload.SetOnDownloadFinishReceiver(new AndroidSysDownload.OnDownloadCompleteFunction() {
                        @Override
                        public void OnDownloadComplete(long downloadId, boolean success, int downloadedSize, int returnedFileSize, Object extra) {
                            downloadStatus.get((int) extra).downloaded=true;
                            if(urls.size()<=1)
                                return;
                            MergeVideos();
                        }
                    }, i);
                    String localPath = videoSavePaths.get(i);
                    String notifyTitle=fileNameWithoutExt;
                    if(urls.size()>1)
                        notifyTitle+= " [" + (i + 1) + "/" + urls.size() + "]";
                    sysDownload.StartDownloadFile(urls.get(i), localPath, notifyTitle);
                }
            }
        }catch (IndexOutOfBoundsException e){
            onFinishFunction.OnFinish(false,paramPlayUrl,null,e.getClass().getName()+"\n"+e.getLocalizedMessage());
        }

        if(paramDownloadDanmaku)
            DownloadDanmaku();
    }

    private int redirectCount=0;

    private void AcFunDownloadByVid(String vidUrl){
        AndroidDownloadFileTask taskGetVideo=new AndroidDownloadFileTask() {
            @Override
            public void OnReturnStream(ByteArrayInputStream stream, boolean success, int response, Object extra, URLConnection connection) {
                if(onFinishFunction ==null)
                    return;
                if(!success){
                    //2019-9-6：经查发现A站查询接口变化，/video/getVideo.aspx接口已无法使用。
                    AcFunDownloadByM3U8();
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
                    if("gzip".equals(enc))//判断输入流是否是压缩的，并获取压缩算as法
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

    static class DownloadEpisodeStatus{
        String savePath;
        boolean downloaded;
        public DownloadEpisodeStatus(String localPath){
            savePath=localPath;
            downloaded=false;
        }
    }

    ArrayList<DownloadEpisodeStatus>downloadStatus;
    ArrayList<String>videoSavePaths=new ArrayList<>();

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

        downloadStatus=new ArrayList<>();
        int downloadedCount=0;
        for (int i=0;i< preferred.segUrl.size();i++) {
            videoSavePaths.add(paramSavePath+"/"+fileNameWithoutExt);
            downloadStatus.add(new DownloadEpisodeStatus(videoSavePaths.get(i)));
            if(FileUtility.IsFileExists(videoSavePaths.get(i))) {
                downloadStatus.get(i).downloaded = true;
                downloadedCount++;
            }
            if(preferred.segUrl.size()>1)
                videoSavePaths.set(i,videoSavePaths.get(i)+" ["+i+"]");
            videoSavePaths.set(i,videoSavePaths.get(i)+"."+ext);
            if(!downloadStatus.get(i).downloaded) {
                AndroidSysDownload sysDownload = new AndroidSysDownload(ctx);
                sysDownload.SetUserAgent(Values.userAgentChromeWindows);
                if (FileUtility.IsFileExists(cookiePath))
                    sysDownload.SetCookie(FileUtility.ReadFile(cookiePath));
                sysDownload.SetOnDownloadFinishReceiver(new AndroidSysDownload.OnDownloadCompleteFunction() {
                    @Override
                    public void OnDownloadComplete(long downloadId, boolean success, int downloadedSize, int returnedFileSize, Object extra) {
                        Toast.makeText(ctx, ctx.getString(R.string.message_download_finish, videoSavePaths.get((int) extra)), Toast.LENGTH_LONG).show();
                        downloadStatus.get((int) extra).downloaded = true;
                        if (downloadStatus.size() <= 1)
                            return;
                        MergeVideos();
                    }
                }, i);
                String notifyTitle=fileNameWithoutExt;
                if(preferred.segUrl.size()>1)
                    notifyTitle+=" [" + (i + 1) + "/" + preferred.segUrl.size() + "]";
                sysDownload.StartDownloadFile(preferred.segUrl.get(i), videoSavePaths.get(i), notifyTitle);
            }
        }
        if(downloadedCount==downloadStatus.size()&&downloadedCount>1){
            MergeVideos();
        }

        if(paramDownloadDanmaku)
            DownloadDanmaku();
    }

    private void DownloadDanmaku(){
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
                String savePath=paramSavePath+"/"+fileNameWithoutExt+".json";
                if(!success){
                    onFinishFunction.OnFinish(false,null,savePath,ctx.getString(R.string.message_download_failed,(String)extra));
                    return;
                }
                try {
                    FileUtility.WriteFile(savePath,StreamUtility.GetStringFromStream(iStream, false));//注意此处如果用WriteStreamToFile可能会出现谜之乱码
                }catch (IOException e){
                    onFinishFunction.OnFinish(false,null,savePath,ctx.getString(R.string.message_download_failed,(String)extra));
                    return;
                }
                onFinishFunction.OnFinish(true,null,savePath,null);
            }
        };
        Common.SetYouGetHttpHeader(task);
        String danmakuUrl=GetDanmakuUrl(videoId);
        task.SetExtra(danmakuUrl);
        task.execute(danmakuUrl);
    }

    private void MergeVideos(){
        for(DownloadEpisodeStatus dp:downloadStatus){
            if(!dp.downloaded)
                return;
        }
        String[]a=new String[videoSavePaths.size()];
        Joiner joiner=Joiner.AutoChooseJoiner(videoSavePaths.toArray(a));
        if(joiner!=null){
            String output=paramSavePath+"/"+fileNameWithoutExt+"."+joiner.getExt();
            if(joiner.join(a,output)==0){
                for(String dPath:a){
                    FileUtility.DeleteFile(dPath);
                }
                Toast.makeText(ctx,ctx.getString(R.string.message_merged_videos)+"\n"+output,Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void DownloadBangumi(String url,int episodeToDownload_fromZero,int quality,String saveDirPath){
        DownloadBangumi(url, episodeToDownload_fromZero,0, saveDirPath,true);
    }

    OnReturnVideoQualityFunction onReturnQualities;

    @Override
    public void QueryQualities(String url, int episodeToDownload_fromZero, OnReturnVideoQualityFunction f){
        QueryQualities(url, episodeToDownload_fromZero, f,false);
    }

    public void QueryQualities(String url, int episodeToDownload_fromZero, OnReturnVideoQualityFunction f,boolean episodeFound) {
        onReturnQualities=f;
        DownloadBangumi(url,episodeToDownload_fromZero,-1,null,false,episodeFound);
    }

    public void DownloadBangumi(String url,int episodeToDownload_fromZero,int quality,String saveDirPath,boolean downloadDanmaku){
        DownloadBangumi(url,episodeToDownload_fromZero,quality,saveDirPath,downloadDanmaku,false);
    }

    private int downloadQuality=-1;

    //注意episodeToDownload是从0数起的
    public void DownloadBangumi(String url,int episodeToDownload_fromZero,int quality,String saveDirPath,boolean downloadDanmaku,boolean episodeFound){
        //参考：https://github.com/soimort/you-get/blob/develop/src/you_get/extractors/acfun.py#L111
        paramPlayUrl=url;
        paramSavePath=saveDirPath;
        paramDownloadDanmaku=downloadDanmaku;
        downloadQuality=quality;
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
        if(FileUtility.IsFileExists(cookiePath))
            task.SetCookie(FileUtility.ReadFile(cookiePath));
        task.execute(url);
    }
}

package com.lxfly2000.youget;

import android.content.Context;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.lxfly2000.animeschedule.R;
import com.lxfly2000.animeschedule.Values;
import com.lxfly2000.utilities.AndroidDownloadFileTask;
import com.lxfly2000.utilities.AndroidSysDownload;
import com.lxfly2000.utilities.FileUtility;
import com.lxfly2000.utilities.StreamUtility;
import com.lxfly2000.youget.joiner.Joiner;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

public class YoukuGet extends YouGet {
    private String paramPlayUrl,paramSavePath, fileNameWithoutExt;
    private String title;
    private int episodeToDownload,downloadQuality;
    String cookiePath= Values.GetRepositoryPathOnLocal()+"/cookie_youku.txt";
    public YoukuGet(@NonNull Context context) {
        super(context);
        stream_types=new HashMap<>();
        stream_types.put("hd3","1080P");
        stream_types.put("hd3v2","1080P");
        stream_types.put("mp4hd3","1080P");
        stream_types.put("mp4hd3v2","1080P");
        stream_types.put("hd2","超清");
        stream_types.put("hd2v2","超清");
        stream_types.put("mp4hd2","超清");
        stream_types.put("mp4hd2v2","超清");
        stream_types.put("mp4hd","高清");
        stream_types.put("flvhd","渣清");
        stream_types.put("3gphd","渣清");
        stream_types.put("mp4sd","标清");
        stream_types.put("flv","标清");
        stream_types.put("mp4","标清");
    }

    HashMap<String,String>stream_types;
    private int GetStreamTypeIndex(String t) {
        String[] stream_types_sort = {"hd3", "hd3v2", "mp4hd3", "mp4hd3v2", "hd2", "hd2v2", "mp4hd2", "mp4hd2v2", "mp4hd", "flvhd", "3gphd", "mp4sd", "flv", "mp4"};
        for(int i=0;i<stream_types_sort.length;i++) {
            if (t.equals(stream_types_sort[i]))
                return i;
        }
        return 0;
    }

    @Override
    public void DownloadBangumi(String url, int episodeToDownload_fromZero, int quality, String saveDirPath) {
        DownloadBangumi(url, episodeToDownload_fromZero, quality, saveDirPath,false);
    }

    public void DownloadBangumi(String url, int episodeToDownload_fromZero, int quality, String saveDirPath, boolean listRetrieved) {
        paramPlayUrl=url;
        paramSavePath=saveDirPath;
        episodeToDownload=episodeToDownload_fromZero;
        downloadQuality=quality;
        if(Pattern.compile(".*list\\.youku\\.com/show/id_.*\\.html").matcher(paramPlayUrl).find()){
            ReadListId();
            return;
        }else if(!listRetrieved){
            GetList();
            return;
        }
        GetData(url);
    }

    private void GetList(){
        AndroidDownloadFileTask task=new AndroidDownloadFileTask() {
            @Override
            public void OnReturnStream(ByteArrayInputStream stream, boolean success, int response, Object extra, URLConnection connection) {
                if(!success){
                    onFinishFunction.OnFinish(false,paramPlayUrl,null,ctx.getString(R.string.message_unable_to_read_url));
                    return;
                }
                try{
                    String htmlString= StreamUtility.GetStringFromStream(stream);
                    Matcher m=Pattern.compile("<a href=\"((http(s)?:)?//)?list.youku.com/show/id_[A-Za-z0-9]+(.html)?\"( \\w+=\"[A-Za-z0-9+\\-=_]*\")* class=\"title\"").matcher(htmlString);
                    if(m.find()){
                        String listIdLine=htmlString.substring(m.start(),m.end());
                        m=Pattern.compile("list.youku.com/show/id_[A-Za-z0-9]+").matcher(listIdLine);
                        if(m.find()){
                            paramPlayUrl="https://list.youku.com/show/id_"+listIdLine.substring(m.start()+23,m.end())+".html";
                            DownloadBangumi(paramPlayUrl,episodeToDownload,downloadQuality,paramSavePath,true);
                            return;
                        }
                    }
                    onFinishFunction.OnFinish(false,paramPlayUrl,null,ctx.getString(R.string.message_unable_to_read_url));
                }catch (IOException e){
                    onFinishFunction.OnFinish(false,paramPlayUrl,null,ctx.getString(R.string.message_error_on_reading_stream,e.getLocalizedMessage()));
                }
            }
        };
        task.SetUserAgent(Values.userAgentChromeWindows);
        task.execute(paramPlayUrl.replace("http:","https:"));
    }

    private void ReadListId(){
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
                    String htmlString=StreamUtility.GetStringFromStream(iStream,false);
                    ReadVideoUrl(Common.Match1(htmlString,"showid:\"(\\d+)\""));
                }catch (IOException e){
                    onFinishFunction.OnFinish(false,paramPlayUrl,null,ctx.getString(R.string.message_error_on_reading_stream,e.getLocalizedMessage()));
                }
            }
        };
        task.execute(paramPlayUrl);
    }

    private void ReadVideoUrl(String listId){
        String requestUrl="https://list.youku.com/show/episode?id="+listId+"&stage=reload_"+(episodeToDownload+1)+"&callback=a";
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
                    String videoId=Common.Match1(StreamUtility.GetStringFromStream(iStream,false),"id_([^\\.]+)\\.html");
                    paramPlayUrl="https://v.youku.com/v_show/id_"+videoId+".html";
                    DownloadBangumi(paramPlayUrl,episodeToDownload,downloadQuality,paramSavePath,true);
                }catch (IOException e){
                    onFinishFunction.OnFinish(false,paramPlayUrl,null,ctx.getString(R.string.message_error_on_reading_stream,e.getLocalizedMessage()));
                }
            }
        };
        task.execute(requestUrl);
    }

    OnReturnVideoQualityFunction onReturnVideoQualityFunction;

    @Override
    public void QueryQualities(String url, int episodeToDownload_fromZero, OnReturnVideoQualityFunction f) {
        onReturnVideoQualityFunction=f;
        DownloadBangumi(url,episodeToDownload_fromZero,-1,null);
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

    String ckey="DIl58SLFxFNndSV1GFNnMQVYkx1PP5tKe1siZu/86PR1u/Wh1Ptd+WOZsHHWxysSfAOhNJpdVWsdVJNsfJ8Sxd8WKVvNfAS8aS8fAOzYARzPyPc3JvtnPHjTdKfESTdnuTW6ZPvk2pNDh4uFzotgdMEFkzQ5wZVXl2Pf1/Y6hLK0OnCNxBj3+nb0v72gZ6b0td+WOZsHHWxysSo/0y9D2K42SaB8Y/+aD2K42SaB8Y/+ahU+WOZsHcrxysooUeND";
    String vid;

    private void GetData(String url){
        vid=Common.Match1(url,"v\\.youku\\.com/v_show/id_(.*)\\.html");
        if(vid==null)
            vid=Common.Match1(url,"m\\.youku\\.com/video/id_(.*).html");
        if(vid==null) {
            onFinishFunction.OnFinish(false, paramPlayUrl, null, ctx.getString(R.string.message_unable_to_read_url));
            return;
        }
        fetch_cna(new OnReturnCNAFunction() {
            @Override
            public void OnReturnCNA(@Nullable String cna) {
                String requestUrl="https://ups.youku.com/ups/get.json?vid="+vid+"&ccode=0519&client_ip=192.168.1.1&utid="+cna+"&client_ts="+(System.currentTimeMillis()/1000)+"&ckey=";
                try{
                    requestUrl+=URLEncoder.encode(ckey,"UTF-8");
                }catch (UnsupportedEncodingException e){
                    onFinishFunction.OnFinish(false,paramPlayUrl,null,e.getLocalizedMessage());
                }
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
                            if(downloadQuality==-1) {
                                GetStreamQualities(StreamUtility.GetStringFromStream(iStream, false),true);
                                return;
                            }
                            DownloadStreams(StreamUtility.GetStringFromStream(iStream, false));
                        }catch (IOException e){
                            onFinishFunction.OnFinish(false,paramPlayUrl,null,ctx.getString(R.string.message_error_on_reading_stream,e.getLocalizedMessage()));
                        }
                    }
                };
                Common.SetYouGetHttpHeader(task);
                task.SetReferer("https://v.youku.com");
                task.SetCookie(FileUtility.ReadFile(cookiePath));
                task.execute(requestUrl);
            }
        });
    }

    private void SortStreamTypes(ArrayList<VideoQuality>vqs){
        Collections.sort(vqs, new Comparator<VideoQuality>() {
            @Override
            public int compare(VideoQuality a, VideoQuality b) {
                return Integer.compare(GetStreamTypeIndex(a.qualityName),GetStreamTypeIndex(b.qualityName));
            }
        });
    }

    private ArrayList<VideoQuality> GetStreamQualities(String jsonString,boolean fReturn){
        try{
            JSONObject jsonData=new JSONObject(jsonString);
            ArrayList<VideoQuality>vqs=new ArrayList<>();
            JSONArray array=jsonData.getJSONObject("data").getJSONArray("stream");
            for(int i=0;i<array.length();i++){
                vqs.add(new VideoQuality(i,array.getJSONObject(i).getString("stream_type")));
            }
            SortStreamTypes(vqs);
            for(VideoQuality vq:vqs){
                vq.qualityName+=" "+stream_types.get(vq.qualityName);
            }
            if(fReturn)
                onReturnVideoQualityFunction.OnReturnVideoQuality(true,vqs);
            return vqs;
        }catch (JSONException e) {
            onFinishFunction.OnFinish(false, paramPlayUrl, null, ctx.getString(R.string.message_json_exception, e.getLocalizedMessage()));
            return null;
        }
    }

    static class DownloadStatus{
        public String localPath;
        public boolean downloaded;
        public DownloadStatus(String localPath){
            downloaded=false;
            this.localPath=localPath;
        }
    }

    ArrayList<DownloadStatus>downloadStatus;
    private void DownloadStreams(String jsonString){
        try{
            int downloadedCount=0;
            JSONObject jsonData=new JSONObject(jsonString);
            title=jsonData.getJSONObject("data").getJSONObject("video").getString("title");
            String series=jsonData.getJSONObject("data").getJSONObject("show").getString("title");
            fileNameWithoutExt=series+" "+(episodeToDownload+1)+" "+title;
            VideoQuality tagQuality=GetStreamQualities(jsonString,false).get(downloadQuality);
            JSONArray array=jsonData.getJSONObject("data").getJSONArray("stream").getJSONObject(tagQuality.index).getJSONArray("segs");
            ArrayList<String>segsUrl=new ArrayList<>();
            for(int i=0;i<array.length();i++){
                segsUrl.add(array.getJSONObject(i).getString("cdn_url"));
            }
            String ext = Common.Match1(segsUrl.get(0),"\\.(\\w+)\\?");
            downloadStatus=new ArrayList<>();
            for(int i=0;i<segsUrl.size();i++) {
                String path=fileNameWithoutExt;
                if(segsUrl.size()>1)
                    path+="["+i+"]";
                path+="."+ext;
                path=paramSavePath+"/"+FileUtility.ReplaceIllegalPathChar(path);
                downloadStatus.add(new DownloadStatus(path));
                if(FileUtility.IsFileExists(path)){
                    downloadedCount++;
                    downloadStatus.get(i).downloaded=true;
                }else {
                    AndroidSysDownload sysDownload = new AndroidSysDownload(ctx);
                    sysDownload.SetOnDownloadFinishReceiver(new AndroidSysDownload.OnDownloadCompleteFunction() {
                        @Override
                        public void OnDownloadComplete(long downloadId, boolean success, int downloadedSize, int returnedFileSize, Object extra) {
                            downloadStatus.get((int)extra).downloaded=true;
                            if(segsUrl.size()<=1)
                                return;
                            MergeVideos();
                        }
                    }, i);
                    sysDownload.SetCookie(FileUtility.ReadFile(cookiePath));
                    sysDownload.StartDownloadFile(segsUrl.get(i),path);
                }
            }
            if(downloadedCount==segsUrl.size()&&downloadedCount>1){
                MergeVideos();
            }
        }catch (JSONException e){
            onFinishFunction.OnFinish(false,paramPlayUrl,null,ctx.getString(R.string.message_json_exception,e.getLocalizedMessage()));
        }catch (IndexOutOfBoundsException e){
            onFinishFunction.OnFinish(false,paramPlayUrl,null,e.getClass().getName()+"\n"+e.getLocalizedMessage());
        }
    }

    public void MergeVideos(){
        for(int i=0;i<downloadStatus.size();i++){
            if(!downloadStatus.get(i).downloaded)
                return;
        }
        String[]a=new String[downloadStatus.size()];
        for(int i=0;i<a.length;i++)
            a[i]=downloadStatus.get(i).localPath;
        Joiner joiner=Joiner.AutoChooseJoiner(a);
        if(joiner!=null){
            String mergePath=paramSavePath+"/"+fileNameWithoutExt+"."+joiner.getExt();
            if(joiner.join(a,mergePath)==0){
                for(String p:a){
                    FileUtility.DeleteFile(p);
                }
                Toast.makeText(ctx,ctx.getString(R.string.message_merged_videos) + "\n" + mergePath, Toast.LENGTH_SHORT).show();
            }
        }
    }
}

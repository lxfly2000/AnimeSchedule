//（参考自you-get）
//因无论a链接还是v链接都能找到albumID属性，故可直接请求观看URL查找albumID，然后：
//请求：
//https://cache.video.iqiyi.com/jp/avlist/205528001/1/50/
//                                        ~~~~~~~~~albumID
//可得到所有分集的观看地址
//
//请求：
//https://www.iqiyi.com/v_19rr9qyzxg.html
//                        ~~~~~~~~~~VID
//收到HTML，
//data-player-tvid="([^"]+)"或tv(?:i|I)d=(.+?)\&或param\[\'tvid\'\]\s*=\s*"(.+?)"查找tvid
//data-player-videoid="([^"]+)"或vid=(.+?)\&或param\[\'vid\'\]\s*=\s*"(.+?)"查找videoID
//tvid:592846600（字符串类型）
//videoID：6b7c9e27935f48b2219129346a7bb851（字符串类型）
//albumID:205528001（字符串类型）
//
//请求：
//http://pcw-api.iqiyi.com/video/video/playervideoinfo?tvid=592846600
//                                                          ~~~~~~~~~tvid
//获得一个JSON
//先判断/code是不是A00000，不是说明有错误
///data/vn+' '+/data/subt可获取分集名称
//
//用上面的两个ID调用getVMS获得一个JSON
//请求：
//http://cache.m.iqiyi.com/tmts/592846600/6b7c9e27935f48b2219129346a7bb851/?t=1566653794219&sc=d532b81ac3798b42fe6957bfb284566d&src=76f90cbd92f94a2e925d83e8ccd22cb7
//还是判断code为A00000
///data/vidl会返回一些不同画质的视频m3u8地址，vd为画质代码
//按查找表按stream_types的顺序将画质由高到低排序
//数据中的m3u就是视频流地址
//根据视频流地址将所有的视频片段下载到本地

package com.lxfly2000.youget;

import android.content.Context;
import android.content.SharedPreferences;
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
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

public class IQiyiGet extends YouGet {
    private String paramPlayUrl,paramSavePath,fileNameWithoutExt;
    private int episodeToDownload;
    private SharedPreferences preferences;
    String cookiePath= Values.GetRepositoryPathOnLocal()+"/cookie_iqiyi.txt";
    public IQiyiGet(@NonNull Context context) {
        super(context);
        preferences=Values.GetPreference(ctx);
    }

    private int downloadQuality=-1;

    @Override
    public void DownloadBangumi(String url, int episodeToDownload_fromZero, int quality, String saveDirPath) {
        paramPlayUrl=url;
        paramSavePath=saveDirPath;
        downloadQuality=quality;
        episodeToDownload=episodeToDownload_fromZero;
        new Iqiyi().GetIQiyiAnimeIDFromURL(paramPlayUrl);
    }

    OnReturnVideoQualityFunction onReturnQualities;

    @Override
    public void QueryQualities(String url, int episodeToDownload_fromZero, @NonNull OnReturnVideoQualityFunction f) {
        onReturnQualities=f;
        DownloadBangumi(url,episodeToDownload_fromZero,-1,null);
    }

    static abstract class OnReturnFunction{
        public abstract void OnReturn(boolean success,Object ret);
    }

    void getVMS(String tvid,String vid,OnReturnFunction f){
        long t=System.currentTimeMillis();
        String src="76f90cbd92f94a2e925d83e8ccd22cb7";
        String key="d5fb4bd9d50c4be6948c97edd7254b0e";
        String sc= HashUtility.GetStringMD5(String.valueOf(t)+key+vid);
        String vmsReq=String.format("http://cache.m.iqiyi.com/tmts/%s/%s/?t=%s&sc=%s&src=%s",tvid,vid,t,sc,src);
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
                    f.OnReturn(true,new JSONObject(StreamUtility.GetStringFromStream(iStream,false)));
                }catch (IOException e){
                    onFinishFunction.OnFinish(false,paramPlayUrl,null,ctx.getString(R.string.message_error_on_reading_stream,e.getLocalizedMessage()));
                }catch (JSONException e){
                    onFinishFunction.OnFinish(false,paramPlayUrl,null,ctx.getString(R.string.message_json_exception,e.getLocalizedMessage()));
                }
            }
        };
        Common.SetYouGetHttpHeader(task);
        if(FileUtility.IsFileExists(cookiePath))
            task.SetCookie(FileUtility.ReadFile(cookiePath));
        task.execute(vmsReq);
    }

    class Iqiyi{
        ArrayList<Common.StreamType>stream_types;
        HashMap<Integer,String>vd_2_id;
        HashMap<String,String>id_2_profile;
        int[]vdSortTable={10,19,5,18,4,14,17,2,21,1,96};
        public Iqiyi(){
            stream_types=new ArrayList<>();
            stream_types.add(new Common.StreamType("4k","m3u8","4k"));
            stream_types.add(new Common.StreamType("BD","m3u8","1080p"));
            stream_types.add(new Common.StreamType("TD","m3u8","720p"));
            stream_types.add(new Common.StreamType("TD_H265","m3u8","720p H265"));
            stream_types.add(new Common.StreamType("HD","m3u8","540p"));
            stream_types.add(new Common.StreamType("HD_H265","m3u8","540p H265"));
            stream_types.add(new Common.StreamType("SD","m3u8","360p"));
            stream_types.add(new Common.StreamType("LD","m3u8","210p"));
            vd_2_id=new HashMap<>();
            vd_2_id.put(10,"4k");
            vd_2_id.put(19,"4k");
            vd_2_id.put(5,"BD");
            vd_2_id.put(18,"BD");
            vd_2_id.put(21,"HD_H265");
            vd_2_id.put(2,"HD");
            vd_2_id.put(4,"TD");
            vd_2_id.put(17,"TD_H265");
            vd_2_id.put(96,"LD");
            vd_2_id.put(1,"SD");
            vd_2_id.put(14,"TD");
            id_2_profile=new HashMap<>();
            id_2_profile.put("4k","4k");
            id_2_profile.put("BD","1080p");
            id_2_profile.put("TD","720p");
            id_2_profile.put("HD","540p");
            id_2_profile.put("SD","360p");
            id_2_profile.put("LD","210p");
            id_2_profile.put("HD_H265","540p H265");
            id_2_profile.put("TD_H265","720p H265");
        }

        private void GetIQiyiAnimeIDFromURL(String url){
            //根据目前（2018-10-1）观察到的情况，爱奇艺的链接无论是a链接还是v链接都有含有“albumId: #########,”代码的脚本，通过此就能查询到番剧的数字ID
            AndroidDownloadFileTask task=new AndroidDownloadFileTask() {
                @Override
                public void OnReturnStream(ByteArrayInputStream stream, boolean success, int response, Object extra,URLConnection connection) {
                    if (onFinishFunction == null)
                        return;
                    if (!success) {
                        onFinishFunction.OnFinish(false, paramPlayUrl, null, ctx.getString(R.string.message_unable_to_fetch_anime_info));
                        return;
                    }
                    Pattern p = Pattern.compile("albumId: *\"?[1-9][0-9]*\"?,");
                    try {
                        String htmlString = StreamUtility.GetStringFromStream(stream);//整个网页的内容
                        Matcher m = p.matcher(htmlString);
                        boolean mfind = false;
                        if (m.find())
                            mfind = true;
                        else {
                            p = Pattern.compile("a(lbum-)?id *= *\"[1-9][0-9]*\"");
                            m = p.matcher(htmlString);
                            if (m.find())
                                mfind = true;
                            else {
                                p=Pattern.compile("\"aid\":[1-9][0-9]*");
                                m=p.matcher(htmlString);
                                if(m.find())
                                    mfind=true;
                                else if(((String)extra).startsWith("http:")) {
                                    GetIQiyiAnimeIDFromURL(((String) extra).replaceFirst("http", "https"));
                                    return;
                                }
                            }
                        }
                        if (mfind) {
                            htmlString = htmlString.substring(m.start(), m.end());//数字ID所在代码的内容
                            Pattern pSub = Pattern.compile("[0-9]+");
                            Matcher mSub = pSub.matcher(htmlString);
                            if (mSub.find())
                                ReadIQiyiJson_OnCallback(htmlString.substring(mSub.start(), mSub.end()));//数字ID的字符串
                            else
                                onFinishFunction.OnFinish(false, paramPlayUrl, null, ctx.getString(R.string.message_unable_get_id_number));
                        } else {
                            if (url.contains("www.iqiyi.com/v_"))//2019-5-1:无法识别www.iqiyi.com/v_开头的链接
                                GetIQiyiAnimeIDFromURL(url.replaceFirst("www\\.iqiyi\\.com", "m.iqiyi.com"));
                            else
                                onFinishFunction.OnFinish(false, paramPlayUrl, null, ctx.getString(R.string.message_unable_get_id_number_line));
                        }
                    } catch (IOException e) {
                        onFinishFunction.OnFinish(false, paramPlayUrl, null, ctx.getString(R.string.message_error_on_reading_stream, e.getLocalizedMessage()));
                    }
                }
            };
            task.SetExtra(url);
            task.execute(url);
        }

        private void ReadIQiyiJson_OnCallback(String albumId){
            String avlistUrl="https://cache.video.iqiyi.com/jp/avlist/"+albumId+"/1/50/";
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
                        String jsonString=StreamUtility.GetStringFromStream(stream);
                        JSONObject jsonData=new JSONObject(jsonString.substring(jsonString.indexOf('{'),jsonString.lastIndexOf('}')+1));
                        JSONArray vlist=jsonData.getJSONObject("data").getJSONArray("vlist");
                        for(int i=0;i<vlist.length();i++){
                            if(episodeToDownload+1==vlist.getJSONObject(i).getInt("pd")){
                                ReadPage(vlist.getJSONObject(i).getString("vurl"));
                                break;
                            }
                        }
                    }catch (IOException e){
                        onFinishFunction.OnFinish(false,paramPlayUrl,null,ctx.getString(R.string.message_error_on_reading_stream,e.getLocalizedMessage()));
                    }catch (JSONException e){
                        onFinishFunction.OnFinish(false,paramPlayUrl,null,ctx.getString(R.string.message_json_exception,e.getLocalizedMessage()));
                    }catch (IndexOutOfBoundsException e){
                        onFinishFunction.OnFinish(false,paramPlayUrl,null,e.getClass().getName()+"\n"+e.getLocalizedMessage());
                    }
                }
            };
            task.execute(avlistUrl);
        }

        int redirectCount=0;
        private void ReadPage(String url){
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
                        if(response==301||response==302){
                            redirectCount++;
                            if(redirectCount>preferences.getInt(ctx.getString(R.string.key_redirect_max_count), Values.vDefaultRedirectMaxCount)){
                                onFinishFunction.OnFinish(false,paramPlayUrl,null,ctx.getString(R.string.message_too_many_redirect));
                            }else {
                                String newURL=connection.getRequestProperty("Location");
                                if(newURL==null)
                                    newURL=url.replace("http:","https:");
                                ReadPage(newURL);
                            }
                            return;
                        }
                        String enc=connection.getHeaderField("Content-Encoding");
                        InputStream iStream=stream;
                        if("gzip".equals(enc))//判断输入流是否是压缩的，并获取压缩算法
                            iStream=new GZIPInputStream(stream);
                        else if("deflate".equals(enc))
                            iStream=new InflaterInputStream(stream,new Inflater(true));
                        String htmlString=StreamUtility.GetStringFromStream(iStream,false);
                        String tvId=Common.Match1(htmlString,"data-player-tvid=\"([^\"]+)\"");
                        if(tvId==null)
                            tvId=Common.Match(htmlString,"tv(?:i|I)d=(.+?)\\&",2);
                        if(tvId==null)
                            tvId=Common.Match1(htmlString,"param\\[\\'tvid\\'\\]\\s*=\\s*\"(.+?)\"");
                        String videoId=Common.Match1(htmlString,"data-player-videoid=\"([^\"]+)\"");
                        if(videoId==null)
                            videoId=Common.Match1(htmlString,"vid=(.+?)\\&");
                        if(videoId==null)
                            videoId=Common.Match1(htmlString,"param\\[\\'vid\\'\\]\\s*=\\s*\"(.+?)\"");
                        GetVideoInfo(tvId,videoId);
                    }catch (IOException e){
                        onFinishFunction.OnFinish(false,paramPlayUrl,null,ctx.getString(R.string.message_error_on_reading_stream,e.getLocalizedMessage()));
                    }
                }
            };
            Common.SetYouGetHttpHeader(task);
            task.execute(url);
        }

        private void GetVideoInfo(String tvid,String videoId){
            getVMS(tvid, videoId, new OnReturnFunction() {
                @Override
                public void OnReturn(boolean success, Object ret) {
                    JSONObject jsonObject=(JSONObject)ret;
                    try{
                        JSONArray vidl=jsonObject.getJSONObject("data").getJSONArray("vidl");
                        ArrayList<Integer>vds=new ArrayList<>();
                        for(int i=0;i<vidl.length();i++){
                            vds.add(vidl.getJSONObject(i).getInt("vd"));
                        }
                        Collections.sort(vds, (a, b) -> {
                            //注意此处查找索引是不能用binarySearch的！它只能对排序后的数组查找。
                            int ia=0,ib=0;
                            for(;ia<vdSortTable.length;ia++){
                                if(a==vdSortTable[ia])
                                    break;
                            }
                            for(;ib<vdSortTable.length;ib++){
                                if(b==vdSortTable[ib])
                                    break;
                            }
                            return Integer.compare(ia,ib);
                        });
                        ArrayList<String>vdStrings=new ArrayList<>();
                        for(int i=0;i<vds.size();i++)
                            vdStrings.add(vd_2_id.get(vds.get(i))+" "+id_2_profile.get(vd_2_id.get(vds.get(i))));
                        if(downloadQuality==-1){
                            ArrayList<VideoQuality>videoQualities=new ArrayList<>();
                            for(int i=0;i<vdStrings.size();i++)
                                videoQualities.add(new VideoQuality(i,vdStrings.get(i)));
                            onReturnQualities.OnReturnVideoQuality(true,videoQualities);
                            return;
                        }
                        for(int i=0;i<vidl.length();i++){
                            if(vidl.getJSONObject(i).getInt("vd")==vds.get(downloadQuality)){
                                String m3u=vidl.getJSONObject(i).getString("m3u");
                                GetEpisodeName(tvid,m3u);
                                break;
                            }
                        }
                    }catch (JSONException e){
                        onFinishFunction.OnFinish(false,null,null,e.getLocalizedMessage());
                    }
                }
            });
        }

        private void GetEpisodeName(String tvid,String m3u){
            String url="http://pcw-api.iqiyi.com/video/video/playervideoinfo?tvid="+tvid;
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
                        JSONObject jsonData=new JSONObject(StreamUtility.GetStringFromStream(iStream,false));
                        if(!jsonData.getString("code").equals("A00000")){
                            onFinishFunction.OnFinish(false,paramPlayUrl,null,jsonData.getString("code"));
                            return;
                        }
                        JSONObject data=jsonData.getJSONObject("data");
                        fileNameWithoutExt=data.getString("vn");
                        if(data.has("subt"))
                            fileNameWithoutExt+=" "+data.getString("subt");
                        fileNameWithoutExt=FileUtility.ReplaceIllegalPathChar(fileNameWithoutExt);
                        ParseM3U8(m3u);
                    }catch (IOException e){
                        onFinishFunction.OnFinish(false,paramPlayUrl,null,ctx.getString(R.string.message_error_on_reading_stream,e.getLocalizedMessage()));
                    }catch (JSONException e){
                        onFinishFunction.OnFinish(false,paramPlayUrl,null,ctx.getString(R.string.message_json_exception,e.getLocalizedMessage()));
                    }
                }
            };
            Common.SetYouGetHttpHeader(task);
            task.execute(url);
        }

        private void ParseM3U8(String m3u){
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
                        DownloadM3U8Main(StreamUtility.GetStringFromStream(iStream,false));
                    }catch (IOException e){
                        onFinishFunction.OnFinish(false,paramPlayUrl,null,ctx.getString(R.string.message_error_on_reading_stream,e.getLocalizedMessage()));
                    }
                }
            };
            Common.SetYouGetHttpHeader(task);
            if(FileUtility.IsFileExists(cookiePath))
                task.SetCookie(FileUtility.ReadFile(cookiePath));
            task.execute(m3u);
        }

        private void DownloadM3U8Main(String m3u8file){
            ArrayList<String>urls=new ArrayList<>();
            Pattern p=Pattern.compile("https?://[^\\n]+");
            while (true){
                Matcher m=p.matcher(m3u8file);
                if(!m.find())
                    break;
                urls.add(m3u8file.substring(m.start(),m.end()));
                m3u8file=m3u8file.substring(m.start()+1);
            }
            DownloadM3U8Seg(urls,0);
        }

        private void DownloadM3U8Seg(ArrayList<String>urls,int seg){
            AndroidSysDownload sysDownload=new AndroidSysDownload(ctx);
            try {
                String ext = Common.Match1(urls.get(seg), "\\.(\\w+)\\?");
                sysDownload.SetOnDownloadFinishReceiver(new AndroidSysDownload.OnDownloadCompleteFunction() {
                    @Override
                    public void OnDownloadComplete(long downloadId, boolean success, int downloadedSize, int returnedFileSize, Object extra) {
                        if (seg + 1 < urls.size()) {
                            DownloadM3U8Seg(urls, seg + 1);
                            return;
                        }
                        ArrayList<String> paths = new ArrayList<>();
                        for (int i = 0; i < seg + 1; i++)
                            paths.add(paramSavePath + "/" + fileNameWithoutExt + "[" + i + "]." + ext);
                        String[] a = new String[paths.size()];
                        paths.toArray(a);
                        Joiner joiner = Joiner.AutoChooseJoiner(a);
                        if(joiner!=null) {
                            String mergePath = paramSavePath + "/" + fileNameWithoutExt + "." + joiner.getExt();
                            if (joiner.join(a, mergePath) == 0) {
                                for (String segFile : a) {
                                    FileUtility.DeleteFile(segFile);
                                }
                                Toast.makeText(ctx, ctx.getString(R.string.message_merged_videos) + "\n" + mergePath, Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }, null);
                String localPath=paramSavePath + "/" + fileNameWithoutExt + "[" + seg + "]." + ext;
                sysDownload.StartDownloadFile(urls.get(seg), localPath, fileNameWithoutExt+" ["+(seg+1)+"/" +urls.size()+"]");
            }catch (IndexOutOfBoundsException e){
                onFinishFunction.OnFinish(false,paramPlayUrl,null,e.getClass().getName()+"\n"+e.getLocalizedMessage());
            }
        }
    }
}

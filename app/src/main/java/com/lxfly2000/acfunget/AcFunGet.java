package com.lxfly2000.acfunget;

import android.content.Context;
import android.util.Base64;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.lxfly2000.animeschedule.R;
import com.lxfly2000.utilities.AndroidDownloadFileTask;
import com.lxfly2000.utilities.FileUtility;
import com.lxfly2000.utilities.JSONUtility;
import com.lxfly2000.utilities.StreamUtility;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AcFunGet {
    private String paramPlayUrl,paramSavePath, fileNameWithoutExt;
    private int paramDownloadEpisodeFromZero;
    private boolean paramDownloadDanmaku;
    private String htmlString;
    private String videoId;
    private Context ctx;
    public AcFunGet(@NonNull Context context){
        ctx=context;
    }

    protected String GetDanmakuUrl(String videoId){
        return "https://danmu.aixifan.com/V2/"+videoId;
    }

    class YoukuStreamType{
        public ArrayList<String> segUrl;
        public int totalSize;
        public YoukuStreamType(ArrayList<String> segUrl,int totalSize){
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
                    onFinishFunction.OnFinish(false,null,null,ctx.getString(R.string.message_unable_to_fetch_anime_info));
                    return;
                }
                try{
                    String jsonData=new JSONObject(StreamUtility.GetStringFromStream(stream)).getString("data");
                    String encText=new String(Base64.decode(jsonData,Base64.DEFAULT));
                    String decText=RC4.decryptRC4(encText,"8bdc7e1a");
                    JSONObject jsonYouku=new JSONObject(decText);

                    HashMap<String,YoukuStreamType> mapYoukuStream=new HashMap<>();
                    JSONArray jArray=jsonYouku.getJSONArray("stream");
                    for(int i=0;i<jArray.length();i++){
                        JSONObject jStream=jArray.getJSONObject(i);
                        String tp=jStream.getString("stream_type");
                        mapYoukuStream.put(tp,new YoukuStreamType(new ArrayList<>(),jStream.getInt("total_size")));
                        if(jStream.has("segs")){
                            JSONArray segs=jStream.getJSONArray("segs");
                            for(int j=0;j<segs.length();j++){
                                JSONObject seg=segs.getJSONObject(j);
                                mapYoukuStream.get(tp).segUrl.add(seg.getString("url"));
                            }
                        }else{
                            mapYoukuStream.put(tp,new YoukuStreamType(JSONUtility.JSONArrayToStringArray(jStream.getJSONArray("m3u8")),jStream.getInt("total_size")));
                        }
                    }
                    AcFunDownloadByVid_Async1(mapYoukuStream);
                }catch (IOException e){
                    onFinishFunction.OnFinish(false,null,null,ctx.getString(R.string.message_error_on_reading_stream,e.getLocalizedMessage()));
                }catch (JSONException e){
                    onFinishFunction.OnFinish(false,null,null,ctx.getString(R.string.message_json_exception,e.getLocalizedMessage()));
                }
            }
        };
        Common.SetAcFunHttpHeader(task);
        task.SetReferer(ref);
        task.execute(url);
    }

    private void AcFunDownloadByVid(){
        AndroidDownloadFileTask taskGetVideo=new AndroidDownloadFileTask() {
            @Override
            public void OnReturnStream(ByteArrayInputStream stream, boolean success, int response, Object extra, URLConnection connection) {
                if(onFinishFunction ==null)
                    return;
                if(!success){
                    onFinishFunction.OnFinish(false,null,null,ctx.getString(R.string.message_unable_to_fetch_anime_info));
                    return;
                }
                try{
                    JSONObject jsonInfo=new JSONObject(StreamUtility.GetStringFromStream(stream));
                    String sourceType=jsonInfo.getString("sourceType");
                    if(!sourceType.equals("zhuzhan")){
                        onFinishFunction.OnFinish(false,null,null,ctx.getString(R.string.message_not_supported_source_type,sourceType));
                        return;
                    }
                    YoukuAcFunProxy(jsonInfo.getString("sourceId"),jsonInfo.getString("encode"),"https://www.acfun.cn/v/ac"+videoId);
                }catch (IOException e){
                    onFinishFunction.OnFinish(false,null,null,ctx.getString(R.string.message_error_on_reading_stream,e.getLocalizedMessage()));
                }catch (JSONException e){
                    onFinishFunction.OnFinish(false,null,null,ctx.getString(R.string.message_json_exception,e.getLocalizedMessage()));
                }
            }
        };
        Common.SetAcFunHttpHeader(taskGetVideo);
        taskGetVideo.execute("http://www.acfun.cn/video/getVideo.aspx?id="+videoId);
    }

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
        int size=0;
        for (String url : preferred.segUrl) {
            //TODO: url_info???
            int segSize=0;
            size+=segSize;
        }
        String ext="";
        if(Pattern.compile("fid=[0-9A-Z\\-]*.flv").matcher(preferred.segUrl.get(0)).find())
            ext="flv";
        else
            ext="mp4";

        //牛批，辣是真的牛批，下载用Python，网盘用Python，翻墙用Python，你是不是沙币啊这种人，
        //下载用Python，网盘用Python，是不是要把你家你公司电脑都装个Python全家桶才觉得有效率吗？惹了你的马了，那个人累了，
        //不想敲键盘了是破坏Python吗？做Java是破坏Python吗？做安卓那么辛苦的时候，我要用Python吗？在你老板电脑上装Python吧，日你马的
        //回去等你被炒吧，像你马个弱智一样，用Python，你电脑炸了，对了吧，我是你哥哥，我们俩都是Python铁粉，你电脑炸了，好吧，你马的弱智
        //用Python，我用你马的赖孩，用Python，你让我怎么用吗，是不是跟个码农一样Python放桌面，老子也学那个GEEK一样的24小时开着Python？
        //用Python，我PY你马，你马PY通红，滚去修Bug吧，沙币一样的，不会搞APP就不要搞，日你的温了，弱智一样的

        //只能等好心人有缘人来帮我把这功能完成了吧……肝不动了
        //TODO:download_urls？？？

        if(paramDownloadDanmaku){
            AndroidDownloadFileTask task=new AndroidDownloadFileTask() {
                @Override
                public void OnReturnStream(ByteArrayInputStream stream, boolean success, int response, Object extra, URLConnection connection) {
                    if(onFinishFunction ==null)
                        return;
                    if(!success){
                        onFinishFunction.OnFinish(false,null,null,ctx.getString(R.string.message_download_failed,(String)extra));
                        return;
                    }
                    String savePath=paramSavePath+"/"+fileNameWithoutExt+".json";
                    if(!FileUtility.WriteStreamToFile(savePath,stream))
                        onFinishFunction.OnFinish(false,null,null,ctx.getString(R.string.message_download_failed,(String)extra));
                    else
                        onFinishFunction.OnFinish(true,null,savePath,ctx.getString(R.string.message_download_finish,(String)extra));
                }
            };
            Common.SetAcFunHttpHeader(task);
            String danmakuUrl=GetDanmakuUrl(videoId);
            task.SetExtra(danmakuUrl);
            task.execute(danmakuUrl);
        }
    }

    public void DownloadBangumi(String url,int dnEpisodeFromZero,String savePath,boolean downloadDanmaku){
        Toast.makeText(ctx,"非常抱歉，针对AcFun的番剧下载功能尚未完成。如果你愿意，你可以在电脑上使用you-get来下载到A站最高画质的视频，这是一个非常好用的支持非常多网站的视频下载工具。\n\n希望能有大佬帮我完善这个功能～！::>_<::",Toast.LENGTH_LONG).show();
        paramPlayUrl=url;
        paramDownloadEpisodeFromZero=dnEpisodeFromZero;
        paramSavePath=savePath;
        paramDownloadDanmaku=downloadDanmaku;
        Matcher mUrl= Pattern.compile("https?://[^\\.]*\\.*acfun\\.[^\\.]+/bangumi/a[ab](\\d+)").matcher(url);
        if(!mUrl.find()){
            if(onFinishFunction !=null)
                onFinishFunction.OnFinish(false,null,null,ctx.getString(R.string.message_not_supported_url));
            return;
        }
        mUrl=Pattern.compile("\\d+").matcher(url);
        if(!mUrl.find()){
            if(onFinishFunction !=null)
                onFinishFunction.OnFinish(false,null,null,ctx.getString(R.string.message_not_supported_url));
            return;
        }
        url="https://www.acfun.cn/bangumi/ab"+url.substring(mUrl.start(),mUrl.end());
        AndroidDownloadFileTask task=new AndroidDownloadFileTask() {
            @Override
            public void OnReturnStream(ByteArrayInputStream stream, boolean success, int response, Object extra, URLConnection connection) {
                if(onFinishFunction ==null)
                    return;
                if(!success){
                    onFinishFunction.OnFinish(false,null,null,ctx.getString(R.string.message_unable_to_fetch_anime_info));
                    return;
                }
                try{
                    htmlString= StreamUtility.GetStringFromStream(stream);
                    Matcher m=Pattern.compile("<script>window\\.pageInfo([^<]+)</script>").matcher(htmlString);
                    if(!m.find()){
                        onFinishFunction.OnFinish(false,null,null,ctx.getString(R.string.message_cannot_fetch_property,m.pattern().toString()));
                        return;
                    }
                    String tagScript=htmlString.substring(m.start(),m.end());
                    String jsonString=tagScript.substring(tagScript.indexOf("{"),tagScript.indexOf("};")+1);
                    JSONObject jsonData=new JSONObject(jsonString);
                    fileNameWithoutExt =jsonData.getString("bangumiTitle")+" "+jsonData.getString("episodeName")+" "+jsonData.getString("title");
                    videoId=String.valueOf(jsonData.getInt("videoId"));
                    AcFunDownloadByVid();
                }catch (IOException e){
                    onFinishFunction.OnFinish(false,null,null,ctx.getString(R.string.message_error_on_reading_stream,e.getLocalizedMessage()));
                }catch (JSONException e){
                    onFinishFunction.OnFinish(false,null,null,ctx.getString(R.string.message_json_exception,e.getLocalizedMessage()));
                }
            }
        };
        Common.SetAcFunHttpHeader(task);
        task.execute(url);
    }

    public static abstract class OnFinishFunction {
        public abstract void OnFinish(boolean success, @Nullable String bangumiPath, @Nullable String danmakuPath, @Nullable String msg);
    }

    private OnFinishFunction onFinishFunction;

    public void SetOnFinish(OnFinishFunction f){
        onFinishFunction =f;
    }
}

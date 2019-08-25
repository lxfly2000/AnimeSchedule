package com.lxfly2000.animeschedule.spider;

import android.content.Context;
import android.widget.Toast;
import com.lxfly2000.animeschedule.AnimeJson;
import com.lxfly2000.animeschedule.R;
import com.lxfly2000.animeschedule.URLUtility;
import com.lxfly2000.animeschedule.Values;
import com.lxfly2000.animeschedule.data.AnimeItem;
import com.lxfly2000.utilities.AndroidDownloadFileTask;
import com.lxfly2000.utilities.StreamUtility;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IQiyiSpider extends Spider {
    public IQiyiSpider(Context context){
        super(context);
    }

    private AnimeItem item=new AnimeItem();

    @Override
    public void Execute(String url){
        GetIQiyiAnimeIDFromURL(url);
    }

    private void GetIQiyiAnimeDescriptionFromTaiwanURL(String url, String htmlString){
        if(htmlString==null){
            AndroidDownloadFileTask task=new AndroidDownloadFileTask() {
                @Override
                public void OnReturnStream(ByteArrayInputStream stream, boolean success, int response, Object extra, URLConnection connection) {
                    if(!success){
                        onReturnDataFunction.OnReturnData(item,STATUS_FAILED,ctx.getString(R.string.message_unable_to_read_url));
                        return;
                    }
                    try {
                        GetIQiyiAnimeDescriptionFromTaiwanURL((String)extra, StreamUtility.GetStringFromStream(stream));
                    }catch (IOException e){
                        onReturnDataFunction.OnReturnData(item,STATUS_FAILED,ctx.getString(R.string.message_error_on_reading_stream,e.getLocalizedMessage()));
                    }
                }
            };
            task.SetExtra(url);
            task.execute(url);
            return;
        }
        Matcher m= Pattern.compile("more-desc *= *\"?[^\"]+\"?").matcher(htmlString);
        if(m.find()){
            String descString=htmlString.substring(m.start(),m.end());
            item.description=descString.substring(descString.indexOf('\"')+1,descString.lastIndexOf('\"'));
            onReturnDataFunction.OnReturnData(item,STATUS_ONGOING,null);
            return;
        }
        if(url.toLowerCase().contains("/v_")&&url.substring(0,url.lastIndexOf('/')).toLowerCase().contains("tw")) {
            Matcher mLink = Pattern.compile(url.substring(url.indexOf(':') + 1, url.lastIndexOf('/')).concat("/a_[a-zA-Z0-9]+\\.html")).matcher(htmlString);
            if(mLink.find())
                GetIQiyiAnimeDescriptionFromTaiwanURL(url.substring(0,url.indexOf(':')+1).concat(htmlString.substring(mLink.start(), mLink.end())), null);
        }
    }

    private void GetIQiyiAnimeActorsInfo(String url,String htmlString){
        if(htmlString==null){
            AndroidDownloadFileTask task=new AndroidDownloadFileTask() {
                @Override
                public void OnReturnStream(ByteArrayInputStream stream, boolean success, int response, Object extra,URLConnection connection) {
                    if(!success){
                        onReturnDataFunction.OnReturnData(item,STATUS_FAILED,ctx.getString(R.string.message_unable_to_read_url));
                        return;
                    }
                    try {
                        GetIQiyiAnimeActorsInfo((String)extra, StreamUtility.GetStringFromStream(stream));
                    }catch (IOException e){
                        onReturnDataFunction.OnReturnData(item,STATUS_FAILED,ctx.getString(R.string.message_error_on_reading_stream,e.getLocalizedMessage()));
                    }
                }
            };
            task.SetExtra(url);
            task.SetUserAgent(Values.userAgentChromeWindows);
            task.execute(url);
            return;
        }
        String strJson= URLUtility.GetIQiyiJsonContainingActorsInfo(htmlString);
        if(strJson!=null){
            try{
                JSONArray jsonCast=new JSONObject(strJson).getJSONArray("dubbers");
                StringBuilder strCast=new StringBuilder();
                for(int j=0;j<jsonCast.length();j++){
                    if(j>0)
                        strCast.append("\n");
                    JSONArray jsonRoles=jsonCast.getJSONObject(j).getJSONArray("roles");
                    for(int i=0;i<jsonRoles.length();i++){
                        if(i>0)
                            strCast.append("，");
                        strCast.append(jsonRoles.getString(i));
                    }
                    strCast.append("：").append(jsonCast.getJSONObject(j).getString("name"));
                }
                item.actors=strCast.toString();
                onReturnDataFunction.OnReturnData(item,STATUS_ONGOING,null);
            }catch (JSONException e){
                onReturnDataFunction.OnReturnData(item,STATUS_FAILED,ctx.getString(R.string.message_json_exception,e.getLocalizedMessage()));
            }
            return;
        }
        if(url.toLowerCase().contains("/a_")) {
            Matcher mLink = Pattern.compile(url.substring(url.indexOf(':') + 1, url.lastIndexOf('/')).concat("/v_[a-zA-Z0-9]+\\.html")).matcher(htmlString);
            if(mLink.find())
                GetIQiyiAnimeActorsInfo(url.substring(0,url.indexOf(':')+1).concat(htmlString.substring(mLink.start(), mLink.end())), null);
        }
        if(url.startsWith("http:"))
            GetIQiyiAnimeActorsInfo(url.replaceFirst("http","https"),null);
    }

    private void GetIQiyiAnimeIDFromURL(String url){
        //根据目前（2018-10-1）观察到的情况，爱奇艺的链接无论是a链接还是v链接都有含有“albumId: #########,”代码的脚本，通过此就能查询到番剧的数字ID
        item.title=url;
        onReturnDataFunction.OnReturnData(item,STATUS_ONGOING,null);
        AndroidDownloadFileTask task=new AndroidDownloadFileTask() {
            @Override
            public void OnReturnStream(ByteArrayInputStream stream, boolean success, int response, Object extra,URLConnection connection) {
                if(!success){
                    onReturnDataFunction.OnReturnData(item,STATUS_FAILED,ctx.getString(R.string.message_unable_to_read_url));
                    return;
                }
                Pattern p=Pattern.compile("albumId: *\"?[1-9][0-9]*\"?,");
                try {
                    String htmlString = StreamUtility.GetStringFromStream(stream);//整个网页的内容
                    Matcher m = p.matcher(htmlString);
                    boolean mfind=false;
                    if(m.find())
                        mfind=true;
                    else{
                        p=Pattern.compile("a(lbum-)?id *= *\"[1-9][0-9]*\"");
                        m=p.matcher(htmlString);
                        if(m.find())
                            mfind=true;
                        else{
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
                    GetIQiyiAnimeActorsInfo((String)extra,htmlString);
                    GetIQiyiAnimeDescriptionFromTaiwanURL((String)extra,htmlString);
                    if(mfind) {
                        htmlString = htmlString.substring(m.start(), m.end());//数字ID所在代码的内容
                        Pattern pSub=Pattern.compile("[0-9]+");
                        Matcher mSub=pSub.matcher(htmlString);
                        if(mSub.find())
                            ReadIQiyiJson_OnCallback(htmlString.substring(mSub.start(),mSub.end()));//数字ID的字符串
                        else
                            onReturnDataFunction.OnReturnData(item,STATUS_FAILED,ctx.getString(R.string.message_unable_get_id_number));
                    }else{
                        if(url.contains("www.iqiyi.com/v_"))//2019-5-1:无法识别www.iqiyi.com/v_开头的链接
                            GetIQiyiAnimeIDFromURL(url.replaceFirst("www\\.iqiyi\\.com","m.iqiyi.com"));
                        else
                            onReturnDataFunction.OnReturnData(item,STATUS_FAILED,ctx.getString(R.string.message_unable_get_id_number_line));
                    }
                }catch (IOException e){
                    onReturnDataFunction.OnReturnData(item,STATUS_FAILED,ctx.getString(R.string.message_error_on_reading_stream,e.getLocalizedMessage()));
                }
            }
        };
        task.SetExtra(url);
        task.execute(url);
    }

    private void ReadIQiyiJson_OnCallback(String idString){
        item.title="Album ID: "+idString;
        onReturnDataFunction.OnReturnData(item,STATUS_ONGOING,null);
        //String jsonUrlGetAlbumId="https://nl-rcd.iqiyi.com/apis/urc/getalbumrc?albumId="+idString;//因为此链接爱奇艺要求登录或验证所以无法使用
        String jsonUrlGetSnsScore="https://pcw-api.iqiyi.com/video/score/getsnsscore?qipu_ids="+idString;
        String jsonpUrlGetAvList="https://cache.video.iqiyi.com/jp/avlist/"+idString+"/1/50/";//这后面的1/50好像没有什么影响的吧……
        /*AndroidDownloadFileTask taskDownloadJsonGetAlbumId=new AndroidDownloadFileTask() {
            @Override
            public void OnReturnStream(ByteArrayInputStream stream, boolean success, Object extra) {
                if(!success){
                    onReturnDataFunction.OnReturnData(item,STATUS_FAILED,"无法获取 GetAlbumRC.");
                    return;
                }
                try {
                    JSONObject jsonObject = new JSONObject(StreamUtility.GetStringFromStream(stream));
                    item.coverUrl=jsonObject.getJSONObject("data").getString("albumImageUrl");
                    item.title=jsonObject.getJSONObject("data").getString("albumName");
                    String tvYearString=String.valueOf(jsonObject.getJSONObject("data").getInt("tvYear"));
                    item.startDate=tvYearString.substring(0,4)+"-"+tvYearString.substring(4,6)+"-"+tvYearString.substring(6);

                    //总集数可能是data.allSet，data.allSets或data.mpd，AvList链接中pt，allNum……中的一个
                    item.episodeCount=jsonObject.getJSONObject("data").getInt("allSet");
                    //item.episodeCount=jsonObject.getJSONObject("data").getInt("allSets");
                    //item.episodeCount=jsonObject.getJSONObject("data").getInt("mpd");
                }catch (JSONException e){
                    onReturnDataFunction.OnReturnData(item,STATUS_FAILED,"发生异常：\n"+e.getLocalizedMessage());
                }catch (IOException e){
                    onReturnDataFunction.OnReturnData(item,STATUS_FAILED,"读取流出错：\n"+e.getLocalizedMessage());
                }
            }
        };*/
        AndroidDownloadFileTask taskDownloadJsonGetSnsScore=new AndroidDownloadFileTask() {
            @Override
            public void OnReturnStream(ByteArrayInputStream stream, boolean success, int response, Object extra,URLConnection connection) {
                if(!success){
                    onReturnDataFunction.OnReturnData(item,STATUS_FAILED,ctx.getString(R.string.message_cannot_fetch_property,"GetSnsScore"));
                    return;
                }
                try {
                    JSONObject jsonObject=new JSONObject(StreamUtility.GetStringFromStream(stream));
                    item.rank=(int)Math.round(jsonObject.getJSONArray("data").getJSONObject(0).getDouble("sns_score")/2);
                }catch (JSONException e){
                    onReturnDataFunction.OnReturnData(item,STATUS_FAILED,ctx.getString(R.string.message_json_exception,e.getLocalizedMessage()));
                }catch (IOException e){
                    onReturnDataFunction.OnReturnData(item,STATUS_FAILED,ctx.getString(R.string.message_error_on_reading_stream,e.getLocalizedMessage()));
                }
                onReturnDataFunction.OnReturnData(item,STATUS_OK,null);
            }
        };
        AndroidDownloadFileTask taskDownloadJsonpGetAvList=new AndroidDownloadFileTask() {
            @Override
            public void OnReturnStream(ByteArrayInputStream stream, boolean success, int response, Object extra,URLConnection connection) {
                if(!success){
                    onReturnDataFunction.OnReturnData(item,STATUS_FAILED,ctx.getString(R.string.message_cannot_fetch_property,"GetAvList"));
                    return;
                }
                try {
                    String jsonString=StreamUtility.GetStringFromStream(stream);
                    jsonString=jsonString.substring(jsonString.indexOf('{'),jsonString.lastIndexOf('}')+1);
                    JSONObject jsonObject=new JSONObject(jsonString);
                    try {
                        JSONArray vlist=jsonObject.getJSONObject("data").getJSONArray("vlist");
                        String descString = vlist.getJSONObject(0).getString("desc");
                        if (descString.length() > 0)
                            item.description=descString;
                        for(int i=0;i<vlist.length();i++){
                            AnimeItem.EpisodeTitle et=new AnimeItem.EpisodeTitle();
                            et.episodeIndex=vlist.getJSONObject(i).getString("pds");
                            et.episodeTitle=vlist.getJSONObject(i).getString("vt");
                            item.episodeTitles.add(et);
                        }
                    }catch (JSONException e){/*Ignore*/}
                    try {
                        String qiyiPlayStrategy = jsonObject.getJSONObject("data").getString("ps");
                        if (qiyiPlayStrategy.contains("每周")) {
                            item.updatePeriod=7;
                            item.updatePeriodUnit= AnimeJson.unitDay;
                        }
                        Matcher mTime = Pattern.compile("[0-9]+:[0-9]+").matcher(qiyiPlayStrategy);
                        if (mTime.find())
                            item.updateTime=qiyiPlayStrategy.substring(mTime.start(), mTime.end());
                    }catch (JSONException e){/*Ignore*/}
                    ReadIQiyiJsonpAnimeCategory_OnCallback(String.valueOf(jsonObject.getJSONObject("data").getJSONArray("vlist").getJSONObject(0).getInt("id")),
                            jsonObject.getJSONObject("data").getJSONArray("vlist").getJSONObject(0).getString("vid"));
                }catch (JSONException e){
                    onReturnDataFunction.OnReturnData(item,STATUS_FAILED,ctx.getString(R.string.message_json_exception,e.getLocalizedMessage()));
                }catch (IOException e){
                    onReturnDataFunction.OnReturnData(item,STATUS_FAILED,ctx.getString(R.string.message_error_on_reading_stream,e.getLocalizedMessage()));
                }catch (IndexOutOfBoundsException e){
                    onReturnDataFunction.OnReturnData(item,STATUS_FAILED,e.getClass().getName()+"\n"+e.getLocalizedMessage());
                }
            }
        };
        //taskDownloadJsonGetAlbumId.execute(jsonUrlGetAlbumId);
        taskDownloadJsonGetSnsScore.execute(jsonUrlGetSnsScore);
        taskDownloadJsonpGetAvList.execute(jsonpUrlGetAvList);
    }

    private void ReadIQiyiJsonpAnimeCategory_OnCallback(String tvidString,String vidString){
        String requestUrl="https://cache.video.iqiyi.com/jp/vi/"+tvidString+"/"+vidString+"/";
        AndroidDownloadFileTask task=new AndroidDownloadFileTask() {
            @Override
            public void OnReturnStream(ByteArrayInputStream stream, boolean success, int response, Object extra,URLConnection connection) {
                if(!success){
                    onReturnDataFunction.OnReturnData(item,STATUS_FAILED,ctx.getString(R.string.message_iqiyi_cannot_fetch_category));
                    return;
                }
                try {
                    String jsonString=StreamUtility.GetStringFromStream(stream);
                    jsonString=jsonString.substring(jsonString.indexOf('{'),jsonString.lastIndexOf('}')+1);
                    JSONObject jsonObject=new JSONObject(jsonString);
                    try {
                        item.categories=jsonObject.getString("tg").split(" ");
                    }catch (JSONException e){/*Ignore*/}
                    try {
                        item.title=jsonObject.getString("an");
                    }catch (JSONException e){/*Ignore*/}
                    try {
                        String startTimeString = jsonObject.getString("stm");
                        if (startTimeString.length() >= 8)
                            item.startDate=startTimeString.substring(0, 4) + "-" + startTimeString.substring(4, 6) + "-" + startTimeString.substring(6);
                        else
                            onReturnDataFunction.OnReturnData(item,STATUS_FAILED, ctx.getString(R.string.message_date_string_too_short, startTimeString.length()));
                    }catch (JSONException e){/*Ignore*/}
                    try {
                        item.episodeCount=jsonObject.getInt("es");//其他地方也有疑似总集数的属性
                    }catch (JSONException e){/*Ignore*/}
                    item.coverUrl=jsonObject.getString("apic");
                }catch (JSONException e){
                    onReturnDataFunction.OnReturnData(item,STATUS_FAILED,ctx.getString(R.string.message_json_exception,e.getLocalizedMessage()));
                }catch (IOException e){
                    onReturnDataFunction.OnReturnData(item,STATUS_FAILED,ctx.getString(R.string.message_error_on_reading_stream,e.getLocalizedMessage()));
                }
                onReturnDataFunction.OnReturnData(item,STATUS_OK,null);
            }
        };
        task.execute(requestUrl);
    }
}

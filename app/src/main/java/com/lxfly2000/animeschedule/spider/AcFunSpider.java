package com.lxfly2000.animeschedule.spider;

import android.content.Context;
import com.lxfly2000.animeschedule.AnimeJson;
import com.lxfly2000.animeschedule.R;
import com.lxfly2000.animeschedule.URLUtility;
import com.lxfly2000.animeschedule.Values;
import com.lxfly2000.animeschedule.data.AnimeItem;
import com.lxfly2000.utilities.AndroidDownloadFileTask;
import com.lxfly2000.utilities.StreamUtility;
import com.lxfly2000.utilities.StringUtility;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URLConnection;
import java.text.SimpleDateFormat;

public class AcFunSpider extends Spider {
    public AcFunSpider(Context context){
        super(context);
    }

    private AnimeItem item=new AnimeItem();

    @Override
    public void Execute(String url){
        //A站链接形式：
        //[1]手机端：https://m.acfun.cn/v/?ab=5024870#[...参数]
        //                                    ~~~~~~~番剧ID，番剧播放/介绍页（手机端的两个页面是合并在一起的）
        //[2]电脑端介绍页：https://www.acfun.cn/bangumi/aa5024870#[...参数]
        //[3]电脑端播放页1：https://www.acfun.cn/bangumi/ab5024870_34169_326905[用下划线加了一些莫名其妙的数字]
        //                                                  GroupID~~~~~ ~~~~~~ItemID
        //   目前(2019-8-11)观察到的结果是同一番剧的GroupID是相同的。
        //[4]电脑端播放页2：https://www.acfun.cn/bangumi/ab5024870
        String bangumiId= URLUtility.GetAcFunBangumiId(url);
        if(bangumiId==null){
            onReturnDataFunction.OnReturnData(item,STATUS_FAILED,ctx.getString(R.string.message_acfun_bangumi_id_not_found));
            return;
        }
        item.title="ID: "+bangumiId;
        onReturnDataFunction.OnReturnData(item,STATUS_ONGOING,null);
        String requestUrl="https://www.acfun.cn/bangumi/aa"+bangumiId;
        AndroidDownloadFileTask task=new AndroidDownloadFileTask() {
            @Override
            public void OnReturnStream(ByteArrayInputStream stream, boolean success, int response, Object extra, URLConnection connection) {
                if(!success){
                    onReturnDataFunction.OnReturnData(item,STATUS_FAILED,ctx.getString(R.string.message_unable_to_fetch_anime_info));
                    return;
                }
                try{
                    String htmlString= StreamUtility.GetStringFromStream(stream);
                    JSONObject albumInfo=new JSONObject(StringUtility.ParseBracketObject(htmlString,htmlString.indexOf("window.pageInfo"),'{','}'));
                    item.title=albumInfo.getString("bangumiTitle");
                    item.coverUrl=albumInfo.getString("bangumiCoverImageV");
                    if(item.description==null||"".equals(item.description))
                        item.description=albumInfo.getString("bangumiIntro");
                    item.updateTime=albumInfo.getString("updateDayTimeStr");
                    item.startDate=new SimpleDateFormat("yyyy-M-d").format(albumInfo.getLong("firstPlayDate"));
                    //分类
                    StringBuilder sb=new StringBuilder();
                    JSONArray tags=albumInfo.getJSONArray("hotTags");
                    for(int i=0;i<tags.length();i++){
                        if(sb.length()>0)
                            sb.append(",");
                        sb.append(tags.getJSONObject(i).getString("name"));
                    }
                    item.categories=sb.toString().split(",");
                    int updateWeekDay=albumInfo.getInt("updateWeekDay");
                    if(updateWeekDay>=1&&updateWeekDay<=7){
                        item.updatePeriod=7;
                        item.updatePeriodUnit= AnimeJson.unitDay;
                    }else{
                        item.updatePeriod=-1;
                        item.updatePeriodUnit=AnimeJson.unitMonth;
                    }
                    //总集数：A站提供的集数是已经播出的集数，不是总集数，所以只能算出已经完结的集数，否则只能用-1表示。
                    if("已完结".equals(albumInfo.getString("extendsStatus"))) {
                        item.episodeCount=albumInfo.getInt("itemCount");//已有集数
                    }else{
                        item.episodeCount=-1;
                    }
                    //AcFun暂无评分系统
                }catch (JSONException e){
                    onReturnDataFunction.OnReturnData(null,STATUS_FAILED,ctx.getString(R.string.message_json_exception,e.getLocalizedMessage()));
                }catch (IOException e){
                    onReturnDataFunction.OnReturnData(null,STATUS_FAILED,ctx.getString(R.string.message_error_on_reading_stream,e.getLocalizedMessage()));
                }
                onReturnDataFunction.OnReturnData(item,STATUS_OK,null);
            }
        };
        task.SetUserAgent(Values.userAgentChromeWindows);
        task.execute(requestUrl);
        GetTitleForEachEpisode(bangumiId,20);
    }

    private void GetTitleForEachEpisode(String bangumiId){
        GetTitleForEachEpisode(bangumiId,0);
    }

    private void GetTitleForEachEpisode(String bangumiId,int epiSize){
        AndroidDownloadFileTask taskEpisodeTitle=new AndroidDownloadFileTask() {
            @Override
            public void OnReturnStream(ByteArrayInputStream stream, boolean success, int response, Object extra, URLConnection connection) {
                if(!success){
                    return;
                }
                try{
                    String htmlString=StreamUtility.GetStringFromStream(stream);
                    JSONObject jsonData=new JSONObject(htmlString).getJSONObject("data");
                    int totalCount=jsonData.getInt("totalCount");
                    //防止出现集数不全的问题
                    //已测试链接：https://www.acfun.cn/bangumi/ab5022156
                    if(jsonData.getInt("size")<totalCount){
                        GetTitleForEachEpisode(bangumiId,totalCount);
                        return;
                    }
                    JSONArray epArray=jsonData.getJSONArray("content");
                    for(int i=0;i<epArray.length();i++){
                        JSONObject epObject=epArray.getJSONObject(i).getJSONArray("videos").getJSONObject(0);
                        AnimeItem.EpisodeTitle et=new AnimeItem.EpisodeTitle();
                        et.episodeIndex=epObject.getString("episodeName");
                        et.episodeTitle=epObject.getString("newTitle");
                        String intr=epObject.getString("intr");
                        if(intr!=null&&intr.length()>0)
                            item.description=intr;
                        et.episodeWatchUrl="https://www.acfun.cn/bangumi/ab"+epObject.getInt("albumId")+"_"+epObject.getInt("groupId")+"_"+epObject.getInt("id");
                        item.episodeTitles.add(et);
                    }
                    onReturnDataFunction.OnReturnData(item,STATUS_OK,null);
                }catch (JSONException e){/*Ignore*/}catch (IOException e){/*Ignore*/}
            }
        };
        taskEpisodeTitle.SetUserAgent(Values.userAgentChromeWindows);
        String queryUrl="https://www.acfun.cn/album/abm/bangumis/video?albumId="+bangumiId;
        if(epiSize>0)
            queryUrl=queryUrl+"&size="+epiSize;
        //https://www.acfun.cn/album/abm/bangumis/video?albumId=[番剧abID]&groupId=[GroupID]&num=1&size=[请求集数]&_=[时间戳毫秒]
        taskEpisodeTitle.execute(queryUrl);
    }
}

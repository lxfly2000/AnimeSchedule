package com.lxfly2000.animeschedule.spider;

import android.content.Context;
import com.lxfly2000.animeschedule.AnimeJson;
import com.lxfly2000.animeschedule.R;
import com.lxfly2000.animeschedule.Values;
import com.lxfly2000.animeschedule.data.AnimeItem;
import com.lxfly2000.utilities.*;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QQVideoSpider extends Spider {
    public QQVideoSpider(Context context){
        super(context);
    }

    private AnimeItem item=new AnimeItem();

    @Override
    public void Execute(String url){
        //腾讯网址格式：
        //应用&移动端：①https://m.v.qq.com/play/play.html?cid=5rs71cp7oy0429e&vid=o0030avj27k&ptag=……&……(后面的参数可以不用管)
        //　　　　　　　　　②https://m.v.qq.com/play.html?cid=5rs71cp7oy0429e&vid=o0030avj27k&ptag=……&……(后面的参数可以不用管)
        //　　　　　　　　　　　　③https://m.v.qq.com/cover/5/5rs71cp7oy0429e.html ~~~~~~~~~~视频（分集的）VideoID
        //                                                     ~~~~~~~~~~~~~~~coverid,也称为cid，其实只管这个CoverID就可以了
        //网页端： ①http://v.qq.com/x/cover/5rs71cp7oy0429e.html
        //　　　　②https://v.qq.com/x/cover/5rs71cp7oy0429e/o0030avj27k.html
        //                                   ~~~~~~~~~~~~~~~同样也只需要管CoverID就行了
        //网页端剧集简介：https://v.qq.com/detail/5/5rs71cp7oy0429e.html
        //                         CoverID的首字符~ ~~~~~~~~~~~~~~~剧集简介也是用CoverID标识的
        String coverId="";
        if(url.contains("m.v.qq.com")){
            Matcher m= Pattern.compile("coverid=[A-Za-z0-9\\-_]+").matcher(url);
            if(m.find()){
                coverId=url.substring(m.start()+8,m.end());
            }else{
                m=Pattern.compile("cid=[A-Za-z0-9\\-_]+").matcher(url);
                if(m.find()){
                    coverId=url.substring(m.start()+4,m.end());
                }else{
                    m=Pattern.compile("m.v.qq.com/cover/[A-Za-z0-9\\-_]/[A-Za-z0-9\\-_]+").matcher(url);
                    if(m.find())
                        coverId=url.substring(m.start()+19,m.end());
                }
            }
        }else if(url.contains("v.qq.com/x/cover/")){
            Matcher m=Pattern.compile("v.qq.com/x/cover/[A-Za-z0-9\\-_]+").matcher(url);
            if(m.find())
                coverId=url.substring(m.start()+17,m.end());
        }else if(url.contains("v.qq.com/detail/")){
            Matcher m=Pattern.compile("v.qq.com/detail/[A-Za-z0-9\\-_]/[A-Za-z0-9\\-_]+").matcher(url);
            if(m.find())
                coverId=url.substring(m.start()+18,m.end());
        }
        if(coverId.equals("")){
            onReturnDataFunction.OnReturnData(null,STATUS_FAILED,ctx.getString(R.string.message_not_supported_url));
            return;
        }
        item.title="CoverID: "+coverId;
        onReturnDataFunction.OnReturnData(item,STATUS_ONGOING,null);
        AndroidDownloadFileTask task=new AndroidDownloadFileTask() {
            @Override
            public void OnReturnStream(ByteArrayInputStream stream, boolean success, int response, Object extra, URLConnection connection) {
                if(!success){
                    onReturnDataFunction.OnReturnData(null,STATUS_FAILED,ctx.getString(R.string.message_unable_to_fetch_anime_info));
                    return;
                }
                try {
                    String htmlString = StreamUtility.GetStringFromStream(stream);
                    JSONObject coverInfo=new JSONObject(StringUtility.ParseBracketObject(htmlString, htmlString.indexOf("var COVER_INFO"), '{', '}'));
                    item.coverUrl=coverInfo.getString("vertical_pic_url");
                    item.title=coverInfo.getString("title");
                    item.description=coverInfo.getString("description");
                    item.actors= JSONUtility.JSONArrayToString(coverInfo.getJSONArray("leading_actor"),"\n");
                    item.staff=JSONUtility.JSONArrayToString(coverInfo.getJSONArray("director_id"),",");
                    item.startDate=coverInfo.getString("publish_date");
                    if(!coverInfo.getString("hollywood_online").equals("")) {
                        String[]sp=coverInfo.getString("hollywood_online").split(" ");
                        if(sp.length>1)
                            item.updateTime=sp[1];
                    }
                    if(coverInfo.getString("update_desc").contains("周")) {
                        item.updatePeriodUnit = AnimeJson.unitDay;
                        item.updatePeriod=7;
                    }
                    try {
                        item.episodeCount = Integer.parseInt(coverInfo.getString("episode_all"));
                    }catch (NumberFormatException e){
                        item.episodeCount=-1;
                    }
                    ArrayList<String>cats=new ArrayList<>();
                    cats.add(coverInfo.getString("main_genre"));
                    String[]subcat=JSONUtility.JSONArrayToString(coverInfo.getJSONArray("subtype"),",").split(",");
                    for(int i=0;i<subcat.length;i++)
                        cats.add(subcat[i]);
                    String[]sa=new String[coverInfo.getJSONArray("subtype").length()+1];
                    item.categories=cats.toArray(sa);
                    try {
                        item.rank = Math.round(Float.parseFloat(coverInfo.getJSONObject("score").getString("score"))/2);
                    }catch (Exception e){
                        item.rank=0;
                    }
                    for(int i=1;i<=item.episodeCount;i++){
                        AnimeItem.EpisodeTitle et=new AnimeItem.EpisodeTitle();
                        et.episodeTitle=item.title+" "+i;
                        et.episodeIndex=String.valueOf(i);
                        item.episodeTitles.add(et);
                    }
                }catch (JSONException e){
                    onReturnDataFunction.OnReturnData(null,STATUS_FAILED,ctx.getString(R.string.message_json_exception,e.getLocalizedMessage()));
                }catch (IOException e){
                    onReturnDataFunction.OnReturnData(null,STATUS_FAILED,ctx.getString(R.string.message_error_on_reading_stream,e.getLocalizedMessage()));
                }
                onReturnDataFunction.OnReturnData(item,STATUS_OK,null);
            }
        };
        task.SetUserAgent(Values.userAgentChromeWindows);
        task.execute("https://v.qq.com/x/cover/"+coverId+".html");
    }
}

package com.lxfly2000.animeschedule.spider;

import android.content.Context;
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

public class AcFunSpider extends Spider {
    public AcFunSpider(Context context){
        super(context);
    }

    private AnimeItem item=new AnimeItem();

    @Override
    public void Execute(String url){
        //A站链接形式：
        //[1]手机端：https://m.acfun.cn/v/?ab=5024870#[...参数]
        //                                    ~~~~~~~番剧播放/介绍页（手机端的两个页面是合并在一起的）
        //[2]电脑端介绍页：https://www.acfun.cn/bangumi/aa5024870#[...参数]
        //[3]电脑端播放页1：https://www.acfun.cn/bangumi/ab5024870_34169_326905[用下划线加了一些莫名其妙的数字]
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
            public void OnReturnStream(ByteArrayInputStream stream, boolean success, int response, Object extra, Object additionalReturned) {
                if(!success){
                    onReturnDataFunction.OnReturnData(item,STATUS_FAILED,ctx.getString(R.string.message_unable_to_fetch_anime_info));
                    return;
                }
                try{
                    String htmlString= StreamUtility.GetStringFromStream(stream);
                    JSONObject albumInfo=new JSONObject(StringUtility.ParseBracketObject(htmlString,htmlString.indexOf("var albumInfo"),'{','}'));
                    item.title=albumInfo.getString("title");
                    item.coverUrl=albumInfo.getString("coverImageV");
                    item.description=albumInfo.getString("intro");
                    item.updateTime=albumInfo.getString("webUpdateTime");
                    item.startDate=albumInfo.getInt("year")+"-"+albumInfo.getInt("month")+"-"+albumInfo.getInt("day");
                    //分类
                    StringBuilder sb=new StringBuilder();
                    JSONArray tags=albumInfo.getJSONArray("tags");
                    for(int i=0;i<tags.length();i++){
                        if(sb.length()>0)
                            sb.append(",");
                        sb.append(tags.getJSONObject(i).getString("name"));
                    }
                    item.categories=sb.toString().split(",");
                    //更新周期：A站目前看来好像没有提供更新周期的属性
                    //总集数：A站提供的集数是已经播出的集数，不是总集数，所以只能算出已经完结的集数，否则只能用-1表示。
                    if(albumInfo.getInt("extendsStatus")==0) {//疑似是放送/完结的标志，1=放送，0=完结
                        item.episodeCount=albumInfo.getInt("contentsCount");//已有集数
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
    }
}

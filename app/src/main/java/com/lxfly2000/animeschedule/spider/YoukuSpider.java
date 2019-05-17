package com.lxfly2000.animeschedule.spider;

import android.content.Context;
import com.lxfly2000.animeschedule.R;
import com.lxfly2000.animeschedule.data.AnimeItem;
import com.lxfly2000.utilities.AndroidDownloadFileTask;
import com.lxfly2000.utilities.StreamUtility;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class YoukuSpider extends Spider {
    public YoukuSpider(Context ctx){
        super(ctx);
    }

    private AnimeItem item=new AnimeItem();

    public void FindListIDByVID(String vid){
        AndroidDownloadFileTask task=new AndroidDownloadFileTask() {
            @Override
            public void OnReturnStream(ByteArrayInputStream stream, boolean success, int response, Object extra,Object additionalReturned) {
                if(!success){
                    onReturnDataFunction.OnReturnData(null,STATUS_FAILED,ctx.getString(R.string.message_unable_to_fetch_anime_id));
                    return;
                }
                try{
                    String htmlString= StreamUtility.GetStringFromStream(stream);
                    Matcher m=Pattern.compile("<a href=\"((http(s)?:)?//)?list.youku.com/show/id_[A-Za-z0-9]+(.html)?\"( \\w+=\"[A-Za-z0-9+\\-=_]*\")* class=\"title\"").matcher(htmlString);
                    if(m.find()){
                        String listIdLine=htmlString.substring(m.start(),m.end());
                        m=Pattern.compile("list.youku.com/show/id_[A-Za-z0-9]+").matcher(listIdLine);
                        if(m.find()){
                            Execute("https://list.youku.com/show/id_"+listIdLine.substring(m.start()+23,m.end())+".html");
                            return;
                        }
                    }
                    onReturnDataFunction.OnReturnData(null,STATUS_FAILED,ctx.getString(R.string.message_unable_to_fetch_anime_id));
                }catch (IOException e){
                    onReturnDataFunction.OnReturnData(null,STATUS_FAILED,ctx.getString(R.string.message_error_on_reading_stream,e.getLocalizedMessage()));
                }
            }
        };
        task.SetUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/74.0.3729.131 Safari/537.36");
        task.execute("https://v.youku.com/v_show/id_"+vid);
    }

    @Override
    public void Execute(String url){
        //优酷网址格式：
        //应用&移动端：https://m.youku.com/video/id_XMzE3MzA2Mzgw.html?……（后面乱七八糟的参数不用管）
        //                                          ~~~~~~~~~~~~~视频VideoID，注意后面可能没有“.html”
        //网页端视频播放页：https://v.youku.com/v_show/id_XMzE3MzA2Mzgw.html?……（后面乱七八糟的参数不用管）
        //                                                ~~~~~~~~~~~~~视频VideoID，注意后面可能没有“.html”
        //网页端剧集简介：https://list.youku.com/show/id_zfcbf969861ad11e0bea1.html?……（后面乱七八糟的参数不用管）
        //                                               ~~~~~~~~~~~~~~~~~~~~~剧集ListID
        String listId="";
        Matcher mUrl= Pattern.compile("[mv].youku.com/(video|v_show)/id_[A-Za-z0-9]+").matcher(url);
        if(mUrl.find()){
            Matcher mId=Pattern.compile("/id_[A-Za-z0-9]+").matcher(url.substring(mUrl.start(),mUrl.end()));
            if(mId.find()) {
                String videoId= url.substring(mUrl.start(), mUrl.end()).substring(mId.start() + 4, mId.end());
                item.title="VideoID: "+videoId;
                onReturnDataFunction.OnReturnData(item,STATUS_ONGOING,null);
                FindListIDByVID(videoId);
                return;
            }
        }else{
            mUrl=Pattern.compile("list.youku.com/show/id_[A-Za-z0-9]+").matcher(url);
            if(mUrl.find())
                listId=url.substring(mUrl.start()+23,mUrl.end());
        }
        if(listId.equals("")){
            onReturnDataFunction.OnReturnData(null,STATUS_FAILED,ctx.getString(R.string.message_not_supported_url));
            return;
        }
        item.title="ListID: "+listId;
        onReturnDataFunction.OnReturnData(item,STATUS_ONGOING,null);

        AndroidDownloadFileTask task=new AndroidDownloadFileTask() {
            @Override
            public void OnReturnStream(ByteArrayInputStream stream, boolean success, int response, Object extra,Object additionalReturned) {
                if(!success){
                    onReturnDataFunction.OnReturnData(null,STATUS_FAILED,ctx.getString(R.string.message_unable_to_fetch_anime_info));
                    return;
                }
                try{
                    String htmlString=StreamUtility.GetStringFromStream(stream);
                    //妈个球优酷的网页可以说是这4个网站里面最难抓的
                    Matcher mHtml=Pattern.compile("<img src=\"((http(s)?:)?//)?r[0-9]+.ykimg.com/[A-Z0-9]+\" alt=\"[^\"]*\"").matcher(htmlString);
                    if(mHtml.find()){
                        String imgLine=htmlString.substring(mHtml.start(),mHtml.end());
                        Matcher mTitle=Pattern.compile("alt=\"[^\"]*\"").matcher(imgLine);
                        if(mTitle.find())
                            item.title=imgLine.substring(mTitle.start()+5,mTitle.end()-1);
                        Matcher mCover=Pattern.compile("src=\"((http(s)?:)?//)?r[0-9]+.ykimg.com/[A-Z0-9]+\"").matcher(imgLine);
                        if(mCover.find()) {
                            item.coverUrl = imgLine.substring(mCover.start() + 5, mCover.end() - 1);
                            if(item.coverUrl.startsWith("//"))
                                item.coverUrl="http:"+item.coverUrl;
                            else if(!item.coverUrl.startsWith("http"))
                                item.coverUrl="http://"+item.coverUrl;
                            if(!item.coverUrl.substring(item.coverUrl.lastIndexOf('/')).contains("."))
                                item.coverUrl=item.coverUrl+".jpg";
                        }
                    }
                    mHtml=Pattern.compile("<span class=\"intro-more hide\">[^<]*</span>").matcher(htmlString);
                    if(mHtml.find()){
                        item.description=htmlString.substring(mHtml.start()+30,mHtml.end()-7);
                        if(item.description.startsWith("简介："))
                            item.description=item.description.substring(3);
                    }
                    mHtml=Pattern.compile("<li class=\"p-row\">声优：.*?</li>").matcher(htmlString);//在通配符后加个问号表示遇到第一个符合的字符串就停止查找
                    if(mHtml.find()){
                        String cvStr=htmlString.substring(mHtml.start(),mHtml.end());
                        cvStr=DeleteAllXmlTags(cvStr);
                        cvStr=cvStr.substring(3);
                        item.actors=cvStr.replaceAll("/","\n");
                    }
                    mHtml=Pattern.compile("<li *>导演：.*?</li>").matcher(htmlString);
                    if(mHtml.find()){
                        item.staff=DeleteAllXmlTags(htmlString.substring(mHtml.start(),mHtml.end())).replace("导演：","监督：");
                    }
                    mHtml=Pattern.compile("<span class=\"pub\"><label>上映：</label>[0-9\\-]+</span>").matcher(htmlString);
                    if(mHtml.find()){
                        item.startDate=DeleteAllXmlTags(htmlString.substring(mHtml.start(),mHtml.end())).substring(3);
                    }
                    mHtml=Pattern.compile("\\d+集全").matcher(htmlString);
                    if(mHtml.find()){
                        String ec=htmlString.substring(mHtml.start(),mHtml.end());
                        item.episodeCount=Integer.parseInt(ec.substring(0,ec.length()-2));
                    }else{
                        item.episodeCount=-1;
                    }
                    mHtml=Pattern.compile("<li>类型：.*?</li>").matcher(htmlString);
                    if(mHtml.find()){
                        item.categories=DeleteAllXmlTags(htmlString.substring(mHtml.start(),mHtml.end())).substring(3).split("/");
                    }
                    mHtml=Pattern.compile("<li class=\"p-score\">评分：.*?</li>").matcher(htmlString);
                    if(mHtml.find()){
                        String rk=DeleteAllXmlTags(htmlString.substring(mHtml.start(),mHtml.end()));
                        Matcher mRk=Pattern.compile("[0-9\\.]+").matcher(rk);
                        if(mRk.find()){
                            item.rank=Math.round(Float.parseFloat(rk.substring(mRk.start(),mRk.end()))/2);
                        }
                    }
                    onReturnDataFunction.OnReturnData(item,STATUS_OK,null);
                }catch (IOException e){
                    onReturnDataFunction.OnReturnData(null,STATUS_FAILED,ctx.getString(R.string.message_error_on_reading_stream,e.getLocalizedMessage()));
                }
            }
        };
        task.SetUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/74.0.3729.131 Safari/537.36");
        task.execute("https://list.youku.com/show/id_"+listId+".html");
    }

    public static String DeleteAllXmlTags(String source){
        int brackets=0;
        StringBuilder sb=new StringBuilder();
        for(int i=0;i<source.length();i++){
            if(source.charAt(i)=='<')
                brackets++;
            else if(source.charAt(i)=='>')
                brackets--;
            else if(brackets==0)
                sb.append(source.charAt(i));
        }
        return sb.toString();
    }
}

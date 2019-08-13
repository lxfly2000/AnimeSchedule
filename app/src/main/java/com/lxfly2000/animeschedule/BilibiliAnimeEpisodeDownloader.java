package com.lxfly2000.animeschedule;

import android.content.Context;
import android.widget.Toast;
import androidx.annotation.NonNull;
import com.lxfly2000.utilities.AndroidDownloadFileTask;
import com.lxfly2000.utilities.AndroidSysDownload;
import com.lxfly2000.utilities.FileUtility;
import com.lxfly2000.utilities.StreamUtility;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

public class BilibiliAnimeEpisodeDownloader {
    private Context ctx;
    public int error=0;
    public BilibiliAnimeEpisodeDownloader(@NonNull Context context){
        ctx=context;
    }

    private JSONObject jsonEntry,checkedEp;
    private String ssidString,epidString,avidString,cidString;
    private int videoQuality;

    public void DownloadEpisode(JSONObject jsonSeason, int indexEpisode, int videoQuality,int firstMethod) {
        this.videoQuality=BilibiliUtility.GetVideoQuality(videoQuality,true).value;
        try {
            //写入entry.json文件
            jsonEntry = new JSONObject(BilibiliUtility.jsonRawBilibiliEntry);
            checkedEp = jsonSeason.getJSONArray("episodes").getJSONObject(indexEpisode);
            jsonEntry.put("is_completed", true);
            jsonEntry.put("total_bytes", 0);//下载所有分段后计算总大小
            jsonEntry.put("type_tag", BilibiliUtility.GetVideoQuality(videoQuality).tag);
            jsonEntry.put("title", jsonSeason.getString("title"));
            jsonEntry.put("cover", jsonSeason.getString("cover"));
            jsonEntry.put("prefered_video_quality", videoQuality);
            jsonEntry.put("danmaku_count", 3000);//需要在弹幕文件下载完成后修改此值
            jsonEntry.put("time_create_stamp", System.currentTimeMillis());
            jsonEntry.put("time_update_stamp", System.currentTimeMillis());
            ssidString=String.valueOf(jsonSeason.getInt("season_id"));
            jsonEntry.put("season_id", ssidString);
            JSONObject jsonSource = new JSONObject();
            jsonSource.put("av_id", checkedEp.getInt("aid"));
            jsonSource.put("cid", checkedEp.getInt("cid"));
            jsonSource.put("website", checkedEp.getString("from"));
            jsonEntry.put("source", jsonSource);
            jsonEntry.getJSONObject("ep").put("av_id", checkedEp.getInt("aid"));
            jsonEntry.getJSONObject("ep").put("page", checkedEp.getInt("page"));
            jsonEntry.getJSONObject("ep").put("danmaku", checkedEp.getInt("cid"));
            jsonEntry.getJSONObject("ep").put("cover", checkedEp.getString("cover"));
            jsonEntry.getJSONObject("ep").put("episode_id", checkedEp.getInt("ep_id"));
            jsonEntry.getJSONObject("ep").put("index", checkedEp.getString("index"));
            jsonEntry.getJSONObject("ep").put("index_title", checkedEp.getString("index_title"));
            jsonEntry.getJSONObject("ep").put("from", checkedEp.getString("from"));
            jsonEntry.getJSONObject("ep").put("season_type", jsonSeason.getInt("season_type"));
            epidString = String.valueOf(checkedEp.getInt("ep_id"));
            avidString=String.valueOf(checkedEp.getInt("aid"));
            cidString=String.valueOf(checkedEp.getInt("cid"));

            //下载danmaku.xml
            AndroidDownloadFileTask taskDownloadDanmaku = new AndroidDownloadFileTask() {
                @Override
                public void OnReturnStream(ByteArrayInputStream stream, boolean success, int response, Object extra, URLConnection connection) {
                    String damakuPath = BilibiliUtility.GetBilibiliDownloadEpisodePath(ctx, ssidString, epidString) + "/danmaku.xml";
                    if (success) {
                        try {
                            //注意弹幕文件返回的输入流是带有Deflate压缩的
                            String xmlString = StreamUtility.GetStringFromStream(new InflaterInputStream(stream,new Inflater(true)),false);
                            FileUtility.WriteFile(damakuPath, xmlString);
                            Matcher matcher = Pattern.compile("<maxlimit>[0-9]+</maxlimit>").matcher(xmlString);
                            if (matcher.find()) {
                                xmlString = xmlString.substring(matcher.start(), matcher.end());
                                matcher = Pattern.compile("[0-9]+").matcher(xmlString);
                                if (matcher.find()) {
                                    int danmaku_maxlimit = Integer.parseInt(xmlString.substring(matcher.start(), matcher.end()));
                                    try {
                                        jsonEntry.put("danmaku_count", danmaku_maxlimit);
                                    } catch (JSONException e) {
                                        Toast.makeText(ctx, ctx.getString(R.string.message_json_exception, e.getLocalizedMessage()), Toast.LENGTH_LONG).show();
                                    }
                                }
                            }
                        } catch (IOException e) {
                            error=1;
                            Toast.makeText(ctx, ctx.getString(R.string.message_io_exception, e.getLocalizedMessage()), Toast.LENGTH_LONG).show();
                        }
                    } else {
                        error=1;
                        Toast.makeText(ctx, ctx.getString(R.string.message_download_failed, damakuPath), Toast.LENGTH_LONG).show();
                    }
                    DownloadEpisode_QueryLinksAndSaveEntryJson(firstMethod);
                }
            };
            taskDownloadDanmaku.SetAcceptEncoding("deflate");
            taskDownloadDanmaku.execute("http://comment.bilibili.com/" + checkedEp.getInt("cid") + ".xml");
        } catch (JSONException e) {
            error=1;
            Toast.makeText(ctx, ctx.getString(R.string.message_json_exception, e.getLocalizedMessage()), Toast.LENGTH_LONG).show();
        }
    }

    private int paramQueryMethod,queryTriesCount=0;
    private void DownloadEpisode_QueryLinksAndSaveEntryJson(int queryMethod) {
        queryTriesCount++;
        if(queryTriesCount>BilibiliQueryInfo.queryMethodCount){
            Toast.makeText(ctx,R.string.message_bilibili_video_link_all_failed,Toast.LENGTH_SHORT).show();
            return;
        }
        paramQueryMethod=queryMethod;
        BilibiliQueryInfo queryInfo = new BilibiliQueryInfo(ctx);
        queryInfo.SetParam(ssidString, epidString, avidString, cidString, videoQuality);
        queryInfo.SetOnReturnEpisodeInfoFunction(new BilibiliQueryInfo.OnReturnEpisodeInfoFunction() {
            @Override
            public void OnReturnEpisodeInfo(BilibiliQueryInfo.EpisodeInfo info,boolean success) {
                if(!success){
                    error=1;
                    Toast.makeText(ctx,info.resultMessage,Toast.LENGTH_LONG).show();
                    DownloadEpisode_QueryLinksAndSaveEntryJson((paramQueryMethod+1)%BilibiliQueryInfo.queryMethodCount);
                    return;
                }
                try {
                    jsonEntry.put("total_bytes", info.GetDownloadBytesSum());
                } catch (JSONException e) {
                    error=1;
                }
                FileUtility.WriteFile(BilibiliUtility.GetBilibiliDownloadEntryPath(ctx, ssidString, epidString), jsonEntry.toString());
                DownloadEpisode_Video(info);
            }
        });
        queryInfo.Query(paramQueryMethod);
    }

    private void DownloadEpisode_Video(BilibiliQueryInfo.EpisodeInfo info){
        String episodeVideoQualityPath=BilibiliUtility.GetBilibiliDownloadEpisodePath(ctx,ssidString,epidString)+"/"+
                BilibiliUtility.GetVideoQuality(videoQuality).tag;
        try {
            //写入index.json文件
            JSONObject jsonIndex = new JSONObject(BilibiliUtility.jsonRawBilibiliEpisodeIndex);
            jsonIndex.put("from", checkedEp.getString("from"));
            jsonIndex.put("type_tag", BilibiliUtility.GetVideoQuality(videoQuality).tag);
            jsonIndex.put("description", BilibiliUtility.GetVideoQuality(videoQuality).desc);
            //https://github.com/xiaoyaocz/BiliAnimeDownload/blob/852eb5b4fb3fdbd9801be2c6e98f69e3ed4d427a/BiliAnimeDownload/BiliAnimeDownload/MainPage.xaml.cs#L342
            jsonIndex.put("parse_timestamp_milli", System.currentTimeMillis());//当前时间戳（毫秒）
            JSONObject jsonSegment=jsonIndex.getJSONArray("segment_list").getJSONObject(0);
            for(int i=0;i<info.parts;i++) {
                jsonSegment.put("url", info.urls[i][0]);//视频URL
                jsonSegment.put("bytes", info.downloadBytes[i]);//分段的视频大小（字节数）
                if(info.urls[i].length>1){
                    for(int i_backup_url=1;i_backup_url<info.urls[i].length;i_backup_url++){
                        jsonSegment.getJSONArray("backup_urls").put(i_backup_url-1,info.urls[i][i_backup_url]);
                    }
                }else {
                    jsonSegment.remove("backup_urls");//如果没有其他URL则删除备用URL
                }
                jsonIndex.getJSONArray("segment_list").put(i,jsonSegment);

                //写入sum文件
                JSONObject sumFile = new JSONObject();
                sumFile.put("length", info.downloadBytes[i]);//分段的视频大小（字节数）
                FileUtility.WriteFile(episodeVideoQualityPath + "/"+i+".blv.4m.sum", sumFile.toString());

                //执行系统下载
                String localPath=episodeVideoQualityPath + "/"+i+".blv";
                //分段下载可测试SSID:2762,EP:7
                //Toast.makeText(ctx,"API:"+paramQueryMethod+" Seg:"+i+"/"+info.parts+" URLNum:"+info.urls[i].length,Toast.LENGTH_SHORT).show();
                if(FileUtility.IsFileExists(localPath)) {
                    Toast.makeText(ctx, ctx.getString(R.string.message_file_exists_skip_download, localPath), Toast.LENGTH_LONG).show();
                }else {
                    DownloadMultilinks(info.urls[i], 0,
                            info.urls[i][0].contains("360")?null:"https://www.bilibili.com/bangumi/play/ep"+info.epidString, info.downloadBytes[i], localPath,
                            "[" + checkedEp.getString("index") + "] " + checkedEp.getString("index_title") + " - " + i);
                }
            }
            FileUtility.WriteFile(BilibiliUtility.GetBilibiliDownloadEpisodeIndexPath(ctx, ssidString, epidString, videoQuality), jsonIndex.toString());
        }catch (JSONException e){
            error=1;
            Toast.makeText(ctx, ctx.getString(R.string.message_json_exception, e.getLocalizedMessage()), Toast.LENGTH_LONG).show();
            DownloadEpisode_QueryLinksAndSaveEntryJson((paramQueryMethod+1)%BilibiliQueryInfo.queryMethodCount);
            return;
        }
        if(info.queryResult!=0){
            error=1;
            Toast.makeText(ctx,info.resultMessage,Toast.LENGTH_LONG).show();
        }
    }

    private void DownloadMultilinks(String[]links,int ilink,String referer,int expectSize,String localPath,String notifyTitle){
        if(ilink>=links.length) {
            DownloadEpisode_QueryLinksAndSaveEntryJson((paramQueryMethod+1)%BilibiliQueryInfo.queryMethodCount);
            return;
        }else if(links[ilink].contains("8986943")){//https://github.com/xiaoyaocz/BiliAnimeDownload/blob/852eb5b4fb3fdbd9801be2c6e98f69e3ed4d427a/BiliAnimeDownload/BiliAnimeDownload/Helpers/Util.cs#L78
            Toast.makeText(ctx,R.string.message_bilibili_sorry_region_restrict_anime,Toast.LENGTH_LONG).show();
            DownloadMultilinks(links,ilink+1,referer,expectSize,localPath,notifyTitle);
            return;
        }
        AndroidSysDownload sysDownload=new AndroidSysDownload(ctx);
        sysDownload.SetUserAgent("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.1 (KHTML, like Gecko) Chrome/22.0.1207.1 Safari/537.1");
        String cookiePath=Values.GetRepositoryPathOnLocal()+"/cookie.txt";
        if(referer!=null)
            sysDownload.SetReferer(referer);
        if(FileUtility.IsFileExists(cookiePath))
            sysDownload.SetCookie(FileUtility.ReadFile(cookiePath));
        sysDownload.SetOnDownloadFinishReceiver(new AndroidSysDownload.OnDownloadCompleteFunction() {
            @Override
            public void OnDownloadComplete(long downloadId,boolean success,int downloadedSize,int returnedSize,Object extra) {
                //Toast.makeText(ctx,"API:"+paramQueryMethod+" ["+(ilink+1)+"/"+links.length+"] S:"+success+
                //        " Expect:"+expectSize+" Dn:"+downloadedSize+" Total:"+returnedSize,Toast.LENGTH_SHORT).show();
                //if(!success||downloadedSize!=expectSize||expectSize!=returnedSize)//这样判断好像不起作用
                if(FileUtility.GetFileSize(localPath)==expectSize) {//暂时先这样凑合着吧
                    Toast.makeText(ctx, ctx.getString(R.string.message_download_finish, localPath), Toast.LENGTH_LONG).show();
                }else {
                    FileUtility.DeleteFile(localPath);
                    DownloadMultilinks(links, (int) extra + 1,referer, expectSize, localPath, notifyTitle);
                }
            }
        },ilink);
        sysDownload.StartDownloadFile(links[ilink],localPath,notifyTitle);
    }
}

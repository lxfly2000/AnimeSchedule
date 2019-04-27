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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BilibiliAnimeEpisodeDownloader {
    private Context ctx;
    public BilibiliAnimeEpisodeDownloader(@NonNull Context context){
        ctx=context;
    }

    public boolean DownloadEpisode(String savePath, JSONObject jsonSeason, int indexEpisode, int videoQuality){
        try {
            //写入entry.json文件
            JSONObject jsonEntry = new JSONObject(BilibiliUtility.jsonRawBilibiliEntry);
            JSONObject checkedEp = jsonSeason.getJSONArray("episodes").getJSONObject(indexEpisode);
            jsonEntry.put("is_completed",true);
            jsonEntry.put("total_bytes",0);//TODO:下载所有分段后计算总大小
            jsonEntry.put("type_tag",BilibiliUtility.GetVideoQuality(videoQuality).tag);
            jsonEntry.put("title", jsonSeason.getString("title"));
            jsonEntry.put("cover", jsonSeason.getString("cover"));
            jsonEntry.put("prefered_video_quality", videoQuality);
            jsonEntry.put("danmaku_count",3000);//需要在弹幕文件下载完成后修改此值
            jsonEntry.put("time_create_stamp", System.currentTimeMillis());
            jsonEntry.put("time_update_stamp", System.currentTimeMillis());
            jsonEntry.put("season_id", String.valueOf(jsonSeason.getInt("season_id")));
            JSONObject jsonSource=new JSONObject();
            jsonSource.put("av_id",checkedEp.getInt("aid"));
            jsonSource.put("cid",checkedEp.getInt("cid"));
            jsonSource.put("website",checkedEp.getString("from"));
            jsonEntry.put("source",jsonSource);
            jsonEntry.getJSONObject("ep").put("av_id", checkedEp.getInt("aid"));
            jsonEntry.getJSONObject("ep").put("page", checkedEp.getInt("page"));
            jsonEntry.getJSONObject("ep").put("danmaku", checkedEp.getInt("cid"));
            jsonEntry.getJSONObject("ep").put("cover", checkedEp.getString("cover"));
            jsonEntry.getJSONObject("ep").put("episode_id", checkedEp.getInt("ep_id"));
            jsonEntry.getJSONObject("ep").put("index", checkedEp.getString("index"));
            jsonEntry.getJSONObject("ep").put("index_title", checkedEp.getString("index_title"));
            jsonEntry.getJSONObject("ep").put("from", checkedEp.getString("from"));
            jsonEntry.getJSONObject("ep").put("season_type", jsonSeason.getInt("season_type"));
            String ssidString=jsonEntry.getString("season_id");
            String epidString=String.valueOf(checkedEp.getInt("ep_id"));
            String entryPath=BilibiliUtility.GetBilibiliDownloadEntryPath(ctx,ssidString,epidString);
            FileUtility.WriteFile(entryPath, jsonEntry.toString());

            //下载danmaku.xml
            AndroidDownloadFileTask taskDownloadDanmaku=new AndroidDownloadFileTask() {
                @Override
                public void OnReturnStream(ByteArrayInputStream stream, boolean success, Object extra) {
                    String damakuPath=BilibiliUtility.GetBilibiliDownloadEpisodePath(ctx,ssidString,epidString)+"/danmaku.xml";
                    if(success){
                        try {
                            String xmlString = StreamUtility.GetStringFromStream(stream);
                            FileUtility.WriteFile(damakuPath, xmlString);
                            Matcher matcher= Pattern.compile("<maxlimit>[0-9]+</maxlimit>").matcher(xmlString);
                            if(matcher.find()) {
                                xmlString = xmlString.substring(matcher.start(), matcher.end());
                                matcher=Pattern.compile("[0-9]+").matcher(xmlString);
                                if(matcher.find()){
                                    int danmaku_maxlimit=Integer.parseInt(xmlString.substring(matcher.start(),matcher.end()));
                                    try {
                                        jsonEntry.put("danmaku_count", danmaku_maxlimit);
                                        FileUtility.WriteFile(entryPath, jsonEntry.toString());
                                    }catch (JSONException e){
                                        Toast.makeText(ctx,ctx.getString(R.string.message_json_exception,e.getLocalizedMessage()),Toast.LENGTH_LONG).show();
                                    }
                                }
                            }
                        }catch (IOException e){
                            Toast.makeText(ctx,ctx.getString(R.string.message_io_exception,e.getLocalizedMessage()),Toast.LENGTH_LONG).show();
                        }
                    }else{
                        Toast.makeText(ctx,ctx.getString(R.string.message_download_failed,damakuPath),Toast.LENGTH_LONG).show();
                    }
                }
            };
            taskDownloadDanmaku.execute("http://comment.bilibili.com/"+checkedEp.getInt("cid")+".xml");

            //TODO：获取视频URL

            String episodeVideoQualityPath=BilibiliUtility.GetBilibiliDownloadEpisodePath(ctx,ssidString,epidString)+"/"+
                    BilibiliUtility.GetVideoQuality(videoQuality);
            //写入index.json文件
            JSONObject jsonIndex=new JSONObject(BilibiliUtility.jsonRawBilibiliEpisodeIndex);
            jsonIndex.put("from",checkedEp.getString("from"));
            jsonIndex.put("type_tag",BilibiliUtility.GetVideoQuality(videoQuality).tag);
            jsonIndex.put("description",BilibiliUtility.GetVideoQuality(videoQuality).desc);
            jsonIndex.put("parse_timestamp_milli",0);//TODO:弄清这个值表示什么
            jsonIndex.getJSONArray("segment_list").getJSONObject(0/*TODO:需要知道分段数*/).put("url","视频URL");//TODO：视频URL
            jsonIndex.getJSONArray("segment_list").getJSONObject(0/*TODO:需要知道分段数*/).put("bytes",0);//TODO：分段的视频大小（字节数）
            jsonIndex.getJSONArray("segment_list").getJSONObject(0/*TODO:需要知道分段数*/).remove("backup_urls");//TODO：如果没有其他URL则删除备用URL
            FileUtility.WriteFile(BilibiliUtility.GetBilibiliDownloadEpisodeIndexPath(ctx,ssidString,epidString,videoQuality),jsonIndex.toString());

            //执行系统下载
            AndroidSysDownload sysDownload=new AndroidSysDownload(ctx);
            sysDownload.StartDownloadFile("视频URL",episodeVideoQualityPath+"/0.blv","["+checkedEp.getString("index")+"] "+
                    checkedEp.getString("index_title"));

            //写入sum文件
            JSONObject sumFile=new JSONObject();
            sumFile.put("length",0);//TODO:分段的视频大小（字节数）
            FileUtility.WriteFile(episodeVideoQualityPath+"/0.blv.4m.sum",sumFile.toString());
        }catch (JSONException e){
            return false;
        }
        return true;
    }
}

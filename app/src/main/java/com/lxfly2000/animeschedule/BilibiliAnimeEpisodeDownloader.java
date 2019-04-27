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

    private JSONObject jsonEntry,checkedEp;
    String ssidString,epidString,avidString,cidString;
    int videoQuality;

    public void DownloadEpisode(String savePath, JSONObject jsonSeason, int indexEpisode, int videoQuality) {
        this.videoQuality=videoQuality;
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
            ssidString=String.valueOf(jsonEntry.getInt("season_id"));
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
                public void OnReturnStream(ByteArrayInputStream stream, boolean success, Object extra) {
                    String damakuPath = BilibiliUtility.GetBilibiliDownloadEpisodePath(ctx, ssidString, epidString) + "/danmaku.xml";
                    if (success) {
                        try {
                            String xmlString = StreamUtility.GetStringFromStream(stream);
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
                            Toast.makeText(ctx, ctx.getString(R.string.message_io_exception, e.getLocalizedMessage()), Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(ctx, ctx.getString(R.string.message_download_failed, damakuPath), Toast.LENGTH_LONG).show();
                    }
                    DownloadEpisode_SaveEntryJson();
                }
            };
            taskDownloadDanmaku.execute("http://comment.bilibili.com/" + checkedEp.getInt("cid") + ".xml");
        } catch (JSONException e) {
            Toast.makeText(ctx, ctx.getString(R.string.message_json_exception, e.getLocalizedMessage()), Toast.LENGTH_LONG).show();
        }
    }

    private void DownloadEpisode_SaveEntryJson() {
        BilibiliQueryInfo queryInfo = new BilibiliQueryInfo(ctx);
        queryInfo.SetParam(ssidString, epidString, avidString, cidString, videoQuality);
        queryInfo.SetOnReturnEpisodeInfoFunction(new BilibiliQueryInfo.OnReturnEpisodeInfoFunction() {
            @Override
            public void OnReturnEpisodeInfo(BilibiliQueryInfo.EpisodeInfo info) {
                try {
                    jsonEntry.put("total_bytes", info.downloadBytes);
                } catch (JSONException e) {/*Ignore*/}
                FileUtility.WriteFile(BilibiliUtility.GetBilibiliDownloadEntryPath(ctx, ssidString, epidString), jsonEntry.toString());
                DownloadEpisode_Video(info);
            }
        });
        queryInfo.Query();
    }

    private void DownloadEpisode_Video(BilibiliQueryInfo.EpisodeInfo info){
        String episodeVideoQualityPath=BilibiliUtility.GetBilibiliDownloadEpisodePath(ctx,ssidString,epidString)+"/"+
                BilibiliUtility.GetVideoQuality(videoQuality);
        try {
            //写入index.json文件
            JSONObject jsonIndex = new JSONObject(BilibiliUtility.jsonRawBilibiliEpisodeIndex);
            jsonIndex.put("from", checkedEp.getString("from"));
            jsonIndex.put("type_tag", BilibiliUtility.GetVideoQuality(videoQuality).tag);
            jsonIndex.put("description", BilibiliUtility.GetVideoQuality(videoQuality).desc);
            //https://github.com/xiaoyaocz/BiliAnimeDownload/blob/852eb5b4fb3fdbd9801be2c6e98f69e3ed4d427a/BiliAnimeDownload/BiliAnimeDownload/MainPage.xaml.cs#L342
            for(int i=0;i<info.parts;i++) {
                jsonIndex.put("parse_timestamp_milli", System.currentTimeMillis());//当前时间戳（毫秒）
                jsonIndex.getJSONArray("segment_list").getJSONObject(i).put("url", info.urls[i]);//视频URL
                jsonIndex.getJSONArray("segment_list").getJSONObject(i).put("bytes", info.downloadBytes[i]);//分段的视频大小（字节数）
                jsonIndex.getJSONArray("segment_list").getJSONObject(i).remove("backup_urls");//如果没有其他URL则删除备用URL
                FileUtility.WriteFile(BilibiliUtility.GetBilibiliDownloadEpisodeIndexPath(ctx, ssidString, epidString, videoQuality), jsonIndex.toString());

                //写入sum文件
                JSONObject sumFile = new JSONObject();
                sumFile.put("length", info.downloadBytes[i]);//分段的视频大小（字节数）
                FileUtility.WriteFile(episodeVideoQualityPath + "/"+i+".blv.4m.sum", sumFile.toString());

                //执行系统下载
                AndroidSysDownload sysDownload = new AndroidSysDownload(ctx);
                sysDownload.StartDownloadFile(info.urls[i], episodeVideoQualityPath + "/"+i+".blv", "[" + checkedEp.getString("index") + "] " +
                        checkedEp.getString("index_title")+" - "+i);
            }
        }catch (JSONException e){
            Toast.makeText(ctx, ctx.getString(R.string.message_json_exception, e.getLocalizedMessage()), Toast.LENGTH_LONG).show();
        }
    }
}

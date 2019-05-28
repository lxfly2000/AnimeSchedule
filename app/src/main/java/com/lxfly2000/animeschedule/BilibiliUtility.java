package com.lxfly2000.animeschedule;

import android.content.Context;
import android.content.SharedPreferences;

public class BilibiliUtility {
    public static String GetBilibiliDownloadPath(Context context){
        SharedPreferences preferences=Values.GetPreference(context);
        return preferences.getString(context.getString(R.string.key_bilibili_save_path),Values.GetAppDataPathExternal(context))
                .concat("/").concat(context.getResources().getStringArray(R.array.pkg_name_bilibili_versions)[preferences.getInt(context.getString(R.string.key_bilibili_version_index),Values.vDefaultBilibiliVersionIndex)])
                .concat("/download");
    }

    public static String GetBilibiliDownloadEpisodePath(Context context,String ssid,String epid){
        return GetBilibiliDownloadPath(context).concat("/s_").concat(ssid).concat("/").concat(epid);
    }

    public static String GetBilibiliDownloadEntryPath(Context context,String ssid,String epid){
        return GetBilibiliDownloadEpisodePath(context,ssid,epid).concat("/entry.json");
    }

    public static String GetBilibiliDownloadEpisodeIndexPath(Context context,String ssid,String epid,int quality){
        return GetBilibiliDownloadEpisodePath(context,ssid,epid)+"/"+GetVideoQuality(quality).tag+"/index.json";
    }

    public static class VideoQuality{
        public int value;
        public String tag;
        public String desc;
        public VideoQuality(int _value,String _tag,String _desc){
            value=_value;
            tag=_tag;
            desc=_desc;
        }
    }

    public static VideoQuality[] videoQualities={
            //旧版清晰度
            new VideoQuality(100,"lua.mp4.bb2api.16","流畅"),
            new VideoQuality(150,"lua.flv480.bb2api.32","清晰"),
            new VideoQuality(200,"lua.flv720.bb2api.64","高清"),
            new VideoQuality(400,"lua.flv.bb2api.80","超清"),
            new VideoQuality(800,"lua.hdflv2.bb2api.bd","1080P"),
            //新版清晰度
            new VideoQuality(16,"lua.mp4.bb2api.16","流畅 360P"),
            new VideoQuality(32,"lua.flv480.bb2api.32","清晰 480P"),
            new VideoQuality(64,"lua.flv720.bb2api.64","高清 720P"),
            new VideoQuality(80,"lua.flv.bb2api.80","高清 1080P"),
            new VideoQuality(112,"lua.hdflv2.bb2api.112","高清 1080P+"),
            new VideoQuality(74,"lua.flv720_p60.bili2api.74","高清 720P60"),
            new VideoQuality(116,"lua.flv_p60.bili2api.116","高清 1080P60"),
    };

    public static VideoQuality GetVideoQuality(int quality){
        return GetVideoQuality(quality,false);
    }
    public static VideoQuality GetVideoQuality(int quality,boolean convertToNewQuality){
        if(convertToNewQuality){
            switch (quality){
                case 100:quality=16;break;
                case 150:quality=32;break;
                case 200:quality=64;break;
                case 400:quality=80;break;
                case 800:quality=112;break;
            }
        }
        for (VideoQuality e : videoQualities) {
            if (e.value == quality) return e;
        }
        return new VideoQuality(quality,"(Tag)","(未知清晰度)");
    }

    public static final String jsonRawBilibiliEntry="{" +
            "    \"is_completed\":false,\n" +
            "    \"total_bytes\":0,\n" +
            "    \"downloaded_bytes\":0,\n" +
            "    \"title\":\"(番剧的正式标题)\",\n" +
            "    \"cover\":\"(番剧的封面)\",\n" +
            "    \"prefered_video_quality\":0,\n" +//清晰度代码
            "    \"guessed_total_bytes\":0,\n" +
            "    \"total_time_milli\":0,\n" +
            "    \"danmaku_count\":0,\n" +
            "    \"time_update_stamp\":0,\n" +//现在的时间戳，毫秒
            "    \"time_create_stamp\":0,\n" +//现在的时间戳，毫秒
            "    \"season_id\":\"(番剧SSID)\",\n" +
            "    \"ep\":{\n" +
            "        \"av_id\":0,\n" +//视频AV号
            "        \"page\":1,\n" +
            "        \"danmaku\":0,\n" +//弹幕CID
            "        \"cover\":\"(本集的封面)\",\n" +
            "        \"episode_id\":0,\n" +//本集的EPID
            "        \"index\":\"1\",\n" +
            "        \"index_title\":\"(本集的标题)\",\n" +
            "        \"from\":\"bangumi\",\n" +
            "        \"season_type\":1\n" +
            "    }\n" +
            "}";

    public static final String jsonRawBilibiliEpisodeIndex="{\n" +
            "    \"from\": \"（分区）\",\n" +
            "    \"type_tag\": \"（清晰度标签）\",\n" +
            "    \"description\": \"（清晰度名称）\",\n" +
            "    \"is_stub\": false,\n" +
            "    \"psedo_bitrate\": 0,\n" +
            "    \"segment_list\": [\n" +
            "        {\n" +
            "            \"url\": \"（视频URL）\",\n" +
            "            \"duration\": 0,\n" +
            "            \"bytes\": 0,\n" +//需要计算
            "            \"meta_url\": \"\",\n" +
            "            \"backup_urls\": [\n" +
            "                \"（备用视频URL，如果没有可省略）\"\n" +
            "            ]\n" +
            "        }\n" +
            "    ],\n" +
            "    \"parse_timestamp_milli\": 0,\n" +//需要计算
            "    \"available_period_milli\": 0,\n" +
            "    \"local_proxy_type\": 0,\n" +
            "    \"user_agent\": \"Bilibili Freedoooooom/MarkII\",\n" +
            "    \"is_downloaded\": false,\n" +
            "    \"is_resolved\": true,\n" +
            "    \"player_codec_config_list\": [\n" +
            "        {\n" +
            "            \"use_list_player\": false,\n" +
            "            \"use_ijk_media_codec\": false,\n" +
            "            \"player\": \"IJK_PLAYER\"\n" +
            "        },\n" +
            "        {\n" +
            "            \"use_list_player\": false,\n" +
            "            \"use_ijk_media_codec\": false,\n" +
            "            \"player\": \"ANDROID_PLAYER\"\n" +
            "        }\n" +
            "    ],\n" +
            "    \"time_length\": 0\n" +
            "}";
}

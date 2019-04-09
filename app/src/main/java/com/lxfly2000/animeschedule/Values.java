package com.lxfly2000.animeschedule;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import com.lxfly2000.utilities.FileUtility;

public class Values {
    public static final String appIdentifier="AnimeSchedule";
    public static final String[] pathJsonDataOnRepository={"anime.js","anime.json"};
    public static final String keyAnimeInfoDate="anime_info_date",vdAnimeInfoDate="1900-1-1";
    public static final String vDefaultString="(NOT SET)";
    public static final String keySortMethod="anime_sort_method";
    public static final int vDefaultSortMethod=1;
    public static final String keySortOrder="anime_sort_order";
    public static final int vDefaultSortOrder=2;
    public static final String keySortSeperateAbandoned="anime_sort_separate_abandoned";
    public static final boolean vDefaultSortSeperateAbandoned=true;
    public static final String keyBilibiliSavePath="bilibili_save_path";
    public static String GetvDefaultBilibiliSavePath(Context ctx){
        return GetAppDataPathExternal(ctx);
    }
    public static final String keyBilibiliVersionIndex="bilibili_version_index";
    public static final int vDefaultBilibiliVersionIndex =0;
    public static final String dateStringDefault="1900-1-1";
    public static final String keyStarMark="star_mark";
    public static final int vDefaultStarMark=0;
    public static final String keyEditWatchedEpisodeDialogType="edit_watched_epi_dlg_type";
    public static final int vDefaultEditWatchedEpisodeDialogType=0;
    public static final String keySkippedVersionCode="skip_ver_code";
    public static final int vDefaultSkippedVersionCode=0;
    public static final String[] starMarks={"★☆","●○"};
    public static String GetRepositoryPathOnLocal(){
        return Environment.getExternalStorageDirectory().getPath()+"/"+appIdentifier;
    }
    public static String GetCoverPathOnLocal(){
        return GetRepositoryPathOnLocal()+"/covers";
    }
    public static String GetJsonDataFullPath(){
        for(int i=0;i<pathJsonDataOnRepository.length;i++){
            if(FileUtility.IsFileExists(GetRepositoryPathOnLocal()+"/"+pathJsonDataOnRepository[i]))
                return GetRepositoryPathOnLocal()+"/"+pathJsonDataOnRepository[i];
        }
        return GetRepositoryPathOnLocal()+"/"+pathJsonDataOnRepository[0];
    }
    public static SharedPreferences GetPreference(Context context){
        return context.getSharedPreferences(appIdentifier,Context.MODE_PRIVATE);
    }
    public static void BuildDefaultSettings(Context context){
        SharedPreferences.Editor editPref=GetPreference(context).edit();
        editPref.putString(keyAnimeInfoDate,vdAnimeInfoDate);
        editPref.apply();
    }
    public static final String[]parsableLinksRegex={
            "(.*bangumi.bilibili.com/anime/[0-9]+)|(.*bilibili.com/bangumi/play/ss[0-9]+)|(.*bilibili.com/bangumi/play/ep[0-9]+)|(.*bilibili.com/bangumi/media/md[0-9]+)",
            ".*iqiyi.com/[a|v]_.*.html"
    };
    public static final String[]webFiles={
            "index.html",//第一个应该是首页文件
            "style.css",
            "main.js",
            "get_covers.py"
    };
    public static final int[]resIdWebFiles={
            R.raw.index,
            R.raw.style,
            R.raw.main,
            R.raw.get_covers
    };
    public static final String jsCallback="setJsonData";
    public static final String urlAuthor="https://github.com/lxfly2000/AnimeSchedule";
    public static final String urlAuthorGithubHome="https://github.com/lxfly2000";
    public static final String urlReportIssue="https://github.com/lxfly2000/AnimeSchedule/issues";
    public static final String urlAuthorEmailBase64="bWFpbHRvOmdhb2JveXVhbjhAcXEuY29t";
    public static final String urlContactQQBase64="aHR0cDovL3FtLnFxLmNvbS9jZ2ktYmluL3FtL3FyP2s9N2c0UjJySi1NYW1LNFVLQkNoVS1PcWpkZHZzNGExN0g=";
    public static final String urlContactTwitter="https://twitter.com/lxf2000";
    public static final String urlDonateQQBase64="aHR0cDovL3ZhYy5xcS5jb20vd2FsbGV0L3FyY29kZS5odG0/bT10ZW5wYXkmYT0xJnU9ODM2MDEzMDM5JmFjPUNCQzI3N0ZCNDQ1QUM3NjY0N0IyREE1ODI0OTBDOUE0MUI2OTA2RERGRDdDMUI5OTM4RDc1ODk5NTRFNjQxMTkmZj13YWxsZXQ=";
    public static final String urlDonateAlipayBase64="aHR0cHM6Ly9xci5hbGlwYXkuY29tL2ZreDA5ODkwNHZ1ODZtOTF0NnJ6dGEx";
    public static final String urlDonateWechatBase64="d3hwOi8vZjJmMHI2RlNlR3ZzNEIyTUhzTUZLdWdudEZMZDhhOG5KWnB3";
    public static final String urlDonatePaypalBase64="aHR0cHM6Ly93d3cucGF5cGFsLm1lL2x4Zmx5MjAwMA==";
    public static final String urlAnimeWatchUrlDefault="http://bangumi.bilibili.com/anime/";
    public static String GetCheckUpdateURL(){
        return urlAuthor+"/raw/master/app/build.gradle";
    }
    public static final String pkgNameBilibiliVersions[]={"tv.danmaku.bili","com.bilibili.app.in","com.bilibili.app.blue"};
    public static String GetAppDataPathExternal(Context ctx){
        return ctx.getExternalCacheDir().getParentFile().getParent();
    }
    public static final int typeBilibiliPreferredVideoQualities[]={100,150,200,400,800,112,74,116};

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
}

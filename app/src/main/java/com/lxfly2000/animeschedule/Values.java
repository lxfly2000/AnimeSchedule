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
            "(.*bangumi.bilibili.com/anime/[0-9]*)|(.*bilibili.com/bangumi/play/ss[0-9]*)",
            ".*bilibili.com/bangumi/play/ep[0-9]*",
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
    public static String GetCheckUpdateURL(){
        return urlAuthor+"/raw/master/app/build.gradle";
    }
}

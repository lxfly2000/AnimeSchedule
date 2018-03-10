package com.lxfly2000.animeschedule;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;

public class Values {
    public static final String appIdentifier="AnimeSchedule";
    public static final String keyRepositoryUrl="repo_url";
    public static final String keyBranch="branch";
    public static final String keyMail="mail";
    public static final String keyDisplayName="display_name";
    public static final String keyUserName="user_name";
    public static final String keyPassword="password";
    public static final String pathJsonDataOnRepository="anime.json";
    public static final String keyAnimeInfoDate="anime_info_date";
    public static final String vDefaultString="(NOT SET)";
    public static String GetRepositoryPathOnLocal(){
        return Environment.getExternalStorageDirectory().getPath()+"/"+appIdentifier;
    }
    public static String GetCoverPathOnLocal(){
        return GetRepositoryPathOnLocal()+"/covers";
    }
    public static String GetJsonDataFullPath(){
        return GetRepositoryPathOnLocal()+"/"+pathJsonDataOnRepository;
    }
    public static SharedPreferences GetPreference(Context context){
        return context.getSharedPreferences(appIdentifier,Context.MODE_PRIVATE);
    }
    public static void BuildDefaultSettings(Context context){
        SharedPreferences.Editor editPref=GetPreference(context).edit();
        editPref.putString(keyRepositoryUrl,"https://github.com/lxfly2000/anime_schedule");
        editPref.putString(keyBranch,"master");
        editPref.putString(keyMail,"");
        editPref.putString(keyDisplayName,"lxfly2000");
        editPref.putString(keyUserName,"lxfly2000");
        editPref.putString(keyPassword,"");
        editPref.putString(keyAnimeInfoDate,"1900-1-1");
        editPref.apply();
    }
    public static final String[]parsableLinksRegex={
            "(.*bangumi.bilibili.com/anime/[0-9]*)|(.*m.bilibili.com/bangumi/play/ss[0-9]*)",
            ".*iqiyi.com/[a|v]_.*.html"
    };
}

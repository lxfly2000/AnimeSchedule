package com.lxfly2000.animeschedule;

import android.os.Environment;

public class Values {
    public static final String appIdentifier="AnimeSchedule";
    public static final String keyRepositoryUrl="repo_url",vdRepositoryUrl="https://github.com/lxfly2000/anime_schedule";
    public static final String keyBranch="branch",vdBranch="master";
    public static final String keyMail="mail",vdMail="";
    public static final String keyDisplayName="display_name",vdDisplayName="lxfly2000";
    public static final String keyUserName="user_name",vdUserName="lxfly2000";
    public static final String keyPassword="password",vdPassword="";
    public static final String pathJsonDataOnRepository="anime.json";
    public static String GetRepositoryPathOnLocal(){
        return Environment.getExternalStorageDirectory().getPath()+"/"+appIdentifier;
    }
    public static String GetJsonDataFullPath(){
        return GetRepositoryPathOnLocal()+"/"+pathJsonDataOnRepository;
    }
}

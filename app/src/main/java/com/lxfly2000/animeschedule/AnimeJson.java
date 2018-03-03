package com.lxfly2000.animeschedule;

import com.lxfly2000.utilities.FileUtility;
import com.lxfly2000.utilities.JSONFormatter;
import org.json.JSONException;
import org.json.JSONObject;

public class AnimeJson {
    private JSONObject json;

    public AnimeJson(String path){
        LoadFromFile(path);
    }

    public AnimeJson(AnimeJson cloneFrom){
        this.json=cloneFrom.json;
    }

    public boolean LoadFromFile(String path){
        try {
            json = new JSONObject(FileUtility.ReadFile(path));
        }catch (JSONException e){
            return false;
        }
        return true;
    }

    public boolean SaveToFile(String path){
        return FileUtility.WriteFile(path, JSONFormatter.Format(json.toString()));
    }

    public int GetLastWatchIndex(){
        try {
            return json.getInt("last_watch_index");
        }catch (JSONException e){
            return -1;
        }
    }

    public int GetLastWatchEpisode(){
        try{
            return json.getInt("last_watch_episode");
        }catch (JSONException e){
            return -1;
        }
    }

    public String GetLastWatchDateString(){
        try {
            return json.getString("last_watch_date");
        }catch (JSONException e){
            return null;
        }
    }

    //TODO:在设置某剧集为已观看的同时需要调用此函数
    public boolean SetLastWatch(int index,int episode,String strdate){
        try{
            json.put("last_watch_index",index);
            json.put("last_watch_episode",episode);
            json.put("last_watch_date",strdate);
        }catch (JSONException e){
            return false;
        }
        return true;
    }

    public int GetAnimeCount(){
        try {
            return json.getJSONArray("anime").length();
        }catch (JSONException e){
            return 0;
        }
    }

    //TODO:anime属性下项目的各种操作……
}

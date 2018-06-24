package com.lxfly2000.animeschedule;

import com.lxfly2000.utilities.FileUtility;
import com.lxfly2000.utilities.JSONFormatter;
import com.lxfly2000.utilities.YMDDate;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class AnimeJson {
    private JSONObject json;

    class ExtraInformation{
        public ArrayList<Integer>lastUpdateEpisode;//从1数起
        public ArrayList<YMDDate>lastUpdateDate;//yyyy-M-d

        public ExtraInformation(){
            lastUpdateEpisode=new ArrayList<>();
            lastUpdateDate=new ArrayList<>();
        }
    }

    private ExtraInformation jsonExtra;

    public AnimeJson(String path){
        LoadFromFile(path);
    }

    public AnimeJson(){
        BuildDefaultData();
    }

    public void BuildDefaultData(){
        try {
            json=new JSONObject();
            json.put("_comment","创建于"+YMDDate.GetTodayDate().ToYMDString());
            json.put("last_watch_index",0);
            json.put("anime",new JSONArray());
        }catch (JSONException e){
            json=null;
        }
    }

    public void LoadFromFile(String path){
        String jsonString=FileUtility.ReadFile(path);
        try {
            json = new JSONObject(jsonString.substring(jsonString.indexOf('(')+1,jsonString.lastIndexOf(')')));
            CalculateExtraInfomation();
        }catch (JSONException | NullPointerException e){
            try {
                json=new JSONObject(jsonString);
                CalculateExtraInfomation();
            }catch (JSONException e1){
                json=null;
            }
        }
    }

    private void CalculateExtraInfomation(){
        if(jsonExtra!=null){
            jsonExtra.lastUpdateDate=null;
            jsonExtra.lastUpdateEpisode=null;
            jsonExtra=null;
        }
        jsonExtra=new ExtraInformation();
        for(int i=0;i<GetAnimeCount();i++){
            int lastUpdateEpisodeFromZero=0-GetAbsenseCount(i);
            YMDDate lastUpdateDate=new YMDDate(GetStartDate(i)),lastUpdateDateTemp=new YMDDate(GetStartDate(i));
            //https://github.com/lxfly2000/anime_schedule/blob/master/main.js#L134
            while(true){
                switch (GetUpdatePeriodUnit(i).toLowerCase()){
                    case unitYear:lastUpdateDateTemp.AddYear(GetUpdatePeriod(i));break;
                    case unitMonth:lastUpdateDateTemp.AddMonth(GetUpdatePeriod(i));break;
                    case unitDay:lastUpdateDateTemp.AddDate(GetUpdatePeriod(i));break;
                }
                if(lastUpdateDateTemp.IsLaterThanDate(YMDDate.GetTodayDate()))
                    break;
                if(GetEpisodeCount(i)!=-1&&lastUpdateEpisodeFromZero+1>=GetEpisodeCount(i))
                    break;
                lastUpdateDate.SetYMDDate(lastUpdateDateTemp);
                lastUpdateEpisodeFromZero++;
            }
            jsonExtra.lastUpdateDate.add(lastUpdateDate);
            jsonExtra.lastUpdateEpisode.add(lastUpdateEpisodeFromZero+1);
        }
    }

    public boolean SaveToFile(String path){
        return FileUtility.WriteFile(path, Values.jsCallback+"("+JSONFormatter.Format(json.toString())+");");
    }

    public String GetLastWatchDateStringForAnime(int index){
        try{
            return json.getJSONArray("anime").getJSONObject(index).getString("last_watch_date_anime");
        }catch (JSONException e){
            return "1900-1-1";
        }
    }

    public boolean SetLastWatchDateForAnime(int index,String date){
        try{
            json.getJSONArray("anime").getJSONObject(index).put("last_watch_date_anime",date);
            return true;
        }catch (JSONException e){
            return false;
        }
    }

    //洋：集数是从1数起的
    public int GetLastWatchEpisodeForAnime(int index){
        try{
            return json.getJSONArray("anime").getJSONObject(index).getInt("last_watch_episode_anime");
        }catch (JSONException e){
            return 0;
        }
    }

    public boolean SetLastWatchEpisodeForAnime(int index,int epi_from_one){
        try{
            json.getJSONArray("anime").getJSONObject(index).put("last_watch_episode_anime",epi_from_one);
            return true;
        }catch (JSONException e){
            return false;
        }
    }

    public int GetLastWatchIndex(){
        try {
            return json.getInt("last_watch_index");
        }catch (JSONException e){
            return 0;
        }
    }

    //【注意】集数是从1数起的
    public int GetLastWatchEpisode(){
        return GetLastWatchEpisodeForAnime(GetLastWatchIndex());
    }

    public String GetLastWatchDateString(){
        return GetLastWatchDateStringForAnime(GetLastWatchIndex());
    }

    //【注意】集数是从1数起的
    public boolean SetLastWatch(int index,int episode,String date){
        try{
            json.put("last_watch_index",index);
            SetLastWatchEpisodeForAnime(index,episode);
            SetLastWatchDateForAnime(index,date);
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

    public boolean SetCoverUrl(int index,String coverUrl){
        try{
            json.getJSONArray("anime").getJSONObject(index).put("cover",coverUrl);
        }catch (JSONException e){
            return false;
        }
        return true;
    }

    public String GetCoverUrl(int index){
        try {
            return json.getJSONArray("anime").getJSONObject(index).getString("cover");
        }catch (JSONException e){
            return Values.vDefaultString;
        }
    }

    public boolean SetTitle(int index,String title){
        try{
            json.getJSONArray("anime").getJSONObject(index).put("title",title);
        }catch (JSONException e){
            return false;
        }
        return true;
    }

    public String GetTitle(int index){
        try {
            return json.getJSONArray("anime").getJSONObject(index).getString("title");
        }catch (JSONException e){
            return Values.vDefaultString;
        }
    }

    public boolean SetDescription(int index,String description){
        try {
            json.getJSONArray("anime").getJSONObject(index).put("description",description);
        }catch (JSONException e){
            return false;
        }
        return true;
    }

    public String GetDescription(int index){
        try {
            return json.getJSONArray("anime").getJSONObject(index).getString("description");
        }catch (JSONException e){
            return Values.vDefaultString;
        }
    }

    public boolean SetStartDate(int index,String date){
        try {
            json.getJSONArray("anime").getJSONObject(index).put("start_date",date);
        }catch (JSONException e){
            return false;
        }
        return true;
    }

    public String GetStartDate(int index){
        try {
            return json.getJSONArray("anime").getJSONObject(index).getString("start_date");
        }catch (JSONException e){
            return "1900-1-1";
        }
    }

    public boolean SetUpdatePeriod(int index,int period){
        try {
            json.getJSONArray("anime").getJSONObject(index).put("update_period",period);
        }catch (JSONException e){
            return false;
        }
        return true;
    }

    public int GetUpdatePeriod(int index){
        try {
            return json.getJSONArray("anime").getJSONObject(index).getInt("update_period");
        }catch (JSONException e){
            return 0;
        }
    }

    public static final String unitDay="day",unitMonth="month",unitYear="year";

    public boolean SetUpdatePeriodUnit(int index,String unit){
        try {
            json.getJSONArray("anime").getJSONObject(index).put("update_period_unit",unit);
        }catch (JSONException e){
            return false;
        }
        return true;
    }

    public String GetUpdatePeriodUnit(int index){
        try {
            return json.getJSONArray("anime").getJSONObject(index).getString("update_period_unit");
        }catch (JSONException e){
            return unitDay;
        }
    }

    //-1表示总集数未知
    public boolean SetEpisodeCount(int index,int count){
        try {
            json.getJSONArray("anime").getJSONObject(index).put("episode_count",count);
        }catch (JSONException e){
            return false;
        }
        return true;
    }

    //-1表示总集数未知
    public int GetEpisodeCount(int index){
        try {
            return json.getJSONArray("anime").getJSONObject(index).getInt("episode_count");
        }catch (JSONException e){
            return 0;
        }
    }

    public boolean SetWatchUrl(int index,String url){
        try {
            json.getJSONArray("anime").getJSONObject(index).put("watch_url",url);
        }catch (JSONException e){
            return false;
        }
        return true;
    }

    public String GetWatchUrl(int index){
        try {
            return json.getJSONArray("anime").getJSONObject(index).getString("watch_url");
        }catch (JSONException e){
            return "";
        }
    }

    public boolean SetAbsenseCount(int index,int count){
        try {
            json.getJSONArray("anime").getJSONObject(index).put("absense_count",count);
        }catch (JSONException e){
            return false;
        }
        return true;
    }

    public int GetAbsenseCount(int index){
        try {
            return json.getJSONArray("anime").getJSONObject(index).getInt("absense_count");
        }catch (JSONException e){
            return 0;
        }
    }

    //【注意】集数是从1数起的
    public boolean SetEpisodeWatched(int index,int episode,boolean watched){
        episode--;
        try {
            JSONArray a=json.getJSONArray("anime").getJSONObject(index).getJSONArray("watched_episode");
            for(int i=a.length();i<episode-1;i++)
                a.put(i,false);
            a.put(episode,watched);
        }catch (JSONException e){
            return false;
        }
        if(watched)
            SetLastWatch(index,episode+1,YMDDate.GetTodayDate().ToYMDString());
        return true;
    }

    //【注意】集数是从1数起的
    public boolean GetEpisodeWatched(int index,int episode){
        episode--;
        try {
            return json.getJSONArray("anime").getJSONObject(index).getJSONArray("watched_episode").getBoolean(episode);
        }catch (JSONException e){
            return false;
        }
    }

    public boolean SetAbandoned(int index,boolean abandoned){
        try {
            json.getJSONArray("anime").getJSONObject(index).put("abandoned",abandoned);
        }catch (JSONException e){
            return false;
        }
        return true;
    }

    public boolean GetAbandoned(int index){
        try {
            return json.getJSONArray("anime").getJSONObject(index).getBoolean("abandoned");
        }catch (JSONException e){
            return false;
        }
    }

    //0～5
    public boolean SetRank(int index,int rank){
        try {
            json.getJSONArray("anime").getJSONObject(index).put("rank",rank);
        }catch (JSONException e){
            return false;
        }
        return true;
    }

    //0～5
    public int GetRank(int index){
        try {
            return json.getJSONArray("anime").getJSONObject(index).getInt("rank");
        }catch (JSONException e){
            return 0;
        }
    }

    public boolean SetColor(int index,String color){
        try {
            json.getJSONArray("anime").getJSONObject(index).put("color",color);
        }catch (JSONException e){
            return false;
        }
        return true;
    }

    public String GetColor(int index){
        try {
            return json.getJSONArray("anime").getJSONObject(index).getString("color");
        }catch (JSONException e){
            return "black";
        }
    }

    public boolean SetCategory(int index,String[]category){
        try {
            JSONObject o=json.getJSONArray("anime").getJSONObject(index);
            JSONArray a=new JSONArray();
            if(category!=null) {
                for (int i = 0; i < category.length; i++)
                    a.put(i, category[i]);
            }
            o.put("category",a);
        }catch (JSONException e){
            return false;
        }
        return true;
    }

    public String[]GetCategory(int index){
        try {
            JSONArray a=json.getJSONArray("anime").getJSONObject(index).getJSONArray("category");
            String[]cat=new String[a.length()];
            for(int i=0;i<cat.length;i++)
                cat[i]=a.getString(i);
            return cat;
        }catch (JSONException e){
            return null;
        }
    }

    public int GetLastUpdateEpisode(int index){
        return jsonExtra.lastUpdateEpisode.get(index);
    }

    public YMDDate GetLastUpdateYMDDate(int index){
        return jsonExtra.lastUpdateDate.get(index);
    }

    public int AddNewItem(){
        int ni=-1;
        try {
            JSONArray a=json.getJSONArray("anime");
            ni=a.length();
            JSONObject o=new JSONObject();
            o.put("watched_episode",new JSONArray());
            a.put(ni,o);
            SetCoverUrl(ni,"");
            SetWatchUrl(ni,"http://bangumi.bilibili.com/anime/");
            SetTitle(ni,"");
            SetDescription(ni,"Bili:");
            SetStartDate(ni,YMDDate.GetTodayDate().ToYMDString());
            SetUpdatePeriod(ni,7);
            SetUpdatePeriodUnit(ni,unitDay);
            SetEpisodeCount(ni,-1);
            SetAbsenseCount(ni,0);
            SetEpisodeWatched(ni,1,false);
            SetAbandoned(ni,false);
            SetRank(ni,0);
            SetColor(ni,"silver");
            SetCategory(ni,new String[]{});
            SetLastWatchDateForAnime(ni,"1900-1-1");
            SetLastWatchEpisodeForAnime(ni,0);
            CalculateExtraInfomation();
        }catch (JSONException e){
            return -1;
        }
        return ni;
    }

    public boolean RemoveItem(int index){
        try {
            int lastWatchIndex=GetLastWatchIndex();
            if(lastWatchIndex==index)
                SetLastWatch(-1,GetLastWatchEpisode(),GetLastWatchDateString());
            else if(lastWatchIndex>index)
                SetLastWatch(lastWatchIndex-1,GetLastWatchEpisode(),GetLastWatchDateString());
            json.getJSONArray("anime").remove(index);
            CalculateExtraInfomation();
        }catch (JSONException e){
            return false;
        }
        return true;
    }

    public boolean ClearAllAnime(){
        try {
            JSONArray a=json.getJSONArray("anime");
            while(a.length()>0) {
                if (!RemoveItem(0))
                    return false;
            }
        }catch (JSONException e){
            return false;
        }
        return true;
    }
}

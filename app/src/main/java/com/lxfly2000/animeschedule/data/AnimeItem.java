package com.lxfly2000.animeschedule.data;

import com.lxfly2000.animeschedule.AnimeJson;

import java.util.ArrayList;

public class AnimeItem {
    public static class EpisodeTitle{
        public String episodeTitle,episodeIndex;
    }

    public String watchUrl;
    public String coverUrl;
    public String title;
    public String description;
    public String actors;
    public String staff;
    public String startDate;
    public String updateTime;
    public int updatePeriod=7;
    public String updatePeriodUnit= AnimeJson.unitDay;
    public int episodeCount=0;
    public int absenseCount=0;
    public String[]categories;
    public int rank=0;
    public ArrayList<EpisodeTitle>episodeTitles=new ArrayList<>();
}

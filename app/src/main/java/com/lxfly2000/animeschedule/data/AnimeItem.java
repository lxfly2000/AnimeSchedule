package com.lxfly2000.animeschedule.data;

import com.lxfly2000.animeschedule.AnimeJson;
import com.lxfly2000.utilities.MinuteStamp;
import com.lxfly2000.utilities.YMDDate;

public class AnimeItem {
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
}

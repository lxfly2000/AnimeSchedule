package com.lxfly2000.animeschedule.data;

import com.lxfly2000.utilities.MinuteStamp;
import com.lxfly2000.utilities.YMDDate;

public class AnimeItem {
    public String watchUrl;
    public String coverUrl;
    public String title;
    public String description;
    public String actors;
    public String staff;
    public YMDDate startDate;
    public MinuteStamp updateTime;
    public int updatePeriod;
    public String updatePeriodUnit;
    public int episodeCount;
    public int absenseCount;
    public String[]categories;
    public int rank;
}

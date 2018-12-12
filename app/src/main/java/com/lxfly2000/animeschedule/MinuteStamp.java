package com.lxfly2000.animeschedule;

import java.util.Locale;

public class MinuteStamp {
    private int minuteStamp;

    public MinuteStamp(){
        this(0);
    }

    public MinuteStamp(int stamp){
        SetStamp(stamp);
    }

    public MinuteStamp(String stamp){
        FromString(stamp);
    }

    //可以接受含有秒的时间
    public void FromString(String stamp){
        String[]parts=stamp.split(":");
        if(parts.length<2)
            return;
        SetStamp(Integer.parseInt(parts[0])*60+Integer.parseInt(parts[1]));
    }

    public String ToString(){
        return String.format(Locale.getDefault(),"%d:%02d",GetStamp()/60,GetStamp()%60);
    }

    public int GetStamp(){
        return minuteStamp;
    }

    public void SetStamp(int stamp){
        minuteStamp=stamp;
    }
}

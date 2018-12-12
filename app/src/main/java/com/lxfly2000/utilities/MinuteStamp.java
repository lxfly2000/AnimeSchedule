package com.lxfly2000.utilities;

import java.util.Calendar;
import java.util.Locale;

public class MinuteStamp {
    private int minuteStamp;

    public MinuteStamp(){
        minuteStamp=0;
    }

    public MinuteStamp(int stamp){
        this();
        SetStamp(stamp);
    }

    public MinuteStamp(String strTime){
        this();
        FromString(strTime);
    }

    public static MinuteStamp GetNowTime(){
        //https://docs.oracle.com/javase/8/docs/api/java/util/Calendar.html
        Calendar nowDate=Calendar.getInstance();
        return new MinuteStamp(nowDate.get(Calendar.HOUR_OF_DAY)*60+nowDate.get(Calendar.MINUTE));
    }

    //可以接受含有秒的时间
    public void FromString(String strTime){
        String[]parts=strTime.split(":");
        if(parts.length<2)
            return;
        for(int i=0;i<2;i++)
            if(parts[i].length()==0)
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

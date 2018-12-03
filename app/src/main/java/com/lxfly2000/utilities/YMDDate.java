package com.lxfly2000.utilities;

import java.util.Date;
import java.util.Locale;
import java.util.regex.Pattern;

//仅处理“yyyy-M-d”格式日期
public class YMDDate {
    Date date;

    public YMDDate(){
        date=new Date();
    }

    public String ToYMDString(){
        return String.format(Locale.getDefault(),"%d-%d-%d",GetYear(),GetMonth(),GetDate());
    }

    public static YMDDate GetTodayDate(){
        //http://blog.csdn.net/henulwj/article/details/8888076
        return new YMDDate();
    }

    public void FromString(String strDate){
        Pattern p=Pattern.compile("^\\d+-\\d+-\\d+$");
        if(p.matcher(strDate).find()) {
            String[] parts = strDate.split("-");
            SetYear(Integer.parseInt(parts[0]));
            SetMonth(Integer.parseInt(parts[1]));
            SetDate(Integer.parseInt(parts[2]));
        }
    }

    public boolean IsLaterThanDate(YMDDate otherDate){
        int idate1=GetYear()*10000+GetMonth()*100+GetDate();
        int idate2=otherDate.GetYear()*10000+otherDate.GetMonth()*100+otherDate.GetDate();
        return idate1>idate2;
    }

    public boolean IsSameToDate(YMDDate otherDate){
        return GetYear()==otherDate.GetYear()&&GetMonth()==otherDate.GetMonth()&&GetDate()==otherDate.GetDate();
    }

    public boolean IsEarlierThanDate(YMDDate otherDate){
        return !IsSameToDate(otherDate)&&!IsLaterThanDate(otherDate);
    }

    public void AddDate(int dateInterval){
        int dz=GetDate()-1+dateInterval;
        int dc=0;
        switch (GetMonth()){
            case 1: case 3: case 5: case 7: case 8: case 10: case 12:
                dc=31;
                break;
            case 4: case 6: case 9: case 11:
                dc=30;
                break;
            case 2:
                dc=IsRunNian()?29:28;
                break;
        }
        if(dz<dc){
            SetDate(dz+1);
            return;
        }
        AddMonth(1);
        AddDate(dateInterval-dc);
    }

    public void AddMonth(int monthInterval){
        int mz=GetMonth()-1+monthInterval;
        SetMonth(mz%12+1);
        AddYear(mz/12);
    }

    public void AddYear(int yearInterval){
        SetYear(GetYear()+yearInterval);
    }

    //0=星期日，1=星期一，……6=星期六
    public int GetDayOfWeek(){
        return date.getDay();
    }

    //均从1数起
    public void SetYear(int y){
        date.setYear(y-1900);
    }
    public void SetMonth(int m){
        date.setMonth(m-1);
    }
    public void SetDate(int d){
        date.setDate(d);
    }

    public int GetYear(){
        return date.getYear()+1900;
    }

    //从1数起
    public int GetMonth(){
        return date.getMonth()+1;
    }

    //从1数起
    public int GetDate(){
        return date.getDate();
    }

    public void SetYMDDate(YMDDate otherDate){
        SetYear(otherDate.GetYear());
        SetMonth(otherDate.GetMonth());
        SetDate(otherDate.GetDate());
    }

    public YMDDate(YMDDate otherDate){
        this();
        SetYMDDate(otherDate);
    }

    public YMDDate(String strDate){
        this();
        FromString(strDate);
    }

    public boolean IsRunNian(){
        if(GetYear()%100==0)
            return GetYear()%400==0;
        else
            return GetYear()%4==0;
    }
}

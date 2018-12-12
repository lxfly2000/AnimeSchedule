package com.lxfly2000.animeschedule;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import com.lxfly2000.utilities.BadgeUtility;
import com.lxfly2000.utilities.MinuteStamp;
import com.lxfly2000.utilities.YMDDate;

import java.util.HashMap;

public class AnimeUpdateNotify extends Service {
    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override//收到启动服务的命令时调用
    public int onStartCommand(Intent intent,int flags,int startId){
        return super.onStartCommand(intent, flags, startId);
    }

    private AnimeJson jsonForNotify;

    //设置要使用的数据
    public void UpdateData(AnimeJson json){
        jsonForNotify=json;
    }

    //设置要使用的数据（从文件读取）
    public void UpdateData(String jsonPath){
        jsonForNotify=new AnimeJson(jsonPath);
    }
    
    private class AnimeDataForNotify{
        int index;
        MinuteStamp time;
        AnimeDataForNotify(int _index, MinuteStamp _time){
            index=_index;
            time=_time;
        }
    }

    private void NotifyUpdateInfo(){
        if(jsonForNotify==null)
            return;
        //https://www.cnblogs.com/lzq198754/p/5780165.html
        HashMap<String, AnimeDataForNotify>todaysAnimeUpdate=new HashMap<>();
        //找到在今天更新的番剧名称
        YMDDate todayYMDDate=YMDDate.GetTodayDate();
        SharedPreferences updatePreference=getSharedPreferences("AnimeUpdateNotify",MODE_PRIVATE);
        for(int i=0;i<jsonForNotify.GetAnimeCount();i++){
            if(jsonForNotify.GetLastUpdateYMDDate(i).IsSameToDate(todayYMDDate)&& !jsonForNotify.GetAbandoned(i)){
                todaysAnimeUpdate.put(jsonForNotify.GetTitle(i),new AnimeDataForNotify(i,new MinuteStamp(updatePreference.getInt(jsonForNotify.GetTitle(i),0))));
            }
        }
        //清除所有已存在的记录
        SharedPreferences.Editor updatePrefWrite=updatePreference.edit().clear();
        //检查是否到了更新时间，如果到了就提醒并更新已经记录的时间
        MinuteStamp nowMinute=MinuteStamp.GetNowTime();
        int updateCount=0;
        for (HashMap.Entry<String, AnimeDataForNotify> e :todaysAnimeUpdate.entrySet()) {
            if (nowMinute.GetStamp() >= e.getValue().time.GetStamp()) {
                e.getValue().time=nowMinute;
                StringBuilder strSchedule=new StringBuilder();
                strSchedule.append(jsonForNotify.GetLastUpdateYMDDate(e.getValue().index).ToLocalizedFormatString());
                if(strSchedule.toString().contains(" ")||Character.isDigit(strSchedule.charAt(strSchedule.length()-1)))
                    strSchedule.append(" ");
                strSchedule.append(e.getValue().time.ToString());
                strSchedule.append(getString(R.string.label_schedule_update_episode,jsonForNotify.GetLastUpdateEpisode(e.getValue().index)));
                PublishUpdateNotification(e.getKey(),strSchedule.toString(),jsonForNotify.GetWatchUrl(e.getValue().index));
                updatePrefWrite.putInt(jsonForNotify.GetTitle(e.getValue().index),e.getValue().time.GetStamp());
                updateCount++;
            }
        }
        updatePrefWrite.apply();
        BadgeUtility.setBadgeCount(this,updateCount,R.mipmap.ic_launcher);
    }
    
    private void PublishUpdateNotification(String title,String description,String actionUri){
        //TODO:通知
    }
}

package com.lxfly2000.animeschedule;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import com.lxfly2000.utilities.BadgeUtility;
import com.lxfly2000.utilities.FileUtility;
import com.lxfly2000.utilities.MinuteStamp;
import com.lxfly2000.utilities.YMDDate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

public class AnimeUpdateNotify extends Service {
    private IBinder serviceBinder=new GetServiceBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return serviceBinder;
    }

    private AnimeJson jsonForNotify=null;

    //设置要使用的数据
    public AnimeUpdateNotify UpdateData(AnimeJson json){
        jsonForNotify=json;
        return this;
    }

    //设置要使用的数据（从文件读取）
    public AnimeUpdateNotify UpdateData(String jsonPath){
        return UpdateData(new AnimeJson(jsonPath));
    }

    //时间1在2之前返回1，之后返回-1，相同返回0
    private int CompareDateTime(YMDDate d1,int t1,YMDDate d2,int t2){
        if(d1.IsEarlierThanDate(d2))
            return 1;
        else if(d1.IsLaterThanDate(d2))
            return -1;
        else return Integer.compare(t2, t1);
    }

    private void NotifyUpdateInfo(){
        if(jsonForNotify==null)
            return;
        SharedPreferences updatePreference=getSharedPreferences("AnimeUpdateNotify",MODE_PRIVATE);
        YMDDate todayDate=YMDDate.GetTodayDate(),lastDate=new YMDDate(updatePreference.getString("date",Values.vdAnimeInfoDate)),animeDate;
        int nowMinute=MinuteStamp.GetNowTime().GetStamp(),lastMinute=updatePreference.getInt("time",0),animeMinute;
        int updateCount=0;
        for (int i=0;i<jsonForNotify.GetAnimeCount();i++) {
            animeDate=jsonForNotify.GetLastUpdateYMDDate(i);
            animeMinute=jsonForNotify.GetUpdateTime(i);
            if (CompareDateTime(lastDate,lastMinute,animeDate,animeMinute)==1&&CompareDateTime(animeDate,animeMinute,todayDate,nowMinute)!=-1) {
                StringBuilder strSchedule=new StringBuilder();
                strSchedule.append(jsonForNotify.GetLastUpdateYMDDate(i).ToLocalizedFormatString());
                if(strSchedule.toString().contains(" ")||Character.isDigit(strSchedule.charAt(strSchedule.length()-1)))
                    strSchedule.append(" ");
                strSchedule.append(new MinuteStamp(jsonForNotify.GetUpdateTime(i)).ToString());
                strSchedule.append(getString(R.string.label_schedule_update_episode,jsonForNotify.GetLastUpdateEpisode(i)));
                //获取cover路径
                String coverUrl=jsonForNotify.GetCoverUrl(i);
                String[]tempSplit=coverUrl.split("/");
                String coverExt="";
                if(tempSplit.length>0&&tempSplit[tempSplit.length-1].contains(".")){
                    coverExt=tempSplit[tempSplit.length-1].substring(tempSplit[tempSplit.length-1].lastIndexOf('.'));
                }
                String coverPath=Values.GetCoverPathOnLocal()+"/"+
                        jsonForNotify.GetTitle(i).replaceAll("[/\":|<>?*]","_")+coverExt;
                PublishUpdateNotification(updateCount,jsonForNotify.GetTitle(i),strSchedule.toString(),coverPath,jsonForNotify.GetWatchUrl(i));
                updateCount++;
            }
        }
        SharedPreferences.Editor updatePrefWrite=updatePreference.edit();
        updatePrefWrite.putString("date",todayDate.ToYMDString());
        updatePrefWrite.putInt("time",nowMinute);
        updatePrefWrite.apply();
        BadgeUtility.setBadgeCount(this,updateCount,R.mipmap.ic_launcher);
    }
    
    private void PublishUpdateNotification(int id,String title,String description,String imgPath,String actionUri){
        NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        PendingIntent pi=PendingIntent.getActivity(this,0,new Intent(Intent.ACTION_VIEW).setData(Uri.parse(actionUri)),0);
        //Builder方法过时解决：https://blog.csdn.net/zwk_sys/article/details/79661045
        NotificationCompat.Builder nb=new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(description)
                //https://blog.csdn.net/yuzhiboyi/article/details/8484771
                .setContentIntent(pi)
                //.setFullScreenIntent(pi,false)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setAutoCancel(true);
        if(FileUtility.IsFileExists(imgPath))
            nb.setLargeIcon(BitmapFactory.decodeFile(imgPath));
        nm.notify(id,nb.build());
        /*new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                nb.setFullScreenIntent(null,false)
                        .setSound(null);
                nm.notify(id,nb.build());//Bug 非预期的动作：即使点了通知框还是会在状态栏中再显示一次
            }
        },5000);*/
    }

    Timer timer=null;

    public AnimeUpdateNotify RestartTimer(){
        if(timer!=null)
            timer.cancel();
        timer=new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                NotifyUpdateInfo();
            }
        },0,1800000);//每隔30分钟运行一次
        return this;
    }

    @Override
    public void onDestroy(){
        timer.cancel();
        super.onDestroy();
    }

    public class GetServiceBinder extends Binder{
        AnimeUpdateNotify GetService(){
            return AnimeUpdateNotify.this;
        }
    }
}

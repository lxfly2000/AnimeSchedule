package com.lxfly2000.animeschedule;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.*;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.SparseArray;
import com.lxfly2000.utilities.*;

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
            if(jsonForNotify.GetAbandoned(i))
                continue;
            animeDate=jsonForNotify.GetLastUpdateYMDDate(i);
            animeMinute=jsonForNotify.GetUpdateTime(i);
            if (CompareDateTime(lastDate,lastMinute,animeDate,animeMinute)==1&&CompareDateTime(animeDate,animeMinute,todayDate,nowMinute)!=-1) {
                PublishUpdateNotification(i);
                updateCount++;
            }
        }
        SharedPreferences.Editor updatePrefWrite=updatePreference.edit();
        updatePrefWrite.putString("date",todayDate.ToYMDString());
        updatePrefWrite.putInt("time",nowMinute);
        updatePrefWrite.apply();
        BadgeUtility.setBadgeCount(this,updateCount,R.drawable.ic_animeschedule);
    }

    private static final String ACTION_WATCH_ANIME=BuildConfig.APPLICATION_ID+".WatchAnime";
    private SparseArray<Timer> timersHideNotifyHead =new SparseArray<>();
    
    private void PublishUpdateNotification(final int index){
        StringBuilder strSchedule=new StringBuilder();
        strSchedule.append(jsonForNotify.GetLastUpdateYMDDate(index).ToLocalizedFormatString());
        if(strSchedule.toString().contains(" ")||Character.isDigit(strSchedule.charAt(strSchedule.length()-1)))
            strSchedule.append(" ");
        strSchedule.append(new MinuteStamp(jsonForNotify.GetUpdateTime(index)).ToString());
        strSchedule.append(getString(R.string.label_schedule_update_episode,jsonForNotify.GetLastUpdateEpisode(index)));
        //获取cover路径
        String coverUrl=jsonForNotify.GetCoverUrl(index);
        String[]tempSplit=coverUrl.split("/");
        String coverExt="";
        if(tempSplit.length>0&&tempSplit[tempSplit.length-1].contains(".")){
            coverExt=tempSplit[tempSplit.length-1].substring(tempSplit[tempSplit.length-1].lastIndexOf('.'));
        }
        String coverPath=Values.GetCoverPathOnLocal()+"/"+
                jsonForNotify.GetTitle(index).replaceAll("[/\":|<>?*]","_")+coverExt;
        final NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        //putExtra的参数在获取时为空的问题：https://blog.csdn.net/wangbole/article/details/7465385
        //注意requestCode在不同的通知里也要是不同的，否则会被覆盖
        PendingIntent pi=PendingIntent.getBroadcast(this,index,new Intent(ACTION_WATCH_ANIME).putExtra("index",index),PendingIntent.FLAG_UPDATE_CURRENT);
        //Builder方法过时解决：https://blog.csdn.net/zwk_sys/article/details/79661045
        final NotificationCompat.Builder nb=new NotificationCompat.Builder(this,"anime_update")
                .setSmallIcon(R.drawable.ic_animeschedule)
                .setContentTitle(jsonForNotify.GetTitle(index))
                .setContentText(strSchedule.toString())
                //https://blog.csdn.net/yuzhiboyi/article/details/8484771
                .setContentIntent(pi)
                .setFullScreenIntent(pi,false)
                .setDefaults(Notification.DEFAULT_LIGHTS|Notification.DEFAULT_SOUND)
                .setAutoCancel(true);
        if(FileUtility.IsFileExists(coverPath))
            nb.setLargeIcon(BitmapFactory.decodeFile(coverPath));
        nm.notify(index,nb.build());
        timersHideNotifyHead.put(index,new Timer());
        timersHideNotifyHead.get(index).schedule(new TimerTask() {
            @Override
            public void run() {
                nb.setFullScreenIntent(null,false)
                        .setDefaults(Notification.DEFAULT_LIGHTS);
                nm.notify(index,nb.build());//Bug 非预期的动作：即使点了通知框还是会在状态栏中再显示一次
            }
        },5000);
    }

    Timer timerCheckAnimeUpdate =null;

    public AnimeUpdateNotify RestartTimer(){
        if(timerCheckAnimeUpdate !=null)
            timerCheckAnimeUpdate.cancel();
        timerCheckAnimeUpdate =new Timer();
        timerCheckAnimeUpdate.schedule(new TimerTask() {
            @Override
            public void run() {
                NotifyUpdateInfo();
            }
        },0,300000);//每隔5分钟运行一次
        return this;
    }

    BroadcastReceiver notifyBroadcastReceiver;

    @Override
    public void onDestroy(){
        timerCheckAnimeUpdate.cancel();
        unregisterReceiver(notifyBroadcastReceiver);
        super.onDestroy();
    }

    @Override
    public void onCreate(){
        super.onCreate();
        //因为要用到json，所以为了简单起见用动态创建广播的方式响应通知动作
        notifyBroadcastReceiver=new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(ACTION_WATCH_ANIME.equals(intent.getAction())) {
                    int index=intent.getIntExtra("index",0);
                    timersHideNotifyHead.get(index).cancel();
                    AndroidUtility.OpenUri(context, jsonForNotify.GetWatchUrl(index));
                }
            }
        };
        registerReceiver(notifyBroadcastReceiver,new IntentFilter(ACTION_WATCH_ANIME));
    }

    public class GetServiceBinder extends Binder{
        AnimeUpdateNotify GetService(){
            return AnimeUpdateNotify.this;
        }
    }
}

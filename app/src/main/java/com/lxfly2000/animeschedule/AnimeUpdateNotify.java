package com.lxfly2000.animeschedule;

import android.app.*;
import android.content.*;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;
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
            if(updateCount>=20)
                break;
        }
        SharedPreferences.Editor updatePrefWrite=updatePreference.edit();
        updatePrefWrite.putString("date",todayDate.ToYMDString());
        updatePrefWrite.putInt("time",nowMinute);
        updatePrefWrite.apply();
        BadgeUtility.setBadgeCount(this,updateCount,R.drawable.ic_animeschedule);
    }

    private static final String ACTION_WATCH_ANIME=BuildConfig.APPLICATION_ID+".WatchAnime";
    private String notifyChannelId=BuildConfig.APPLICATION_ID;

    private void RegisterNotifyIdChannel(){
        //https://blog.csdn.net/qq_15527709/article/details/78853048
        String notifyChannelName = "AnimeSchedule Channel";
        NotificationChannel notificationChannel;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationChannel = new NotificationChannel(notifyChannelId,
                    notifyChannelName, NotificationManager.IMPORTANCE_HIGH);
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.setShowBadge(true);
            notificationChannel.enableVibration(false);
            notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if(manager!=null) {
                manager.createNotificationChannel(notificationChannel);
            }
        }
    }

    private void ReleaseNotifyIdChannel(){
        NotificationManager manager=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (manager != null) {
                manager.deleteNotificationChannel(notifyChannelId);
            }
        }
    }
    
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
        String coverExt=".jpg";
        if(tempSplit.length>0&&tempSplit[tempSplit.length-1].contains(".")){
            coverExt=tempSplit[tempSplit.length-1].substring(tempSplit[tempSplit.length-1].lastIndexOf('.'));
        }
        String coverPath=Values.GetCoverPathOnLocal(this)+"/"+
                FileUtility.ReplaceIllegalPathChar(jsonForNotify.GetTitle(index)+coverExt);
        final NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        //putExtra的参数在获取时为空的问题：https://blog.csdn.net/wangbole/article/details/7465385
        //注意requestCode在不同的通知里也要是不同的，否则会被覆盖
        PendingIntent pi=PendingIntent.getBroadcast(this,index,new Intent(ACTION_WATCH_ANIME).putExtra("index",index),PendingIntent.FLAG_UPDATE_CURRENT);
        //Builder方法过时解决：https://blog.csdn.net/zwk_sys/article/details/79661045
        final NotificationCompat.Builder nb=new NotificationCompat.Builder(this,notifyChannelId)
                .setSmallIcon(R.drawable.ic_animeschedule)
                .setContentTitle(jsonForNotify.GetTitle(index))
                .setContentText(strSchedule.toString())
                //https://blog.csdn.net/yuzhiboyi/article/details/8484771
                .setContentIntent(pi)
                //https://blog.csdn.net/wds1181977/article/details/49783787
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(Notification.DEFAULT_LIGHTS|Notification.DEFAULT_SOUND)
                .setAutoCancel(true);
        if(FileUtility.IsFileExists(coverPath))
            nb.setLargeIcon(BitmapFactory.decodeFile(coverPath));
        if(nm!=null)
            nm.notify(index,nb.build());
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
        ReleaseNotifyIdChannel();
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
                    AndroidUtility.OpenUri(context, jsonForNotify.GetWatchUrl(index));
                }
            }
        };
        RegisterNotifyIdChannel();
        registerReceiver(notifyBroadcastReceiver,new IntentFilter(ACTION_WATCH_ANIME));
    }

    public class GetServiceBinder extends Binder{
        AnimeUpdateNotify GetService(){
            return AnimeUpdateNotify.this;
        }
    }
}

package com.lxfly2000.animeschedule;

import android.Manifest;
import android.app.SearchManager;
import android.content.*;
import android.content.pm.PackageManager;
import android.database.MatrixCursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.BaseColumns;
import androidx.core.app.ActivityOptionsCompat;
import com.google.android.material.snackbar.Snackbar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.widget.*;
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes;
import com.lxfly2000.animeschedule.data.AnimeItem;
import com.lxfly2000.animeschedule.downloaddialog.*;
import com.lxfly2000.animeschedule.spider.*;
import com.lxfly2000.utilities.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {
    private AnimeJson animeJson;
    private ArrayList<Integer>jsonSortTable;
    private int sortOrder=0;
    ListView listAnime;
    private int posListAnimeScroll=0,posListAnimeTop=0;
    private SharedPreferences preferences;
    private int longPressedListItem=-1;
    private AnimeUpdateNotify notifyService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setSupportActionBar(findViewById(R.id.toolbar));
        findViewById(R.id.fabShowAnimeUpdate).setOnClickListener(view -> OnShowAnimeUpdate());

        AppInit(true);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        AppInit(false);
    }

    private static final int requestCodeReadStorage=0;
    private void AppInit(boolean needRequestPermissions){
        //检查权限设置
        if(checkCallingOrSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){
            if(needRequestPermissions&& Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},requestCodeReadStorage);
            }else {
                AlertDialog.Builder about = new AlertDialog.Builder(this);
                about.setTitle(R.string.app_name);
                about.setMessage(R.string.error_permission_reading_sdcard);
                about.setPositiveButton(android.R.string.ok, null);
                about.setOnDismissListener(dialogInterface -> MainActivity.this.finish());
                about.show();
            }
            return;
        }
        preferences=Values.GetPreference(this);
        if(preferences.getString(Values.keyAnimeInfoDate,Values.vDefaultString).contentEquals(Values.vDefaultString)){
            Toast.makeText(this,R.string.message_build_default_settings,Toast.LENGTH_LONG).show();
            Values.BuildDefaultSettings(this);
        }
        listAnime= findViewById(R.id.listAnime);
        registerForContextMenu(listAnime);
        listAnime.setOnItemClickListener(listAnimeCallback);
        listAnime.setOnItemLongClickListener((adapterView, view, i, l) -> {
            longPressedListItem=i;
            return false;
        });
        listAnime.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                //Nothing to do.
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if(firstVisibleItem!=lastTopItemIndex||firstVisibleItem+visibleItemCount!=lastBottomItemIndex+1){
                    if(listAnime.getAdapter()!=null) {
                        //参考：https://blog.csdn.net/jdsjlzx/article/details/17794209
                        posListAnimeScroll = listAnime.getFirstVisiblePosition();
                        View v = listAnime.getChildAt(0);
                        posListAnimeTop = (v == null) ? 0 : v.getTop();
                        DisplayImagesVisible(firstVisibleItem,firstVisibleItem+visibleItemCount-1);
                    }
                }
            }
        });
        if(FileUtility.IsFileExists(Values.GetRepositoryPathOnLocal())){
            String oldPath=Values.GetRepositoryPathOnLocal();
            String newPath=Values.GetRepositoryPathOnLocal(this);
            Toast.makeText(this,getString(R.string.message_move_files,oldPath,newPath),Toast.LENGTH_LONG).show();
            try {
                Runtime.getRuntime().exec(String.format("rm -r -f %s", newPath)).waitFor();
                Runtime.getRuntime().exec(String.format("mv -f %s %s", oldPath, newPath)).waitFor();
            }catch (IOException e){
                Toast.makeText(this,getString(R.string.message_io_exception,e.getLocalizedMessage()),Toast.LENGTH_LONG).show();
            }catch (InterruptedException e){
                Toast.makeText(this,getString(R.string.message_exception,e.getLocalizedMessage()),Toast.LENGTH_LONG).show();
            }
        }
        SaveAndReloadJsonFile(false);
        if(animeJson.GetAnimeCount()>0) {
            GetAnimeUpdateInfo(true);
        }else{
            Snackbar.make(listAnime,R.string.message_no_anime_json,Snackbar.LENGTH_LONG)
                    .setAction(android.R.string.ok, view -> {
                        try{
                            if(FileUtility.WriteFile(Values.GetJsonDataFullPath(getBaseContext()),StreamUtility.GetStringFromStream(getResources().openRawResource(R.raw.anime))))
                                SaveAndReloadJsonFile(false);
                            else
                                Toast.makeText(getBaseContext(), getString(R.string.message_cannot_write_to_file,Values.GetJsonDataFullPath(getBaseContext())), Toast.LENGTH_LONG).show();
                        }catch (IOException e){
                            //Nothing to do.
                        }
                    }).show();
        }

        //检测更新
        new UpdateChecker(this).CheckForUpdate(true);
        //处理Intent
        HandleIntent(getIntent());
        //初始化搜索建议相关变量
        final String[]suggestionKeys=new String[]{"title"};
        final int[]suggestionsIds=new int[]{android.R.id.text1};
        suggestionsAdapter=new SimpleCursorAdapter(this,android.R.layout.simple_list_item_1,null,suggestionKeys,
                suggestionsIds,CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);

        googleDriveOperator=new GoogleDriveOperator(this);
        BadgeUtility.resetBadgeCount(this,R.drawable.ic_animeschedule);
        //这样启动的服务表示服务与Activity是独立的，即使Activity被关闭服务也不会停止
        startService(new Intent(this,AnimeUpdateNotify.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }

    private ServiceConnection updateNotifyConnection=null;

    private void BindNotifyService(){
        updateNotifyConnection=new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                notifyService=((AnimeUpdateNotify.GetServiceBinder)iBinder).GetService();
                ServiceConnectedActions();
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                notifyService=null;
            }
        };
        //这个不能在启动时就调用，因为回调函数有延迟
        //注意要在退出时解除绑定
        //启动，获取服务，https://github.com/lxfly2000/lxplayer/blob/master/app/src/main/java/com/lxfly2000/lxplayer/MainActivity.java
        bindService(new Intent(this, AnimeUpdateNotify.class), updateNotifyConnection, 0);
    }

    @Override
    protected void onDestroy(){
        if(updateNotifyConnection!=null)
            unbindService(updateNotifyConnection);
        super.onDestroy();
    }

    private void ServiceConnectedActions(){
        notifyService.UpdateData(animeJson).RestartTimer();
    }

    @Override
    protected void onNewIntent(Intent intent){
        HandleIntent(intent);
        super.onNewIntent(intent);
    }

    private void HandleIntent(Intent intent){
        if(Intent.ACTION_SEARCH.equals(intent.getAction())){
            String queryWord=intent.getStringExtra(SearchManager.QUERY);
            //跳转至搜索的番剧名称处
            for(int i=0;i<animeJson.GetAnimeCount();i++){
                if(animeJson.GetTitle(jsonSortTable.get(i)).equals(queryWord)){
                    Toast.makeText(this,String.format(getString(R.string.message_anime_jumping),queryWord),Toast.LENGTH_SHORT).show();
                    listAnime.setSelection(i);
                    return;
                }
            }
            Toast.makeText(this,String.format(getString(R.string.message_anime_not_found),queryWord),Toast.LENGTH_LONG).show();
        }else if(Intent.ACTION_SEND.equals(intent.getAction())){
            //分享功能
            OnAddAnime();
            String fullText=intent.getStringExtra(Intent.EXTRA_TEXT);
            if(fullText==null)
                fullText="";
            editDialogDescription.setText(fullText);
            Matcher matcher=Pattern.compile("[a-zA-Z0-9\\-_]+:[^ \\n]+\\.[^ \\n]+").matcher(fullText);//接收的链接必须带有协议名称
            //                                ~~~~~~~~~注意：此处用\w在安卓中是错的！！安卓的正则表达式是强制开启UNICODE匹配的，参考链接：https://developer.android.com/reference/java/util/regex/Pattern#UNICODE_CHARACTER_CLASS
            if(matcher.find())
                editDialogWatchUrl.setText(fullText.substring(matcher.start(),matcher.end()));
            buttonDialogAutofill.performClick();
        }
    }

    private AdapterView.OnItemClickListener listAnimeCallback=new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            String watchUrl=animeJson.GetWatchUrl(jsonSortTable.get(i));
            String url=watchUrl;
            if(URLUtility.IsBilibiliVideoLink(url))
                url=URLUtility.MakeBilibiliVideoUriString(URLUtility.GetBilibiliVideoIdString(url));
            else if(URLUtility.IsBilibiliSeasonBangumiLink(url))
                url=URLUtility.MakeBilibiliSeasonUriString(URLUtility.GetBilibiliSeasonIdString(url));
            try {
                AndroidUtility.OpenUri(getBaseContext(), url);
            }catch (ActivityNotFoundException e){
                try {
                    AndroidUtility.OpenUri(getBaseContext(), watchUrl);
                }catch (ActivityNotFoundException ee){
                    Toast.makeText(getBaseContext(),getString(R.string.message_exception_no_activity_available)+
                            "\n"+e.getLocalizedMessage(),Toast.LENGTH_LONG).show();
                }
            }
        }
    };

    private void ReadJsonFile(){
        if(!FileUtility.IsFileExists(Values.GetJsonDataFullPath(this))){
            animeJson=new AnimeJson();
            SaveJsonFile();
        }
        animeJson=new AnimeJson(Values.GetJsonDataFullPath(this));
        Pattern p=Pattern.compile("\\.json$");
        Matcher m=p.matcher(Values.GetJsonDataFullPath(this));
        if(m.find()){
            File file=new File(Values.GetJsonDataFullPath(this));
            if(file.renameTo(new File(Values.GetRepositoryPathOnLocal(this)+"/"+Values.pathJsonDataOnRepository[0]))) {
                Toast.makeText(this, getString(R.string.message_rename_file,file.getName(),Values.pathJsonDataOnRepository[0]), Toast.LENGTH_LONG).show();
            }else {
                Toast.makeText(this, getString(R.string.message_cannot_rename,file.getName()), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void SaveJsonFile(){
        animeJson.SaveToFile(Values.GetJsonDataFullPath(this));
    }

    class ParametersSetImage{
        public SimpleAdapter listAdapter;
        public String imagePath;
        public int listIndex;
        ParametersSetImage(SimpleAdapter adapter,String path,int index){
            listAdapter=adapter;
            imagePath=path;
            listIndex=index;
        }
    }

    int lastTopItemIndex,lastBottomItemIndex;

    private void DisplayImagesVisible(int top,int bottom){
        final SimpleAdapter adapter=(SimpleAdapter)listAnime.getAdapter();
        for(int i=lastTopItemIndex;i<=lastBottomItemIndex;i++){
            if(i>=0&&i<top||i>bottom&&i<listAnime.getCount()){
                HashMap<String,Object>item=(HashMap)adapter.getItem(i);
                if(item.get("cover")instanceof Bitmap){
                    ((Bitmap)item.get("cover")).recycle();
                    item.remove("cover");
                }
            }
        }
        lastTopItemIndex=top;
        lastBottomItemIndex=bottom;
        for(int i=top;i<=bottom;i++){
            final HashMap<String,Object>item=(HashMap)adapter.getItem(i);
            if(item.get("cover")==null){
                String coverUrl=animeJson.GetCoverUrl(jsonSortTable.get(i));
                final String coverPath=GetLocalCoverPath(coverUrl,i);
                if(FileUtility.IsFileExists(coverPath)){
                    AsyncTask<Object,Integer,Boolean>task=new AsyncTask<Object, Integer, Boolean>() {
                        @Override
                        protected Boolean doInBackground(Object... objects) {
                            item.put("cover",BitmapFactory.decodeFile(coverPath));
                            return (int)objects[0]>=lastTopItemIndex&&(int)objects[0]<=lastBottomItemIndex;
                        }
                        @Override
                        protected void onPostExecute(Boolean result){
                            if(result) {
                                adapter.notifyDataSetChanged();
                            }else if(item.get("cover")instanceof Bitmap){
                                ((Bitmap)item.get("cover")).recycle();
                                item.remove("cover");
                            }
                        }
                    };
                    task.execute(i);
                }else{
                    AndroidDownloadFileTask task=new AndroidDownloadFileTask() {
                        @Override
                        public void OnReturnStream(ByteArrayInputStream stream, boolean success, int response, Object extra, URLConnection connection) {
                            ParametersSetImage param = (ParametersSetImage) extra;
                            try {
                                if (success) {
                                    FileUtility.WriteStreamToFile(param.imagePath, stream);
                                    item.put("cover", BitmapFactory.decodeFile(param.imagePath));
                                } else {
                                    item.put("cover", BitmapFactory.decodeResource(getResources(), R.raw.dn_error));
                                    Toast.makeText(getBaseContext(), getString(R.string.message_cannot_download_cover, animeJson.GetCoverUrl(jsonSortTable.get(param.listIndex)),
                                            ((HashMap<String, Object>) param.listAdapter.getItem(param.listIndex)).get("title")), Toast.LENGTH_LONG).show();
                                }
                                if(param.listIndex>=lastTopItemIndex&&param.listIndex<=lastBottomItemIndex) {
                                    adapter.notifyDataSetChanged();
                                }else if(item.get("cover")instanceof Bitmap){
                                    ((Bitmap)item.get("cover")).recycle();
                                    item.remove("cover");
                                }
                            }catch (IndexOutOfBoundsException e){/*Nothing*/}
                        }
                    };
                    task.SetExtra(new ParametersSetImage(adapter,coverPath,i));
                    task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,coverUrl,coverPath);
                    item.put("cover",task);
                }
            }
        }
    }

    private String GetLocalCoverPath(String coverUrl,int listPosition){
        String[]tempSplit=coverUrl.split("/");
        String coverExt=".jpg";
        if(tempSplit.length>0&&tempSplit[tempSplit.length-1].contains(".")){
            coverExt=tempSplit[tempSplit.length-1].substring(tempSplit[tempSplit.length-1].lastIndexOf('.'));
        }
        String coverPath=Values.GetCoverPathOnLocal(this)+"/"+
                FileUtility.ReplaceIllegalPathChar(animeJson.GetTitle(jsonSortTable.get(listPosition))+coverExt);
        return coverPath;
    }

    private void DisplayList(){
        RebuildSortTable(preferences.getInt(getString(R.string.key_sort_method),Values.vDefaultSortMethod),
                preferences.getInt(getString(R.string.key_sort_order),Values.vDefaultSortOrder),
                preferences.getBoolean(getString(R.string.key_sort_separate_abandoned),Values.vDefaultSortSeperateAbandoned));
        ArrayList<HashMap<String,Object>>listItems=new ArrayList<>();
        String[]keyStrings={"title","description","ranking","schedule","cover"};
        int[]viewIds={R.id.textAnimeTitle,R.id.textAnimeDescription,R.id.textRanking,R.id.textSchedule,R.id.imageCover};
        AnimeItemAdapter customAdapter=new AnimeItemAdapter(this,listItems,R.layout.item_anime,keyStrings,viewIds);
        String starMarks=getResources().getStringArray(R.array.star_marks)[preferences.getInt(getString(R.string.key_star_mark),Values.vDefaultStarMark)];
        String starMarkFull=starMarks.substring(0,1),starMarkEmpty=starMarks.substring(1,2);
        for(int i=0;i<animeJson.GetAnimeCount();i++){
            HashMap<String,Object>listItem=new HashMap<>();
            listItem.put("title",animeJson.GetTitle(jsonSortTable.get(i)));
            listItem.put("description",animeJson.GetDescription(jsonSortTable.get(i)));
            StringBuilder rankingString=new StringBuilder();
            for(int j=0;j<5;j++){
                rankingString.append(j<animeJson.GetRank(jsonSortTable.get(i))?starMarkFull:starMarkEmpty);
            }
            listItem.put("ranking",rankingString.toString());
            StringBuilder strSchedule=new StringBuilder();
            strSchedule.append(animeJson.GetLastUpdateYMDDate(jsonSortTable.get(i)).ToLocalizedFormatString());
            if(strSchedule.toString().contains(" ")||Character.isDigit(strSchedule.charAt(strSchedule.length()-1)))
                strSchedule.append(" ");
            strSchedule.append(new MinuteStamp(animeJson.GetUpdateTime(jsonSortTable.get(i))).ToString());
            strSchedule.append(getString(R.string.label_schedule_update_episode,animeJson.GetLastUpdateEpisode(jsonSortTable.get(i))));
            int haveNotWatched=0;
            for(int j=1;j<=animeJson.GetLastUpdateEpisode(jsonSortTable.get(i));j++){
                if(!animeJson.GetEpisodeWatched(jsonSortTable.get(i),j)){
                    strSchedule.append(", ");
                    if(haveNotWatched==0)
                        strSchedule.append(getString(R.string.label_schedule_to_watch));
                    haveNotWatched++;
                    strSchedule.append(j);
                }
            }
            listItem.put("schedule",strSchedule.toString());
            listItems.add(listItem);
        }
        customAdapter.setViewBinder((view, o, s) -> {
            if(view instanceof ImageView&& o instanceof Bitmap){
                ((ImageView)view).setImageBitmap((Bitmap)o);
                return true;
            }
            return false;
        });
        customAdapter.SetOnImageClickListener(R.id.imageCover,view -> {
            int position=(int)view.getTag();
            String coverPath=GetLocalCoverPath(animeJson.GetCoverUrl(jsonSortTable.get(position)),position);
            if(FileUtility.IsFileExists(coverPath)) {
                ActivityOptionsCompat options=ActivityOptionsCompat.makeSceneTransitionAnimation(this,view,getString(R.string.tag_transition_name_image_view));
                Intent intent=new Intent(this,ImageViewActivity.class);
                intent.setData(Uri.parse("file://"+coverPath));
                startActivity(intent,options.toBundle());
            }
        });
        listAnime.setAdapter(customAdapter);
        listAnime.setSelectionFromTop(posListAnimeScroll,posListAnimeTop);
        setTitle(getString(R.string.app_name)+getString(R.string.title_total_count,animeJson.GetAnimeCount()));
    }

    //排序，method:0=不排序，3=评分，1=更新日期，2=观看日期，order:0=不排序，1=升序，2=降序，sep_ab:是否分开弃番
    private void RebuildSortTable(final int method,final int order,final boolean sep_ab){
        lastTopItemIndex=lastBottomItemIndex=-1;
        sortOrder=order;
        int listCount=animeJson.GetAnimeCount();
        jsonSortTable=new ArrayList<>(listCount);
        for(int i=0;i<listCount;i++)
            jsonSortTable.add(i);
        final YMDDate last_watch_date_a=new YMDDate(),last_watch_date_b=new YMDDate();//这样真的OK？
        Collections.sort(jsonSortTable, (a, b) -> {
            switch (order){
                case 1:
                    switch (method){
                        case 0:
                            return Integer.compare(a,b);
                        case 3:
                            return Integer.compare(animeJson.GetRank(a), animeJson.GetRank(b));
                        case 2:
                            last_watch_date_a.FromString(animeJson.GetLastWatchDateStringForAnime(a));
                            last_watch_date_b.FromString(animeJson.GetLastWatchDateStringForAnime(b));
                            if(last_watch_date_a.IsEarlierThanDate(last_watch_date_b))
                                return -1;
                            else if(last_watch_date_a.IsSameToDate(last_watch_date_b))
                                return 0;
                            else
                                return 1;
                        case 1:default:
                            if(animeJson.GetLastUpdateYMDDate(a).IsEarlierThanDate(animeJson.GetLastUpdateYMDDate(b)))
                                return -1;
                            else if(animeJson.GetLastUpdateYMDDate(a).IsSameToDate(animeJson.GetLastUpdateYMDDate(b)))
                                return Integer.compare(animeJson.GetUpdateTime(a),animeJson.GetUpdateTime(b));
                            else
                                return 1;
                    }
                case 2:
                    switch (method){
                        case 0:
                            return Integer.compare(b,a);
                        case 3:
                            return Integer.compare(animeJson.GetRank(b), animeJson.GetRank(a));
                        case 2:
                            last_watch_date_a.FromString(animeJson.GetLastWatchDateStringForAnime(a));
                            last_watch_date_b.FromString(animeJson.GetLastWatchDateStringForAnime(b));
                            if(last_watch_date_a.IsLaterThanDate(last_watch_date_b))
                                return -1;
                            else if(last_watch_date_a.IsSameToDate(last_watch_date_b))
                                return 0;
                            else
                                return 1;
                        case 1:default:
                            if(animeJson.GetLastUpdateYMDDate(a).IsLaterThanDate(animeJson.GetLastUpdateYMDDate(b)))
                                return -1;
                            else if(animeJson.GetLastUpdateYMDDate(a).IsSameToDate(animeJson.GetLastUpdateYMDDate(b)))
                                return Integer.compare(animeJson.GetUpdateTime(b),animeJson.GetUpdateTime(a));
                            else
                                return 1;
                    }
            }
            return 0;
        });
        if(sep_ab) {
            int processingTotal = listCount;
            for (int i = 0; i < processingTotal; ) {
                if (animeJson.GetAbandoned(jsonSortTable.get(i))) {
                    jsonSortTable.add(jsonSortTable.get(i));
                    jsonSortTable.remove(i);
                    processingTotal--;
                } else {
                    i++;
                }
            }
        }
    }

    private void GetAnimeUpdateInfo(boolean onStartup){
        if(onStartup&&preferences.getString(Values.keyAnimeInfoDate,Values.vDefaultString).contentEquals(YMDDate.GetTodayDate().ToYMDString()))
            return;
        StringBuilder msg=new StringBuilder();
        if(animeJson.GetAnimeCount()==0)
            msg.append(getString(R.string.message_no_data));
        else
            msg.append(getString(R.string.message_last_watched_info,new YMDDate(animeJson.GetLastWatchDateString()).ToLocalizedFormatString(),
                    animeJson.GetTitle(animeJson.GetLastWatchIndex()),animeJson.GetLastWatchEpisode()));
        msg.append("\n").append(getString(R.string.message_update_info));
        int behindCount=0;
        ArrayList<Integer>uTable=new ArrayList<>();
        for(int i=0;i<jsonSortTable.size();i++) {
            if(sortOrder==2)
                uTable.add(i, jsonSortTable.get(jsonSortTable.size() - i - 1));
            else
                uTable.add(i,jsonSortTable.get(i));
        }
        for(int i=0;i<animeJson.GetAnimeCount();i++){
            if(!animeJson.GetAbandoned(uTable.get(i))){
                boolean haveNotWatched=false;
                for(int j=0;j<animeJson.GetLastUpdateEpisode(uTable.get(i));j++){
                    if(!animeJson.GetEpisodeWatched(uTable.get(i), j+1)){
                        haveNotWatched=true;
                        break;
                    }
                }
                if(haveNotWatched) {
                    behindCount++;
                    msg.append("\n").append(getString(R.string.message_title_updated_to,animeJson.GetTitle(uTable.get(i)),
                            animeJson.GetLastUpdateYMDDate(uTable.get(i)).ToLocalizedFormatString(),animeJson.GetLastUpdateEpisode(uTable.get(i))));
                }
            }
        }
        if(behindCount==0){
            msg.append(getString(R.string.message_followed_all_anime));
        }
        AlertDialog dlg=new AlertDialog.Builder(this)
                .setTitle(R.string.action_show_anime_update)
                .setView(R.layout.dialog_anime_update_info)
                .setPositiveButton(android.R.string.ok,null)
                .setNeutralButton(R.string.button_dont_show_again_today, (dialogInterface, i) -> preferences.edit().putString(Values.keyAnimeInfoDate,YMDDate.GetTodayDate().ToYMDString()).apply())
                .show();
        ((TextView)dlg.findViewById(R.id.textAnimeUpdateInfo)).setText(msg.toString());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    SimpleCursorAdapter suggestionsAdapter;

    @Override
    public boolean onPrepareOptionsMenu(Menu menu){
        super.onPrepareOptionsMenu(menu);
        //https://developer.android.google.cn/training/search/setup
        //设置搜索属性
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        final SearchView searchView = (SearchView) menu.findItem(R.id.app_bar_search).getActionView();
        searchView.setSuggestionsAdapter(suggestionsAdapter);
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setOnSuggestionListener(new SearchView.OnSuggestionListener() {
            @Override
            public boolean onSuggestionSelect(int i) {
                return onSuggestionClick(i);
            }

            @Override
            public boolean onSuggestionClick(int i) {
                //查询选择的是什么建议
                //https://stackoverflow.com/a/50385750（答案有误）
                MatrixCursor c=(MatrixCursor)searchView.getSuggestionsAdapter().getCursor();
                c.moveToPosition(i);
                searchView.setQuery(c.getString(1),true);
                return true;
            }
        });
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                UpdateSuggestionAdapter(s);
                return false;
            }
        });
        return true;
    }

    private void UpdateSuggestionAdapter(String queryStr){
        final MatrixCursor c=new MatrixCursor(new String[]{BaseColumns._ID,"title"});
        for(int i=0;i<animeJson.GetAnimeCount();i++){
            if(animeJson.GetTitle(i).toLowerCase().contains(queryStr.toLowerCase()))
                c.addRow(new Object[]{i,animeJson.GetTitle(i)});
        }
        suggestionsAdapter.changeCursor(c);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()){
            case R.id.action_settings:OnActionSettings();return true;
            case R.id.action_show_anime_update:OnShowAnimeUpdate();return true;
            case R.id.action_view_web_page:OnViewWebPage();return true;
            case R.id.action_add_item:OnAddAnime();return true;
            case R.id.action_remove_all_item:OnRemoveAllAnime();return true;
            case R.id.action_show_count_statistics:ShowCountStatistics();return true;
            case R.id.action_test_availability:TestAvailability();return true;
            case R.id.action_drive_download:GoogleDriveDownload();return true;
            case R.id.action_drive_upload:GoogleDriveUpload();return true;
            case R.id.action_about:OnActionAbout();return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void TestAvailability(){
        startActivity(new Intent(this,TestAvailabilityActivity.class));
    }

    private void OnActionAbout(){
        startActivity(new Intent(this,AboutActivity.class));
    }

    private GoogleDriveOperator googleDriveOperator;

    private void GoogleDriveDownload(){
        if(googleDriveOperator==null)
            return;
        new AlertDialog.Builder(this)
                .setIcon(R.drawable.ic_warning_black_24dp)
                .setTitle(R.string.action_google_drive_download)
                .setMessage(R.string.message_overwrite_local_warning)
                .setPositiveButton(android.R.string.yes, (dialogInterface, i) -> {
                    googleDriveOperator.SetOnGoogleDriveDownloadSuccessActions(new GoogleDriveOperator.OnOperationSuccessActions() {
                        @Override
                        public void OnOperationSuccess(Object extra) {
                            Toast.makeText(getBaseContext(),R.string.message_google_drive_download_success,Toast.LENGTH_LONG).show();
                            SaveAndReloadJsonFile(false);
                        }
                    });
                    if(googleDriveOperator.IsAccountSignIn())
                        googleDriveOperator.DownloadFromDrive(Values.appIdentifier,Values.pathJsonDataOnRepository[0],Values.GetJsonDataFullPath(getBaseContext()));
                    else {
                        googleDriveOperator.SetOnSignInSuccessActions(new GoogleDriveOperator.OnOperationSuccessActions() {
                            @Override
                            public void OnOperationSuccess(Object extra) {
                                ((GoogleDriveOperator)extra).DownloadFromDrive(Values.appIdentifier,Values.pathJsonDataOnRepository[0],Values.GetJsonDataFullPath(getBaseContext()));
                            }
                        }.SetExtra(googleDriveOperator));
                        googleDriveOperator.SignInClient();
                    }
                })
                .setNegativeButton(android.R.string.no,null)
                .show();
    }

    private void GoogleDriveUpload(){
        if(googleDriveOperator==null)
            return;
        new AlertDialog.Builder(this)
                .setIcon(R.drawable.ic_warning_black_24dp)
                .setTitle(R.string.action_google_drive_upload)
                .setMessage(R.string.message_overwrite_google_drive_warning)
                .setPositiveButton(android.R.string.yes, (dialogInterface, i) -> {
                    if(googleDriveOperator.IsAccountSignIn())
                        googleDriveOperator.UploadToDrive(Values.GetJsonDataFullPath(getBaseContext()),Values.appIdentifier,Values.pathJsonDataOnRepository[0]);
                    else {
                        googleDriveOperator.SetOnSignInSuccessActions(new GoogleDriveOperator.OnOperationSuccessActions() {
                            @Override
                            public void OnOperationSuccess(Object extra) {
                                ((GoogleDriveOperator)extra).UploadToDrive(Values.GetJsonDataFullPath(getBaseContext()),Values.appIdentifier,Values.pathJsonDataOnRepository[0]);
                            }
                        }.SetExtra(googleDriveOperator));
                        googleDriveOperator.SignInClient();
                    }
                })
                .setNegativeButton(android.R.string.no,null)
                .show();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu,View v,ContextMenu.ContextMenuInfo menuInfo){
        getMenuInflater().inflate(R.menu.menu_anime_list,menu);
        String url=animeJson.GetWatchUrl(jsonSortTable.get(longPressedListItem));
        boolean downloadAvailable=URLUtility.IsBilibiliSeasonBangumiLink(url)||
                URLUtility.IsAcFunLink(url)||
                URLUtility.IsQQVideoLink(url)||
                URLUtility.IsYoukuLink(url)||
                URLUtility.IsIQiyiLink(url);
        if(!downloadAvailable)
            menu.findItem(R.id.action_download).setEnabled(false).setTitle(R.string.menu_download_not_available);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item){
        switch (item.getItemId()){
            case R.id.action_show_detail:ShowAnimeDetail(jsonSortTable.get(longPressedListItem));break;
            case R.id.action_edit_watched_episodes:EditWatchedEpisodes(jsonSortTable.get(longPressedListItem));break;
            case R.id.action_edit_item:EditAnime(jsonSortTable.get(longPressedListItem),false);break;
            case R.id.action_remove_item:RemoveItem(jsonSortTable.get(longPressedListItem));break;
            case R.id.action_download:OpenDownloadDialog(jsonSortTable.get(longPressedListItem));break;
        }
        return false;
    }

    private void EditWatchedEpisodes(final int index){
        int lastWatchEpisode=animeJson.GetLastWatchEpisodeForAnime(index);
        String watchDate=animeJson.GetLastWatchDateStringForAnime(index);
        if(lastWatchEpisode!=0&&!watchDate.equals(Values.dateStringDefault)&&animeJson.GetEpisodeWatchedIntDate(index,lastWatchEpisode)==0)
            animeJson.SetEpisodeWatchedIntDate(index,lastWatchEpisode,new YMDDate(watchDate).To8DigitsInt());
        EditWatchedEpisodeDialog dlg=new EditWatchedEpisodeDialog(this);
        dlg.SetJson(animeJson);
        dlg.SetOnOkListener((dialogInterface, i) -> {
            Toast.makeText(getBaseContext(),R.string.message_saving_item,Toast.LENGTH_LONG).show();
            int i_list,i_prev_list;
            for(i_prev_list=0;i_prev_list<jsonSortTable.size();i_prev_list++){
                if(jsonSortTable.get(i_prev_list)==index)
                    break;
            }
            SaveAndReloadJsonFile(true);
            for(i_list=0;i_list<jsonSortTable.size();i_list++){
                if(jsonSortTable.get(i_list)==index){
                    if(i_list!=i_prev_list)
                        listAnime.setSelection(i_list);
                    break;
                }
            }
        });
        dlg.Show(index);
    }

    private void OpenDownloadDialog(int index){//这个index已经是json中的索引了，无需再通过排序表查找
        String url=animeJson.GetWatchUrl(index);
        if(URLUtility.IsBilibiliSeasonBangumiLink(url)) {
            new BilibiliDownloadDialog(this).OpenDownloadDialog(animeJson, index);
        }else if(URLUtility.IsAcFunLink(url)){
            new AcFunDownloadDialog(this).OpenDownloadDialog(animeJson,index);
        }else if(URLUtility.IsQQVideoLink(url)){
            new QQDownloadDialog(this).OpenDownloadDialog(animeJson,index);
        }else if(URLUtility.IsYoukuLink(url)){
            new YoukuDownloadDialog(this).OpenDownloadDialog(animeJson,index);
        }else if(URLUtility.IsIQiyiLink(url)){
            new IQiyiDownloadDialog(this).OpenDownloadDialog(animeJson,index);
        }
    }

    public void ShowCountStatistics(){
        int followed_sub=0,followed_ab=0,following_sub=0,following_ab=0,notwatched_sub=0,notwatched_ab=0;
        int broadcasting_sub=0,broadcasting_ab=0,finished_sub=0,finished_ab=0,total_sub=0,total_ab=0;
        for(int i=0;i<animeJson.GetAnimeCount();i++){
            int epi_watched=0;
            for(int j=0;j<animeJson.GetEpisodeCount(i)||j<animeJson.GetLastUpdateEpisode(i);j++){
                if(animeJson.GetEpisodeWatched(i,j+1))
                    epi_watched++;
            }
            if(epi_watched==0){
                if(animeJson.GetAbandoned(i))
                    notwatched_ab++;
                else
                    notwatched_sub++;
            }else if(epi_watched<animeJson.GetLastUpdateEpisode(i)){
                if(animeJson.GetAbandoned(i))
                    following_ab++;
                else
                    following_sub++;
            }else{
                if(animeJson.GetAbandoned(i))
                    followed_ab++;
                else
                    followed_sub++;
            }
            if(animeJson.GetLastUpdateEpisode(i)==animeJson.GetEpisodeCount(i)){
                if(animeJson.GetAbandoned(i))
                    finished_ab++;
                else
                    finished_sub++;
            }else{
                if(animeJson.GetAbandoned(i))
                    broadcasting_ab++;
                else
                    broadcasting_sub++;
            }
            if(animeJson.GetAbandoned(i))
                total_ab++;
            else
                total_sub++;
        }
        AlertDialog statDialog=new AlertDialog.Builder(this)
                .setTitle(R.string.action_show_count_statistics)
                .setView(R.layout.statistic_dialog)
                .setPositiveButton(android.R.string.ok,null)
                .show();
        ((TextView)statDialog.findViewById(R.id.textStatFollowedSub)).setText(String.valueOf(followed_sub));
        ((TextView)statDialog.findViewById(R.id.textStatFollowedAb)).setText(String.valueOf(followed_ab));
        ((TextView)statDialog.findViewById(R.id.textStatFollowingSub)).setText(String.valueOf(following_sub));
        ((TextView)statDialog.findViewById(R.id.textStatFollowingAb)).setText(String.valueOf(following_ab));
        ((TextView)statDialog.findViewById(R.id.textStatNotwatchedSub)).setText(String.valueOf(notwatched_sub));
        ((TextView)statDialog.findViewById(R.id.textStatNotwatchedAb)).setText(String.valueOf(notwatched_ab));
        ((TextView)statDialog.findViewById(R.id.textStatOnairSub)).setText(String.valueOf(broadcasting_sub));
        ((TextView)statDialog.findViewById(R.id.textStatOnairAb)).setText(String.valueOf(broadcasting_ab));
        ((TextView)statDialog.findViewById(R.id.textStatEndSub)).setText(String.valueOf(finished_sub));
        ((TextView)statDialog.findViewById(R.id.textStatEndAb)).setText(String.valueOf(finished_ab));
        ((TextView)statDialog.findViewById(R.id.textStatTotalSub)).setText(String.valueOf(total_sub));
        ((TextView)statDialog.findViewById(R.id.textStatTotalAb)).setText(String.valueOf(total_ab));
    }

    private void RemoveItem(final int index){
        new AlertDialog.Builder(this)
                .setMessage(R.string.message_remove_warning)
                .setTitle(animeJson.GetTitle(index))
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    animeJson.RemoveItem(index);
                    SaveAndReloadJsonFile(true);
                })
                .setNegativeButton(android.R.string.cancel,null)
                .show();
    }

    private void OnAddAnime(){
        int ni=animeJson.AddNewItem();
        jsonSortTable.add(ni);
        EditAnime(ni,true);
    }

    private void OnAddAnimeCallback_Revert(){
        int li=animeJson.GetAnimeCount()-1;
        animeJson.RemoveItem(li);
        jsonSortTable.remove(li);
    }

    private void OnRemoveAllAnime(){
        new AlertDialog.Builder(this)
                .setIcon(R.drawable.ic_warning_black_24dp)
                .setTitle(R.string.message_notice_title)
                .setMessage(R.string.message_remove_all_warning)
                .setPositiveButton(android.R.string.yes, (dialogInterface, i) -> RemoveAllAnime())
                .setNegativeButton(android.R.string.no,null)
                .show();
    }

    private int ParseStringToInt(String str,int iDefault){
        if(str.contentEquals(""))
            return iDefault;
        return Integer.parseInt(str);
    }

    private void ShowAnimeDetail(int index){
        AlertDialog detailDialog=new AlertDialog.Builder(this)
                .setTitle(animeJson.GetTitle(index))
                .setView(R.layout.anime_detail_dialog)
                .setPositiveButton(android.R.string.ok,null)
                .show();
        ((TextView)detailDialog.findViewById(R.id.textAnimeDescription)).setText(animeJson.GetDescription(index));
        ((TextView)detailDialog.findViewById(R.id.textAnimeActors)).setText(animeJson.GetActors(index));
        ((TextView)detailDialog.findViewById(R.id.textAnimeStaff)).setText(animeJson.GetStaff(index));
        StringBuilder strCat=new StringBuilder();
        for (String cat : animeJson.GetCategory(index)) {
            if (strCat.length() > 0)
                strCat.append(", ");
            strCat.append(cat);
        }
        ((TextView)detailDialog.findViewById(R.id.textAnimeCategory)).setText(strCat.toString());
        ((TextView)detailDialog.findViewById(R.id.textAnimeCreateDate)).setText(getString(R.string.label_anime_create_date,new YMDDate(animeJson.GetCreationDateStringForAnime(index)).ToLocalizedFormatString()));
    }

    private EditText editDialogDescription;
    private EditText editDialogActors;
    private EditText editDialogStaff;
    private EditText editDialogCover;
    private EditText editDialogTitle;
    private EditText editDialogStartDate;
    private EditText editDialogUpdateTime;
    private EditText editDialogUpdatePeriod;
    private Spinner comboDialogUpdatePeriodUnit;
    private EditText editDialogEpisodeCount;
    private EditText editDialogAbsenseCount;
    private EditText editDialogWatchUrl;
    private EditText editDialogColor;
    private EditText editDialogCategory;
    private CheckBox checkDialogAbandoned;
    private EditText editDialogRanking;
    private Button buttonDialogAutofill;
    private AlertDialog editDialog;
    private void EditAnime(final int index, final boolean fromAddAction){
        //此处的index已经是对应到JSON的序号了，不用再从排序表里找
        editDialog=new AlertDialog.Builder(this)
                .setTitle(R.string.action_edit_item)
                .setView(R.layout.dialog_edit_anime)
                .setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
                    animeJson.SetDescription(index,editDialogDescription.getText().toString());
                    animeJson.SetActors(index,editDialogActors.getText().toString());
                    animeJson.SetStaff(index,editDialogStaff.getText().toString());
                    animeJson.SetCoverUrl(index,editDialogCover.getText().toString());
                    animeJson.SetTitle(index,editDialogTitle.getText().toString());
                    animeJson.SetStartDate(index,editDialogStartDate.getText().toString());
                    animeJson.SetUpdateTime(index,new MinuteStamp(editDialogUpdateTime.getText().toString()).GetStamp());
                    animeJson.SetUpdatePeriod(index,ParseStringToInt(editDialogUpdatePeriod.getText().toString(),7));
                    switch (comboDialogUpdatePeriodUnit.getSelectedItemPosition()){
                        case 0:animeJson.SetUpdatePeriodUnit(index,AnimeJson.unitDay);break;
                        case 1:animeJson.SetUpdatePeriodUnit(index,AnimeJson.unitMonth);break;
                        case 2:animeJson.SetUpdatePeriodUnit(index,AnimeJson.unitYear);break;
                    }
                    animeJson.SetEpisodeCount(index,ParseStringToInt(editDialogEpisodeCount.getText().toString(),-1));
                    animeJson.SetAbsenseCount(index,ParseStringToInt(editDialogAbsenseCount.getText().toString(),0));
                    animeJson.SetWatchUrl(index,editDialogWatchUrl.getText().toString());
                    animeJson.SetColor(index,editDialogColor.getText().toString());
                    String[]categoryArray=null;
                    if(editDialogCategory.getText().length()>0)
                        categoryArray=editDialogCategory.getText().toString().split(",");
                    animeJson.SetCategory(index,categoryArray);
                    animeJson.SetAbandoned(index,checkDialogAbandoned.isChecked());
                    animeJson.SetRank(index,Math.min(ParseStringToInt(editDialogRanking.getText().toString(),0),5));
                    Toast.makeText(getBaseContext(),R.string.message_saving_item,Toast.LENGTH_LONG).show();
                    int i_list,i_prev_list;
                    for(i_prev_list=0;i_prev_list<jsonSortTable.size();i_prev_list++){
                        if(jsonSortTable.get(i_prev_list)==index)
                            break;
                    }
                    SaveAndReloadJsonFile(true);
                    for(i_list=0;i_list<jsonSortTable.size();i_list++){
                        if(jsonSortTable.get(i_list)==index){
                            if(i_list!=i_prev_list)
                                listAnime.setSelection(i_list);
                            break;
                        }
                    }
                })
                .setNegativeButton(android.R.string.cancel, (dialogInterface, i) -> {
                    if(fromAddAction)
                        OnAddAnimeCallback_Revert();
                })
                .setOnCancelListener(dialogInterface -> {
                    if(fromAddAction)
                        OnAddAnimeCallback_Revert();
                })
                .show();
        //http://blog.csdn.net/nihaoqiulinhe/article/details/49026263
        editDialogDescription= editDialog.findViewById(R.id.editTextDescription);
        editDialogActors= editDialog.findViewById(R.id.editTextActors);
        editDialogStaff= editDialog.findViewById(R.id.editTextStaff);
        editDialogCover= editDialog.findViewById(R.id.editDialogCover);
        editDialogTitle= editDialog.findViewById(R.id.editDialogTitle);
        editDialogStartDate= editDialog.findViewById(R.id.editDialogStartDate);
        editDialogUpdateTime= editDialog.findViewById(R.id.editDialogUpdateTime);
        editDialogUpdatePeriod= editDialog.findViewById(R.id.editDialogUpdatePeriod);
        comboDialogUpdatePeriodUnit= editDialog.findViewById(R.id.comboDialogUpdatePeriodUnit);
        editDialogEpisodeCount= editDialog.findViewById(R.id.editDialogEpisodeCount);
        editDialogAbsenseCount= editDialog.findViewById(R.id.editDialogAbsenseCount);
        editDialogWatchUrl= editDialog.findViewById(R.id.editDialogWatchUrl);
        editDialogColor= editDialog.findViewById(R.id.editDialogColor);
        editDialogCategory= editDialog.findViewById(R.id.editDialogCategory);
        checkDialogAbandoned= editDialog.findViewById(R.id.checkAbandoned);
        editDialogRanking= editDialog.findViewById(R.id.editDialogRank);
        buttonDialogAutofill= editDialog.findViewById(R.id.buttonDialogAutofill);

        editDialogDescription.setText(animeJson.GetDescription(index));
        editDialogActors.setText(animeJson.GetActors(index));
        editDialogStaff.setText(animeJson.GetStaff(index));
        editDialogCover.setText(animeJson.GetCoverUrl(index));
        editDialogTitle.setText(animeJson.GetTitle(index));
        editDialogStartDate.setText(animeJson.GetStartDate(index));
        editDialogUpdateTime.setText(new MinuteStamp(animeJson.GetUpdateTime(index)).ToString());
        editDialogUpdatePeriod.setText(String.valueOf(animeJson.GetUpdatePeriod(index)));
        switch (animeJson.GetUpdatePeriodUnit(index)){
            case AnimeJson.unitDay:comboDialogUpdatePeriodUnit.setSelection(0,true);break;
            case AnimeJson.unitMonth:comboDialogUpdatePeriodUnit.setSelection(1,true);break;
            case AnimeJson.unitYear:comboDialogUpdatePeriodUnit.setSelection(2,true);break;
        }
        editDialogEpisodeCount.setText(String.valueOf(animeJson.GetEpisodeCount(index)));
        editDialogAbsenseCount.setText(String.valueOf(animeJson.GetAbsenseCount(index)));
        editDialogWatchUrl.setText(String.valueOf(animeJson.GetWatchUrl(index)));
        editDialogWatchUrl.requestFocus();
        editDialogWatchUrl.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                //Nothing.
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                //https://developer.android.google.cn/reference/android/widget/TextView.html#setError(java.lang.CharSequence)
                editDialogWatchUrl.setError(null);
            }

            @Override
            public void afterTextChanged(Editable editable) {
                //Nothing.
            }
        });
        editDialogColor.setText(String.valueOf(animeJson.GetColor(index)));
        String[]strCategoryArray=animeJson.GetCategory(index);
        StringBuilder stringBuilder=new StringBuilder();
        for(int i=0;i<strCategoryArray.length;i++){
            if(i!=0)
                stringBuilder.append(",");
            stringBuilder.append(strCategoryArray[i]);
        }
        editDialogCategory.setText(stringBuilder.toString());
        checkDialogAbandoned.setChecked(animeJson.GetAbandoned(index));
        editDialogRanking.setText(String.valueOf(animeJson.GetRank(index)));
        buttonDialogAutofill.setOnClickListener(view -> {
            String urlString=editDialogWatchUrl.getText().toString();
            int i_regex=0;
            for(;i_regex<Values.parsableLinksRegex.length;i_regex++){
                Pattern p=Pattern.compile(Values.parsableLinksRegex[i_regex]);
                Matcher m=p.matcher(urlString);
                if(m.find()){
                    Spider spider=null;
                    if(URLUtility.IsBilibiliBangumiLink(urlString)) {
                        spider = new BilibiliSpider(this);
                    }else if(URLUtility.IsAcFunLink(urlString)){
                        spider=new AcFunSpider(this);
                    }else if(URLUtility.IsIQiyiLink(urlString)) {
                        spider = new IQiyiSpider(this);
                    }else if(URLUtility.IsQQVideoLink(urlString)) {
                        spider = new QQVideoSpider(this);
                    }else if(URLUtility.IsYoukuLink(urlString)) {
                        spider = new YoukuSpider(this);
                    }if(spider!=null) {
                        spider.SetOnReturnDataFunction(onReturnDataFunction);
                        spider.Execute(urlString);
                        break;
                    }
                }
            }
            if(i_regex==Values.parsableLinksRegex.length){
                if(urlString.contains("bilibili")) {
                    if(urlString.equals(Values.urlAnimeWatchUrlDefault)){
                        editDialogWatchUrl.setError(getString(R.string.message_bilibili_url_not_given_ssid));
                    }else {
                        editDialogWatchUrl.setError(getString(R.string.message_not_supported_bilibili_url));
                    }
                }else {
                    editDialogWatchUrl.setError(getString(R.string.message_not_supported_url));
                }
            }
        });
        buttonDialogAutofill.setOnLongClickListener(view -> {
            AndroidUtility.MessageBox(this,StringUtility.ArrayStringToString(Values.parsableLinksRegex,"\n\n"),
                    getString(R.string.message_title_autofill_supported_url));
            return true;
        });
    }

    private Spider.OnReturnDataFunction onReturnDataFunction=new Spider.OnReturnDataFunction() {
        @Override
        public void OnReturnData(AnimeItem data, int status, String resultMessage, int focusId) {
            buttonDialogAutofill.setEnabled(status!=Spider.STATUS_ONGOING);
            if(resultMessage!=null)
                Toast.makeText(getBaseContext(),resultMessage,Toast.LENGTH_LONG).show();
            if(focusId!=0)
                editDialog.findViewById(focusId).requestFocus();
            if(status==Spider.STATUS_FAILED)
                return;
            if(data.watchUrl!=null)
                editDialogWatchUrl.setText(data.watchUrl);
            if(data.coverUrl!=null)
                editDialogCover.setText(data.coverUrl);
            if(data.title!=null)
                editDialogTitle.setText(data.title);
            if(data.description!=null)
                editDialogDescription.setText(data.description);
            if(data.actors!=null)
                editDialogActors.setText(data.actors);
            if(data.staff!=null)
                editDialogStaff.setText(data.staff);
            if(data.startDate!=null)
                editDialogStartDate.setText(data.startDate);
            if(data.updateTime!=null)
                editDialogUpdateTime.setText(data.updateTime);
            editDialogUpdatePeriod.setText(String.valueOf(data.updatePeriod));
            switch (data.updatePeriodUnit){
                case AnimeJson.unitDay:comboDialogUpdatePeriodUnit.setSelection(0,true);break;
                case AnimeJson.unitMonth:comboDialogUpdatePeriodUnit.setSelection(1,true);break;
                case AnimeJson.unitYear:comboDialogUpdatePeriodUnit.setSelection(2,true);break;
            }
            editDialogEpisodeCount.setText(String.valueOf(data.episodeCount));
            editDialogAbsenseCount.setText(String.valueOf(data.absenseCount));
            if(data.categories!=null) {
                StringBuilder stringBuilder = new StringBuilder();
                for (int i = 0; i < data.categories.length; i++) {
                    if (i != 0)
                        stringBuilder.append(",");
                    stringBuilder.append(data.categories[i]);
                }
                editDialogCategory.setText(stringBuilder.toString());
            }
            editDialogRanking.setText(String.valueOf(data.rank));
        }
    };

    private void RemoveAllAnime(){
        ConfirmRemoveAllDialog dlg=new ConfirmRemoveAllDialog(this);
        dlg.SetAnswer(String.valueOf(animeJson.GetAnimeCount()));
        dlg.SetOnOkListener((dialogInterface, i) -> {
            animeJson.ClearAllAnime();
            SaveAndReloadJsonFile(true);
        });
        dlg.Show();
    }

    private void SaveAndReloadJsonFile(boolean save){
        if(save)
            SaveJsonFile();
        ReadJsonFile();
        DisplayList();
        if(notifyService==null)
            BindNotifyService();
        else
            ServiceConnectedActions();
    }

    private void OnShowAnimeUpdate(){
        GetAnimeUpdateInfo(false);
    }

    private void OnViewWebPage(){
        for(int i=0;i<Values.webFiles.length;i++) {
            String webFile=Values.GetRepositoryPathOnLocal(this)+"/"+Values.webFiles[i];
            if (!FileUtility.IsFileExists(webFile)) {
                try{
                if(!FileUtility.WriteFile(webFile,StreamUtility.GetStringFromStream(getResources().openRawResource(Values.resIdWebFiles[i])))){
                    Toast.makeText(this, getString(R.string.message_cannot_write_to_file,webFile), Toast.LENGTH_LONG).show();
                    return;
                }}catch (IOException e){
                    return;
                }
            }
        }
        Intent intent=new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.fromFile(new File(Values.GetRepositoryPathOnLocal(this)+"/"+Values.webFiles[0])));
        intent.setPackage("com.android.chrome");
        try {
            startActivity(intent);
        }catch (Exception e){
            if(e instanceof ActivityNotFoundException)
                Toast.makeText(this,R.string.message_cannot_launch_chrome,Toast.LENGTH_LONG).show();
            startActivity(new Intent(this,AnimeWeb.class));
        }
    }

    private void OnActionSettings(){
        startActivityForResult(new Intent(this,SettingsActivity.class),R.id.action_settings&0xFFFF);
    }

    @Override
    protected void onActivityResult (int requestCode,int resultCode,Intent data){
        if(requestCode==(R.id.action_settings&0xFFFF)&&resultCode==RESULT_OK){
            if(data.getBooleanExtra(SettingsActivity.keyNeedReload,false))
                SaveAndReloadJsonFile(false);
        }else if(requestCode== (GoogleSignInStatusCodes.SIGN_IN_REQUIRED&0xFFFF)){
            if(googleDriveOperator!=null)
                googleDriveOperator.OnSignInResultReturn(resultCode, data);
        }
        super.onActivityResult(requestCode,resultCode,data);
    }

    @Override
    protected void onStop(){
        if(googleDriveOperator!=null&&googleDriveOperator.IsAccountSignIn())
            googleDriveOperator.SignOutClient();
        super.onStop();
    }
}

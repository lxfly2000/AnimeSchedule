package com.lxfly2000.animeschedule;

import android.app.SearchManager;
import android.content.*;
import android.database.MatrixCursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.BaseColumns;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.*;
import android.widget.*;
import com.google.android.flexbox.FlexboxLayout;
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes;
import com.lxfly2000.utilities.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
        setSupportActionBar((Toolbar)findViewById(R.id.toolbar));
        findViewById(R.id.fabShowAnimeUpdate).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                OnShowAnimeUpdate();
            }
        });

        if(!AndroidUtility.CheckPermissionWithFinishOnDenied(this,
                "android.permission.READ_EXTERNAL_STORAGE",getString(R.string.error_permission_reading_sdcard)))
            return;
        preferences=Values.GetPreference(this);
        if(preferences.getString(Values.keyAnimeInfoDate,Values.vDefaultString).contentEquals(Values.vDefaultString)){
            Toast.makeText(this,R.string.message_build_default_settings,Toast.LENGTH_LONG).show();
            Values.BuildDefaultSettings(this);
        }
        listAnime=(ListView)findViewById(R.id.listAnime);
        registerForContextMenu(listAnime);
        listAnime.setOnItemClickListener(listAnimeCallback);
        listAnime.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                longPressedListItem=i;
                return false;
            }
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
        SaveAndReloadJsonFile(false);
        if(animeJson.GetAnimeCount()>0) {
            GetAnimeUpdateInfo(true);
        }else{
            Snackbar.make(listAnime,R.string.message_no_anime_json,Snackbar.LENGTH_LONG)
                    .setAction(android.R.string.ok, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            try{
                                if(FileUtility.WriteFile(Values.GetJsonDataFullPath(),StreamUtility.GetStringFromStream(getResources().openRawResource(R.raw.anime))))
                                    SaveAndReloadJsonFile(false);
                                else
                                    Toast.makeText(getBaseContext(), getString(R.string.message_cannot_write_to_file,Values.GetJsonDataFullPath()), Toast.LENGTH_LONG).show();
                            }catch (IOException e){
                                //Nothing to do.
                            }
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
            AndroidUtility.OpenUri(getBaseContext(),animeJson.GetWatchUrl(jsonSortTable.get(i)));
        }
    };

    private void ReadJsonFile(){
        if(!FileUtility.IsFileExists(Values.GetJsonDataFullPath())){
            animeJson=new AnimeJson();
            SaveJsonFile();
        }
        animeJson=new AnimeJson(Values.GetJsonDataFullPath());
        Pattern p=Pattern.compile("\\.json$");
        Matcher m=p.matcher(Values.GetJsonDataFullPath());
        if(m.find()){
            File file=new File(Values.GetJsonDataFullPath());
            if(file.renameTo(new File(Values.GetRepositoryPathOnLocal()+"/"+Values.pathJsonDataOnRepository[0]))) {
                Toast.makeText(this, getString(R.string.message_rename_file,file.getName(),Values.pathJsonDataOnRepository[0]), Toast.LENGTH_LONG).show();
            }else {
                Toast.makeText(this, getString(R.string.message_cannot_rename,file.getName()), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void SaveJsonFile(){
        animeJson.SaveToFile(Values.GetJsonDataFullPath());
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
        SimpleAdapter adapter=(SimpleAdapter)listAnime.getAdapter();
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
            AsyncTask<Object,Integer,Boolean>setImageTask=new AsyncTask<Object, Integer, Boolean>() {
                SimpleAdapter listAdapter;
                @Override
                protected Boolean doInBackground(Object... params) {
                    int imgIndex=(Integer)params[0];
                    listAdapter=(SimpleAdapter)params[1];
                    HashMap<String,Object>item=(HashMap)listAdapter.getItem(imgIndex);
                    if(item.get("cover")==null){
                        String coverUrl=animeJson.GetCoverUrl(jsonSortTable.get(imgIndex));
                        String[]tempSplit=coverUrl.split("/");
                        String coverExt="";
                        if(tempSplit.length>0&&tempSplit[tempSplit.length-1].contains(".")){
                            coverExt=tempSplit[tempSplit.length-1].substring(tempSplit[tempSplit.length-1].lastIndexOf('.'));
                        }
                        String coverPath=Values.GetCoverPathOnLocal()+"/"+
                                animeJson.GetTitle(jsonSortTable.get(imgIndex)).replaceAll("[/\":|<>?*]","_")+coverExt;
                        if(FileUtility.IsFileExists(coverPath)){
                            item.put("cover", BitmapFactory.decodeFile(coverPath));
                            return true;
                        }else{
                            AndroidDownloadFileTask task=new AndroidDownloadFileTask() {
                                @Override
                                public void OnReturnStream(ByteArrayInputStream stream, boolean success, Object extra) {
                                    ParametersSetImage param = (ParametersSetImage) extra;
                                    if(success) {
                                        FileUtility.WriteStreamToFile(param.imagePath,stream);
                                        ((HashMap<String, Object>) param.listAdapter.getItem(param.listIndex)).put("cover", BitmapFactory.decodeFile(param.imagePath));
                                    }else {
                                        ((HashMap<String,Object>)param.listAdapter.getItem(param.listIndex)).put("cover",BitmapFactory.decodeResource(getResources(),R.raw.dn_error));
                                        Toast.makeText(getBaseContext(),getString(R.string.message_cannot_download_cover,animeJson.GetCoverUrl(jsonSortTable.get(param.listIndex)),
                                                (String)((HashMap<String,Object>)param.listAdapter.getItem(param.listIndex)).get("title")),Toast.LENGTH_LONG).show();
                                    }
                                    param.listAdapter.notifyDataSetChanged();
                                }
                            };
                            task.SetExtra(new ParametersSetImage(listAdapter,coverPath,imgIndex));
                            task.executeOnExecutor(THREAD_POOL_EXECUTOR,coverUrl,coverPath);
                            item.put("cover",task);
                        }
                    }
                    return false;
                }
                @Override
                protected void onPostExecute(Boolean result){
                    if(result)
                        listAdapter.notifyDataSetChanged();
                }
            };
            setImageTask.execute(i,adapter);
        }
    }

    private void DisplayList(){
        RebuildSortTable(preferences.getInt(Values.keySortMethod,Values.vDefaultSortMethod),
                preferences.getInt(Values.keySortOrder,Values.vDefaultSortOrder),
                preferences.getBoolean(Values.keySortSeperateAbandoned,Values.vDefaultSortSeperateAbandoned));
        ArrayList<HashMap<String,Object>>listItems=new ArrayList<>();
        String[]keyStrings={"title","description","ranking","schedule","cover"};
        int[]viewIds={R.id.textAnimeTitle,R.id.textAnimeDescription,R.id.textRanking,R.id.textSchedule,R.id.imageCover};
        SimpleAdapter customAdapter=new SimpleAdapter(this,listItems,R.layout.item_anime,keyStrings,viewIds);
        for(int i=0;i<animeJson.GetAnimeCount();i++){
            HashMap<String,Object>listItem=new HashMap<>();
            listItem.put("title",animeJson.GetTitle(jsonSortTable.get(i)));
            listItem.put("description",animeJson.GetDescription(jsonSortTable.get(i)));
            StringBuilder rankingString=new StringBuilder();
            for(int j=0;j<5;j++){
                rankingString.append(j<animeJson.GetRank(jsonSortTable.get(i))?"★":"☆");
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
                    strSchedule.append(String.valueOf(j));
                }
            }
            listItem.put("schedule",strSchedule.toString());
            listItems.add(listItem);
        }
        customAdapter.setViewBinder(new SimpleAdapter.ViewBinder() {
            @Override
            public boolean setViewValue(View view, Object o, String s) {
                if(view instanceof ImageView&& o instanceof Bitmap){
                    ((ImageView)view).setImageBitmap((Bitmap)o);
                    return true;
                }
                return false;
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
        Collections.sort(jsonSortTable,new Comparator<Integer>() {
            @Override
            public int compare(Integer a, Integer b) {
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
            }
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
        new AlertDialog.Builder(this)
                .setTitle(R.string.action_show_anime_update)
                .setMessage(msg.toString())
                .setPositiveButton(android.R.string.ok,null)
                .setNeutralButton(R.string.button_dont_show_again_today, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        preferences.edit().putString(Values.keyAnimeInfoDate,YMDDate.GetTodayDate().ToYMDString()).apply();
                    }
                })
                .show();
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
            case R.id.action_drive_download:GoogleDriveDownload();return true;
            case R.id.action_drive_upload:GoogleDriveUpload();return true;
            case R.id.action_about:OnActionAbout();return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void OnActionAbout(){
        startActivity(new Intent(this,AboutActivity.class));
    }

    private GoogleDriveOperator googleDriveOperator;

    private void GoogleDriveDownload(){
        if(googleDriveOperator==null)
            return;
        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(R.string.action_google_drive_download)
                .setMessage(R.string.message_overwrite_local_warning)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        googleDriveOperator.SetOnGoogleDriveDownloadSuccessActions(new GoogleDriveOperator.OnOperationSuccessActions() {
                            @Override
                            public void OnOperationSuccess(Object extra) {
                                Toast.makeText(getBaseContext(),R.string.message_google_drive_download_success,Toast.LENGTH_LONG).show();
                                SaveAndReloadJsonFile(false);
                            }
                        });
                        if(googleDriveOperator.IsAccountSignIn())
                            googleDriveOperator.DownloadFromDrive(Values.appIdentifier,Values.pathJsonDataOnRepository[0],Values.GetJsonDataFullPath());
                        else {
                            googleDriveOperator.SetOnSignInSuccessActions(new GoogleDriveOperator.OnOperationSuccessActions() {
                                @Override
                                public void OnOperationSuccess(Object extra) {
                                    ((GoogleDriveOperator)extra).DownloadFromDrive(Values.appIdentifier,Values.pathJsonDataOnRepository[0],Values.GetJsonDataFullPath());
                                }
                            }.SetExtra(googleDriveOperator));
                            googleDriveOperator.SignInClient();
                        }
                    }
                })
                .setNegativeButton(android.R.string.no,null)
                .show();
    }

    private void GoogleDriveUpload(){
        if(googleDriveOperator==null)
            return;
        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(R.string.action_google_drive_upload)
                .setMessage(R.string.message_overwrite_google_drive_warning)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if(googleDriveOperator.IsAccountSignIn())
                            googleDriveOperator.UploadToDrive(Values.GetJsonDataFullPath(),Values.appIdentifier,Values.pathJsonDataOnRepository[0]);
                        else {
                            googleDriveOperator.SetOnSignInSuccessActions(new GoogleDriveOperator.OnOperationSuccessActions() {
                                @Override
                                public void OnOperationSuccess(Object extra) {
                                    ((GoogleDriveOperator)extra).UploadToDrive(Values.GetJsonDataFullPath(),Values.appIdentifier,Values.pathJsonDataOnRepository[0]);
                                }
                            }.SetExtra(googleDriveOperator));
                            googleDriveOperator.SignInClient();
                        }
                    }
                })
                .setNegativeButton(android.R.string.no,null)
                .show();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu,View v,ContextMenu.ContextMenuInfo menuInfo){
        getMenuInflater().inflate(R.menu.menu_anime_list,menu);
        if(!URLUtility.IsBilibiliSeasonLink(animeJson.GetWatchUrl(jsonSortTable.get(longPressedListItem))))
            menu.findItem(R.id.action_download).setEnabled(false).setTitle(R.string.menu_download_not_available);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item){
        switch (item.getItemId()){
            case R.id.action_show_detail:ShowAnimeDetail(jsonSortTable.get(longPressedListItem));break;
            case R.id.action_edit_item:EditAnime(jsonSortTable.get(longPressedListItem),false);break;
            case R.id.action_remove_item:RemoveItem(jsonSortTable.get(longPressedListItem));break;
            case R.id.action_download:OpenDownloadDialog(jsonSortTable.get(longPressedListItem));break;
        }
        return false;
    }

    private void OpenDownloadDialog(int index){//这个index已经是json中的索引了，无需再通过排序表查找
        new BilibiliDownloadDialog(this).OpenDownloadDialog(animeJson,index);
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
            }else if(epi_watched<animeJson.GetEpisodeCount(i)||epi_watched<animeJson.GetLastUpdateEpisode(i)){
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
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        animeJson.RemoveItem(index);
                        SaveAndReloadJsonFile(true);
                    }
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
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(R.string.message_notice_title)
                .setMessage(R.string.message_remove_all_warning)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        RemoveAllAnime();
                    }
                })
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
    private FlexboxLayout flexboxDialogWatchedEpisode;
    private EditText editDialogColor;
    private EditText editDialogCategory;
    private CheckBox checkDialogAbandoned;
    private EditText editDialogRanking;
    private Button buttonDialogAutofill;
    private void EditAnime(final int index, final boolean fromAddAction){
        //此处的index已经是对应到JSON的序号了，不用再从排序表里找
        AlertDialog editDialog=new AlertDialog.Builder(this)
                .setTitle(R.string.action_edit_item)
                .setView(R.layout.dialog_edit_anime)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
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
                        //读取已观看的集数
                        for(int i_epi=1;i_epi<=animeJson.GetLastUpdateEpisode(index);i_epi++) {
                            boolean i_epi_watched=((ToggleButton)flexboxDialogWatchedEpisode.getChildAt(i_epi-1)).isChecked();
                            if(animeJson.GetEpisodeWatched(index,i_epi)!=i_epi_watched)
                                animeJson.SetEpisodeWatched(index, i_epi, i_epi_watched);
                        }
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
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if(fromAddAction)
                            OnAddAnimeCallback_Revert();
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialogInterface) {
                        if(fromAddAction)
                            OnAddAnimeCallback_Revert();
                    }
                })
                .show();
        //https://www.jianshu.com/p/132398300738
        ScrollView scrollView=(ScrollView)editDialog.findViewById(R.id.scrollViewEditAnime);
        scrollView.setDescendantFocusability(ViewGroup.FOCUS_BEFORE_DESCENDANTS);
        scrollView.setFocusable(true);
        scrollView.setFocusableInTouchMode(true);
        scrollView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                view.requestFocusFromTouch();
                return false;
            }
        });
        //http://blog.csdn.net/nihaoqiulinhe/article/details/49026263
        editDialogDescription=(EditText)editDialog.findViewById(R.id.editTextDescription);
        editDialogActors=(EditText)editDialog.findViewById(R.id.editTextActors);
        editDialogStaff=(EditText)editDialog.findViewById(R.id.editTextStaff);
        editDialogCover=(EditText)editDialog.findViewById(R.id.editDialogCover);
        editDialogTitle=(EditText)editDialog.findViewById(R.id.editDialogTitle);
        editDialogStartDate=(EditText)editDialog.findViewById(R.id.editDialogStartDate);
        editDialogUpdateTime=(EditText)editDialog.findViewById(R.id.editDialogUpdateTime);
        editDialogUpdatePeriod=(EditText)editDialog.findViewById(R.id.editDialogUpdatePeriod);
        comboDialogUpdatePeriodUnit=(Spinner)editDialog.findViewById(R.id.comboDialogUpdatePeriodUnit);
        editDialogEpisodeCount=(EditText)editDialog.findViewById(R.id.editDialogEpisodeCount);
        editDialogAbsenseCount=(EditText)editDialog.findViewById(R.id.editDialogAbsenseCount);
        editDialogWatchUrl=(EditText)editDialog.findViewById(R.id.editDialogWatchUrl);
        flexboxDialogWatchedEpisode=(FlexboxLayout)editDialog.findViewById(R.id.flexboxDialogWatchedEpisodes);
        editDialogColor=(EditText)editDialog.findViewById(R.id.editDialogColor);
        editDialogCategory=(EditText)editDialog.findViewById(R.id.editDialogCategory);
        checkDialogAbandoned=(CheckBox)editDialog.findViewById(R.id.checkAbandoned);
        editDialogRanking=(EditText)editDialog.findViewById(R.id.editDialogRank);
        buttonDialogAutofill=(Button)editDialog.findViewById(R.id.buttonDialogAutofill);

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
        //显示观看的集数
        StringBuilder stringBuilder=new StringBuilder();
        ToggleButton toggleEpisode;
        FlexboxLayout.LayoutParams layoutToggleEpisode=new FlexboxLayout.LayoutParams(FlexboxLayout.LayoutParams.WRAP_CONTENT,FlexboxLayout.LayoutParams.WRAP_CONTENT);
        for(int i=1;i<=animeJson.GetLastUpdateEpisode(index);i++){
            toggleEpisode=new ToggleButton(this);
            toggleEpisode.setLayoutParams(layoutToggleEpisode);
            toggleEpisode.setMinWidth(0);
            toggleEpisode.setMinimumWidth(0);
            toggleEpisode.setTextOn(String.valueOf(i));
            toggleEpisode.setTextOff(String.valueOf(i));
            toggleEpisode.setChecked(animeJson.GetEpisodeWatched(index,i));
            flexboxDialogWatchedEpisode.addView(toggleEpisode);
        }
        editDialogColor.setText(String.valueOf(animeJson.GetColor(index)));
        String[]strCategoryArray=animeJson.GetCategory(index);
        stringBuilder=new StringBuilder();
        for(int i=0;i<strCategoryArray.length;i++){
            if(i!=0)
                stringBuilder.append(",");
            stringBuilder.append(strCategoryArray[i]);
        }
        editDialogCategory.setText(stringBuilder.toString());
        checkDialogAbandoned.setChecked(animeJson.GetAbandoned(index));
        editDialogRanking.setText(String.valueOf(animeJson.GetRank(index)));
        editDialog.findViewById(R.id.buttonDialogAutofill).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String urlString=editDialogWatchUrl.getText().toString();
                int i_regex=0;
                for(;i_regex<Values.parsableLinksRegex.length;i_regex++){
                    Pattern p=Pattern.compile(Values.parsableLinksRegex[i_regex]);
                    Matcher m=p.matcher(urlString);
                    if(m.find()){
                        if(URLUtility.IsBilibiliLink(urlString.toLowerCase())){
                            ReadBilibiliURL_OnCallback(urlString);
                            break;
                        }else if(URLUtility.IsIQiyiLink(urlString.toLowerCase())){
                            GetIQiyiAnimeIDFromURL(urlString);
                            break;
                        }
                    }
                }
                if(i_regex==Values.parsableLinksRegex.length){
                    if(urlString.contains("bilibili"))
                        Toast.makeText(getBaseContext(),R.string.message_not_supported_bilibili_url,Toast.LENGTH_LONG).show();
                    else
                        Toast.makeText(getBaseContext(),R.string.message_not_supported_url,Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void ReadBilibiliURL_OnCallback(final String urlString){//2018-11-14：B站原来的两个JSON的API均已失效，现在改为了HTML内联JS代码
        /*输入URL：parsableLinkRegex中的任何一个B站URL
        *
        * 在返回的HTML文本（转换成小写）里找ss#####, season_id:#####, "season_id":#####, ssid:#####, "ssid":#####
        * 得到的数值均为 Season ID, 然后就可以从ss##### URL里获取信息了。
        * */
        if(URLUtility.IsBilibiliSeasonLink(urlString)){
            ReadBilibiliSSID_OnCallback(URLUtility.GetBilibiliSeasonIdString(urlString));
            return;
        }
        editDialogTitle.setText(R.string.message_fetching_bilibili_ssid);
        AndroidDownloadFileTask task=new AndroidDownloadFileTask() {
            @Override
            public void OnReturnStream(ByteArrayInputStream stream, boolean success, Object extra) {
                if(!success){
                    Toast.makeText(getBaseContext(),R.string.message_unable_to_fetch_episode_id,Toast.LENGTH_LONG).show();
                    return;
                }
                try{
                    String htmlString=StreamUtility.GetStringFromStream(stream);
                    Matcher m=Pattern.compile("ss[0-9]+").matcher(htmlString);
                    if(m.find()){
                        ReadBilibiliSSID_OnCallback(htmlString.substring(m.start()+2,m.end()));
                        return;
                    }
                    m=Pattern.compile("season_id:[0-9]+").matcher(htmlString);
                    if(m.find()){
                        ReadBilibiliSSID_OnCallback(htmlString.substring(m.start()+10,m.end()));
                        return;
                    }
                    m=Pattern.compile("\"season_id\":[0-9]+").matcher(htmlString);
                    if(m.find()){
                        ReadBilibiliSSID_OnCallback(htmlString.substring(m.start()+12,m.end()));
                        return;
                    }
                    m=Pattern.compile("ssId:[0-9]+").matcher(htmlString);
                    if(m.find()){
                        ReadBilibiliSSID_OnCallback(htmlString.substring(m.start()+5,m.end()));
                        return;
                    }
                    m=Pattern.compile("\"ssId\":[0-9]+").matcher(htmlString);
                    if(m.find()){
                        ReadBilibiliSSID_OnCallback(htmlString.substring(m.start()+7,m.end()));
                        return;
                    }
                    String ssid_not_found_string=getString(R.string.message_bilibili_ssid_not_found);
                    if(urlString.startsWith("http:"))
                        ssid_not_found_string+="\n"+getString(R.string.message_bilibili_ssid_not_found_advise);
                    Toast.makeText(getBaseContext(),ssid_not_found_string,Toast.LENGTH_LONG).show();
                }catch (IOException e){
                    Toast.makeText(getBaseContext(),getString(R.string.message_io_exception,e.getLocalizedMessage()),Toast.LENGTH_LONG).show();
                }
            }
        };
        task.execute(urlString);
    }

    private void ReadBilibiliSSID_OnCallback(final String idString){
        editDialogTitle.setText("SSID:"+idString);
        final String requestUrl="https://www.bilibili.com/bangumi/play/ss"+idString;
        AndroidDownloadFileTask task=new AndroidDownloadFileTask() {
            @Override
            public void OnReturnStream(ByteArrayInputStream stream, boolean success, Object extra) {
                if(!success){
                    Toast.makeText(getBaseContext(),R.string.message_unable_to_fetch_anime_info,Toast.LENGTH_LONG).show();
                    return;
                }
                try {
                    String jsonString=URLUtility.GetBilibiliJsonContainingSSID(StreamUtility.GetStringFromStream(stream),idString);
                    if(jsonString==null){
                        Toast.makeText(getBaseContext(),getString(R.string.message_bilibili_ssid_code_not_found,idString),Toast.LENGTH_LONG).show();
                        return;
                    }
                    JSONObject htmlJson=new JSONObject(jsonString);
                    editDialogCover.setText(htmlJson.getJSONObject("mediaInfo").getString("cover"));
                    editDialogTitle.setText(htmlJson.getJSONObject("mediaInfo").getString("series_title"));
                    editDialogDescription.setText(htmlJson.getJSONObject("mediaInfo").getString("evaluate"));
                    editDialogActors.setText(htmlJson.getJSONObject("mediaInfo").getString("actors"));
                    editDialogStaff.setText(htmlJson.getJSONObject("mediaInfo").getString("staff"));
                    String[]pubTimeParts=htmlJson.getJSONObject("pubInfo").getString("pub_time").split(" ");
                    editDialogStartDate.setText(pubTimeParts[0]);
                    editDialogUpdateTime.setText(pubTimeParts[1]);
                    if(htmlJson.getJSONObject("pubInfo").getString("weekday").contentEquals("-1")){
                        editDialogUpdatePeriod.setText("1");
                        comboDialogUpdatePeriodUnit.setSelection(1,true);
                    }else if("0123456".contains(htmlJson.getJSONObject("pubInfo").getString("weekday"))){
                        editDialogUpdatePeriod.setText("7");
                        comboDialogUpdatePeriodUnit.setSelection(0,true);
                    }
                    String countString=htmlJson.getJSONObject("mediaInfo").getString("total_ep");
                    if(countString.contentEquals("0"))
                        editDialogEpisodeCount.setText("-1");
                    else
                        editDialogEpisodeCount.setText(countString);
                    StringBuilder tagString=new StringBuilder();
                    for(int i=0;i<htmlJson.getJSONObject("mediaInfo").getJSONArray("style").length();i++){
                        if(i!=0)
                            tagString.append(",");
                        tagString.append(htmlJson.getJSONObject("mediaInfo").getJSONArray("style").getString(i));
                    }
                    editDialogCategory.setText(tagString.toString());
                    editDialogWatchUrl.setText(requestUrl);//为避免输入的URL无法被客户端打开把URL统一改成SSID形式
                    editDialogRanking.setText(String.valueOf(Math.round(htmlJson.getJSONObject("mediaRating").getDouble("score")/2)));
                }catch (JSONException e){
                    Toast.makeText(getBaseContext(),getString(R.string.message_exception,e.getLocalizedMessage()),Toast.LENGTH_LONG).show();
                }catch (IOException e){
                    Toast.makeText(getBaseContext(),getString(R.string.message_error_on_reading_stream,e.getLocalizedMessage()),Toast.LENGTH_LONG).show();
                }
            }
        };
        task.execute(requestUrl);
    }

    private void GetIQiyiAnimeDescriptionFromTaiwanURL(String url, String htmlString){
        if(htmlString==null){
            AndroidDownloadFileTask task=new AndroidDownloadFileTask() {
                @Override
                public void OnReturnStream(ByteArrayInputStream stream, boolean success, Object extra) {
                    if(!success){
                        Toast.makeText(getBaseContext(),R.string.message_unable_to_read_url,Toast.LENGTH_SHORT).show();
                        return;
                    }
                    try {
                        GetIQiyiAnimeDescriptionFromTaiwanURL((String)extra, StreamUtility.GetStringFromStream(stream));
                    }catch (IOException e){
                        Toast.makeText(getBaseContext(),getString(R.string.message_error_on_reading_stream,e.getLocalizedMessage()),Toast.LENGTH_LONG).show();
                    }
                }
            };
            task.SetExtra(url);
            task.execute(url);
            return;
        }
        Matcher m=Pattern.compile("more-desc *= *\"?[^\"]+\"?").matcher(htmlString);
        if(m.find()){
            String descString=htmlString.substring(m.start(),m.end());
            editDialogDescription.setText(descString.substring(descString.indexOf('\"')+1,descString.lastIndexOf('\"')));
            return;
        }
        if(url.toLowerCase().contains("/v_")&&url.substring(0,url.lastIndexOf('/')).toLowerCase().contains("tw")) {
            Matcher mLink = Pattern.compile(url.substring(url.indexOf(':') + 1, url.lastIndexOf('/')).concat("/a_[a-zA-Z0-9]+\\.html")).matcher(htmlString);
            if(mLink.find())
                GetIQiyiAnimeDescriptionFromTaiwanURL(url.substring(0,url.indexOf(':')+1).concat(htmlString.substring(mLink.start(), mLink.end())), null);
        }
    }

    private void GetIQiyiAnimeActorsInfo(String url,String htmlString){
        if(htmlString==null){
            AndroidDownloadFileTask task=new AndroidDownloadFileTask() {
                @Override
                public void OnReturnStream(ByteArrayInputStream stream, boolean success, Object extra) {
                    if(!success){
                        Toast.makeText(getBaseContext(),R.string.message_unable_to_read_url,Toast.LENGTH_SHORT).show();
                        return;
                    }
                    try {
                        GetIQiyiAnimeActorsInfo((String)extra, StreamUtility.GetStringFromStream(stream));
                    }catch (IOException e){
                        Toast.makeText(getBaseContext(),getString(R.string.message_error_on_reading_stream,e.getLocalizedMessage()),Toast.LENGTH_LONG).show();
                    }
                }
            };
            task.SetExtra(url);
            task.SetUserAgent("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.1 (KHTML, like Gecko) Chrome/22.0.1207.1 Safari/537.1");
            task.execute(url);
            return;
        }
        String strJson=URLUtility.GetIQiyiJsonContainingActorsInfo(htmlString);
        if(strJson!=null){
            try{
                JSONArray jsonCast=new JSONObject(strJson).getJSONArray("dubbers");
                StringBuilder strCast=new StringBuilder();
                for(int j=0;j<jsonCast.length();j++){
                    if(j>0)
                        strCast.append("\n");
                    JSONArray jsonRoles=jsonCast.getJSONObject(j).getJSONArray("roles");
                    for(int i=0;i<jsonRoles.length();i++){
                        if(i>0)
                            strCast.append("，");
                        strCast.append(jsonRoles.getString(i));
                    }
                    strCast.append("：").append(jsonCast.getJSONObject(j).getString("name"));
                }
                editDialogActors.setText(strCast.toString());
            }catch (JSONException e){
                Toast.makeText(this,getString(R.string.message_json_exception,e.getLocalizedMessage()),Toast.LENGTH_LONG).show();
            }
            return;
        }
        if(url.toLowerCase().contains("/a_")) {
            Matcher mLink = Pattern.compile(url.substring(url.indexOf(':') + 1, url.lastIndexOf('/')).concat("/v_[a-zA-Z0-9]+\\.html")).matcher(htmlString);
            if(mLink.find())
                GetIQiyiAnimeActorsInfo(url.substring(0,url.indexOf(':')+1).concat(htmlString.substring(mLink.start(), mLink.end())), null);
        }
        if(url.startsWith("http:"))
            GetIQiyiAnimeActorsInfo(url.replaceFirst("http","https"),null);
    }

    private void GetIQiyiAnimeIDFromURL(String url){
        //根据目前（2018-10-1）观察到的情况，爱奇艺的链接无论是a链接还是v链接都有含有“albumId: #########,”代码的脚本，通过此就能查询到番剧的数字ID
        editDialogTitle.setText(url);
        AndroidDownloadFileTask task=new AndroidDownloadFileTask() {
            @Override
            public void OnReturnStream(ByteArrayInputStream stream, boolean success, Object extra) {
                if(!success){
                    Toast.makeText(getBaseContext(),R.string.message_unable_to_read_url,Toast.LENGTH_LONG).show();
                    return;
                }
                Pattern p=Pattern.compile("albumId: *\"?[0-9]+\"?,");
                try {
                    String htmlString = StreamUtility.GetStringFromStream(stream);//整个网页的内容
                    Matcher m = p.matcher(htmlString);
                    boolean mfind=false;
                    if(m.find())
                        mfind=true;
                    else{
                        p=Pattern.compile("a(lbum-)?id *= *\"[0-9]+\"");
                        m=p.matcher(htmlString);
                        if(m.find())
                            mfind=true;
                        else{
                            if(((String)extra).startsWith("http:")) {
                                GetIQiyiAnimeIDFromURL(((String) extra).replaceFirst("http", "https"));
                                return;
                            }
                        }
                    }
                    GetIQiyiAnimeActorsInfo((String)extra,htmlString);
                    GetIQiyiAnimeDescriptionFromTaiwanURL((String)extra,htmlString);
                    if(mfind) {
                        htmlString = htmlString.substring(m.start(), m.end());//数字ID所在代码的内容
                        Pattern pSub=Pattern.compile("[0-9]+");
                        Matcher mSub=pSub.matcher(htmlString);
                        if(mSub.find())
                            ReadIQiyiJson_OnCallback(htmlString.substring(mSub.start(),mSub.end()));//数字ID的字符串
                        else
                            Toast.makeText(getBaseContext(),R.string.message_unable_get_id_number,Toast.LENGTH_LONG).show();
                    }else{
                        Toast.makeText(getBaseContext(),R.string.message_unable_get_id_number_line,Toast.LENGTH_LONG).show();
                    }
                }catch (IOException e){
                    Toast.makeText(getBaseContext(),getString(R.string.message_error_on_reading_stream,e.getLocalizedMessage()),Toast.LENGTH_LONG).show();
                }
            }
        };
        task.SetExtra(url);
        task.execute(url);
    }

    private void ReadIQiyiJson_OnCallback(String idString){
        editDialogTitle.setText("Album ID: "+idString);
        //String jsonUrlGetAlbumId="https://nl-rcd.iqiyi.com/apis/urc/getalbumrc?albumId="+idString;//因为此链接爱奇艺要求登录或验证所以无法使用
        String jsonUrlGetSnsScore="https://pcw-api.iqiyi.com/video/score/getsnsscore?qipu_ids="+idString;
        String jsonpUrlGetAvList="https://cache.video.iqiyi.com/jp/avlist/"+idString+"/1/50/";//这后面的1/50好像没有什么影响的吧……
        /*AndroidDownloadFileTask taskDownloadJsonGetAlbumId=new AndroidDownloadFileTask() {
            @Override
            public void OnReturnStream(ByteArrayInputStream stream, boolean success, Object extra) {
                if(!success){
                    Toast.makeText(getBaseContext(),"无法获取 GetAlbumRC.",Toast.LENGTH_LONG).show();
                    return;
                }
                try {
                    JSONObject jsonObject = new JSONObject(StreamUtility.GetStringFromStream(stream));
                    editDialogCover.setText(jsonObject.getJSONObject("data").getString("albumImageUrl"));
                    editDialogTitle.setText(jsonObject.getJSONObject("data").getString("albumName"));
                    String tvYearString=String.valueOf(jsonObject.getJSONObject("data").getInt("tvYear"));
                    editDialogStartDate.setText(tvYearString.substring(0,4)+"-"+tvYearString.substring(4,6)+"-"+tvYearString.substring(6));

                    //总集数可能是data.allSet，data.allSets或data.mpd，AvList链接中pt，allNum……中的一个
                    editDialogEpisodeCount.setText(String.valueOf(jsonObject.getJSONObject("data").getInt("allSet")));
                    //editDialogEpisodeCount.setText(String.valueOf(jsonObject.getJSONObject("data").getInt("allSets")));
                    //editDialogEpisodeCount.setText(String.valueOf(jsonObject.getJSONObject("data").getInt("mpd")));
                }catch (JSONException e){
                    Toast.makeText(getBaseContext(),"发生异常：\n"+e.getLocalizedMessage(),Toast.LENGTH_LONG).show();
                }catch (IOException e){
                    Toast.makeText(getBaseContext(),"读取流出错：\n"+e.getLocalizedMessage(),Toast.LENGTH_LONG).show();
                }
            }
        };*/
        AndroidDownloadFileTask taskDownloadJsonGetSnsScore=new AndroidDownloadFileTask() {
            @Override
            public void OnReturnStream(ByteArrayInputStream stream, boolean success, Object extra) {
                if(!success){
                    Toast.makeText(getBaseContext(),getString(R.string.message_cannot_fetch_property,"GetSnsScore"),Toast.LENGTH_LONG).show();
                    return;
                }
                try {
                    JSONObject jsonObject=new JSONObject(StreamUtility.GetStringFromStream(stream));
                    editDialogRanking.setText(String.valueOf(Math.round(jsonObject.getJSONArray("data").getJSONObject(0).getDouble("sns_score"))/2));
                }catch (JSONException e){
                    Toast.makeText(getBaseContext(),getString(R.string.message_exception,e.getLocalizedMessage()),Toast.LENGTH_LONG).show();
                }catch (IOException e){
                    Toast.makeText(getBaseContext(),getString(R.string.message_error_on_reading_stream,e.getLocalizedMessage()),Toast.LENGTH_LONG).show();
                }
            }
        };
        AndroidDownloadFileTask taskDownloadJsonpGetAvList=new AndroidDownloadFileTask() {
            @Override
            public void OnReturnStream(ByteArrayInputStream stream, boolean success, Object extra) {
                if(!success){
                    Toast.makeText(getBaseContext(),getString(R.string.message_cannot_fetch_property,"GetAvList"),Toast.LENGTH_LONG).show();
                    return;
                }
                try {
                    String jsonString=StreamUtility.GetStringFromStream(stream);
                    jsonString=jsonString.substring(jsonString.indexOf('{'),jsonString.lastIndexOf('}')+1);
                    JSONObject jsonObject=new JSONObject(jsonString);
                    String descString=jsonObject.getJSONObject("data").getJSONArray("vlist").getJSONObject(0).getString("desc");
                    if(descString.length()>0)
                        editDialogDescription.setText(descString);
                    String qiyiPlayStrategy=jsonObject.getJSONObject("data").getString("ps");
                    if(qiyiPlayStrategy.contains("每周")){
                        editDialogUpdatePeriod.setText("7");
                        comboDialogUpdatePeriodUnit.setSelection(0,true);
                    }
                    Matcher mTime=Pattern.compile("[0-9]+:[0-9]+").matcher(qiyiPlayStrategy);
                    if(mTime.find())
                        editDialogUpdateTime.setText(qiyiPlayStrategy.substring(mTime.start(),mTime.end()));
                    ReadIQiyiJsonpAnimeCategory_OnCallback(String.valueOf(jsonObject.getJSONObject("data").getJSONArray("vlist").getJSONObject(0).getInt("id")),
                            jsonObject.getJSONObject("data").getJSONArray("vlist").getJSONObject(0).getString("vid"));
                }catch (JSONException e){
                    Toast.makeText(getBaseContext(),getString(R.string.message_exception,e.getLocalizedMessage()),Toast.LENGTH_LONG).show();
                }catch (IOException e){
                    Toast.makeText(getBaseContext(),getString(R.string.message_error_on_reading_stream,e.getLocalizedMessage()),Toast.LENGTH_LONG).show();
                }
            }
        };
        //taskDownloadJsonGetAlbumId.execute(jsonUrlGetAlbumId);
        taskDownloadJsonGetSnsScore.execute(jsonUrlGetSnsScore);
        taskDownloadJsonpGetAvList.execute(jsonpUrlGetAvList);
    }

    private void ReadIQiyiJsonpAnimeCategory_OnCallback(String tvidString,String vidString){
        String requestUrl="https://cache.video.iqiyi.com/jp/vi/"+tvidString+"/"+vidString+"/";
        AndroidDownloadFileTask task=new AndroidDownloadFileTask() {
            @Override
            public void OnReturnStream(ByteArrayInputStream stream, boolean success, Object extra) {
                if(!success){
                    Toast.makeText(getBaseContext(),R.string.message_iqiyi_cannot_fetch_category,Toast.LENGTH_LONG).show();
                    return;
                }
                try {
                    String jsonString=StreamUtility.GetStringFromStream(stream);
                    jsonString=jsonString.substring(jsonString.indexOf('{'),jsonString.lastIndexOf('}')+1);
                    JSONObject jsonObject=new JSONObject(jsonString);
                    editDialogCategory.setText(jsonObject.getString("tg").replaceAll(" ",","));
                    editDialogTitle.setText(jsonObject.getString("an"));
                    String startTimeString=jsonObject.getString("stm");
                    if(startTimeString.length()>=8)
                        editDialogStartDate.setText(startTimeString.substring(0,4)+"-"+startTimeString.substring(4,6)+"-"+startTimeString.substring(6));
                    else
                        Toast.makeText(getBaseContext(),getString(R.string.message_date_string_too_short,startTimeString.length()),Toast.LENGTH_LONG).show();
                    editDialogEpisodeCount.setText(String.valueOf(jsonObject.getInt("es")));//其他地方也有疑似总集数的属性
                    editDialogCover.setText(jsonObject.getString("apic"));
                }catch (JSONException e){
                    Toast.makeText(getBaseContext(),getString(R.string.message_exception,e.getLocalizedMessage()),Toast.LENGTH_LONG).show();
                }catch (IOException e){
                    Toast.makeText(getBaseContext(),getString(R.string.message_error_on_reading_stream,e.getLocalizedMessage()),Toast.LENGTH_LONG).show();
                }
            }
        };
        task.execute(requestUrl);
    }

    private void RemoveAllAnime(){
        animeJson.ClearAllAnime();
        SaveAndReloadJsonFile(true);
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
            String webFile=Values.GetRepositoryPathOnLocal()+"/"+Values.webFiles[i];
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
        intent.setData(Uri.fromFile(new File(Values.GetRepositoryPathOnLocal()+"/"+Values.webFiles[0])));
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
    }

    @Override
    protected void onStop(){
        if(googleDriveOperator!=null&&googleDriveOperator.IsAccountSignIn())
            googleDriveOperator.SignOutClient();
        super.onStop();
    }
}

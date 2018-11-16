/*TODO:
* 读取json文件并显示（OK）
* 异步加载图片，更新列表显示（OK）
* 根据json中项目的更新日期排序（OK）
* 可增加/删除/长按修改项目，并保存至本地文件（OK）
* 对于B站链接，可根据链接自动获取所需信息（简介，时间，分类等）（OK）
* 可直接在列表上标记观看集数
* 点击打开链接（OK）
* 更新集数提示（用对话框显示，可选择今日不再提示(Neu)/关闭(Posi)）（OK）
* 完善搜索功能（OK）
*/

package com.lxfly2000.animeschedule;

import android.app.SearchManager;
import android.content.*;
import android.content.pm.PackageManager;
import android.database.MatrixCursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import com.google.android.flexbox.FlexboxLayout;
import com.lxfly2000.utilities.*;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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

        //注册检查更新广播接收器
        IntentFilter fiUpdate=new IntentFilter();
        fiUpdate.addAction(ACTION_CHECK_UPDATE_RESULT);
        checkUpdateReceiver=new CheckUpdateReceiver();
        registerReceiver(checkUpdateReceiver,fiUpdate);
        //检测更新
        CheckForUpdate(true);
        //处理Intent
        HandleIntent(getIntent());
        //初始化搜索建议相关变量
        final String[]suggestionKeys=new String[]{"title"};
        final int[]suggestionsIds=new int[]{android.R.id.text1};
        suggestionsAdapter=new SimpleCursorAdapter(this,android.R.layout.simple_list_item_1,null,suggestionKeys,
                suggestionsIds,CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
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
        }
    }

    private AdapterView.OnItemClickListener listAnimeCallback=new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(animeJson.GetWatchUrl(jsonSortTable.get(i)))));
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
            strSchedule.append(animeJson.GetLastUpdateYMDDate(jsonSortTable.get(i)).ToYMDString())
                    .append(getString(R.string.label_schedule_update_episode,animeJson.GetLastUpdateEpisode(jsonSortTable.get(i))));
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

    //排序，method:0=评分，1=更新日期，2=观看日期，order:0=不排序，1=升序，2=降序，sep_ab:是否分开弃番
    private void RebuildSortTable(final int method,final int order,final boolean sep_ab){
        lastTopItemIndex=lastBottomItemIndex=-1;
        sortOrder=order;
        int listCount=animeJson.GetAnimeCount();
        jsonSortTable=new ArrayList<>(listCount);
        for(int i=0;i<listCount;i++)
            jsonSortTable.add(i);
        final YMDDate last_watch_date_a=new YMDDate(),last_watch_date_b=new YMDDate();//这样真的OK？
        jsonSortTable.sort(new Comparator<Integer>() {
            @Override
            public int compare(Integer a, Integer b) {
                switch (order){
                    case 1:
                        switch (method){
                            case 0:
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
                                    return 0;
                                else
                                    return 1;
                        }
                    case 2:
                        switch (method){
                            case 0:
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
                                else if(animeJson.GetLastUpdateYMDDate(a).IsLaterThanDate(animeJson.GetLastUpdateYMDDate(b)))
                                    return 0;
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
            msg.append(getString(R.string.message_last_watched_info,animeJson.GetLastWatchDateString(),
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
                            animeJson.GetLastUpdateYMDDate(uTable.get(i)).ToYMDString(),animeJson.GetLastUpdateEpisode(uTable.get(i))));
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
            if(animeJson.GetTitle(i).toLowerCase().startsWith(queryStr.toLowerCase()))
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
            case R.id.action_check_update:CheckForUpdate(false);return true;
            case R.id.action_show_count_statistics:ShowCountStatistics();return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu,View v,ContextMenu.ContextMenuInfo menuInfo){
        getMenuInflater().inflate(R.menu.menu_anime_list,menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item){
        switch (item.getItemId()){
            case R.id.action_edit_item:EditAnime(jsonSortTable.get(longPressedListItem),false);break;
            case R.id.action_remove_item:RemoveItem(jsonSortTable.get(longPressedListItem));break;
        }
        return false;
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

    private EditText editDialogDescription;
    private EditText editDialogCover;
    private EditText editDialogTitle;
    private EditText editDialogStartDate;
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
    //TODO：其他对话框中的控件
    private void EditAnime(final int index, final boolean fromAddAction){
        //此处的index已经是对应到JSON的序号了，不用再从排序表里找
        //TODO：完善对话框
        AlertDialog editDialog=new AlertDialog.Builder(this)
                .setTitle(R.string.action_edit_item)
                .setView(R.layout.dialog_edit_anime)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        animeJson.SetDescription(index,editDialogDescription.getText().toString());
                        animeJson.SetCoverUrl(index,editDialogCover.getText().toString());
                        animeJson.SetTitle(index,editDialogTitle.getText().toString());
                        animeJson.SetStartDate(index,editDialogStartDate.getText().toString());
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
                        SaveAndReloadJsonFile(true);
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
        //http://blog.csdn.net/nihaoqiulinhe/article/details/49026263
        editDialogDescription=(EditText)editDialog.findViewById(R.id.editTextDescription);
        editDialogCover=(EditText)editDialog.findViewById(R.id.editDialogCover);
        editDialogTitle=(EditText)editDialog.findViewById(R.id.editDialogTitle);
        editDialogStartDate=(EditText)editDialog.findViewById(R.id.editDialogStartDate);
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

        editDialogDescription.setText(animeJson.GetDescription(index));
        editDialogCover.setText(animeJson.GetCoverUrl(index));
        editDialogTitle.setText(animeJson.GetTitle(index));
        editDialogStartDate.setText(animeJson.GetStartDate(index));
        editDialogUpdatePeriod.setText(String.valueOf(animeJson.GetUpdatePeriod(index)));
        switch (animeJson.GetUpdatePeriodUnit(index)){
            case AnimeJson.unitDay:comboDialogUpdatePeriodUnit.setSelection(0,true);break;
            case AnimeJson.unitMonth:comboDialogUpdatePeriodUnit.setSelection(1,true);break;
            case AnimeJson.unitYear:comboDialogUpdatePeriodUnit.setSelection(2,true);break;
        }
        editDialogEpisodeCount.setText(String.valueOf(animeJson.GetEpisodeCount(index)));
        editDialogAbsenseCount.setText(String.valueOf(animeJson.GetAbsenseCount(index)));
        editDialogWatchUrl.setText(String.valueOf(animeJson.GetWatchUrl(index)));
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
                        if(urlString.toLowerCase().contains("bilibili")){
                            ReadBilibiliURL_OnCallback(urlString);
                            break;
                        }else if(urlString.toLowerCase().contains("iqiyi")){
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
        Matcher mssid=Pattern.compile("/[0-9]+").matcher(urlString);
        if(mssid.find()){
            ReadBilibiliSSID_OnCallback(urlString.substring(mssid.start()+1,mssid.end()));
            return;
        }
        mssid=Pattern.compile("/ss[0-9]+").matcher(urlString);
        if(mssid.find()){
            ReadBilibiliSSID_OnCallback(urlString.substring(mssid.start()+3,mssid.end()));
            return;
        }
        editDialogTitle.setText("获取SSID中……");
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
                    String ssid_not_found_string="未找到SSID属性。";
                    if(urlString.startsWith("http:"))
                        ssid_not_found_string+="\n请尝试将URL前面的HTTP改为HTTPS。";
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
                    String htmlString=StreamUtility.GetStringFromStream(stream);
                    Matcher m=Pattern.compile("<script>[^<>]+"+idString+"[^<>]+</script>").matcher(htmlString);
                    if(!m.find()){
                        Toast.makeText(getBaseContext(),"未找到含有SSID为"+idString+"的脚本代码。",Toast.LENGTH_LONG).show();
                        return;
                    }
                    htmlString=htmlString.substring(m.start(),m.end());
                    htmlString=htmlString.substring(htmlString.indexOf('{'));
                    int brackets=1,posJSONEnd;
                    for(posJSONEnd=1;posJSONEnd<htmlString.length();posJSONEnd++){
                        if(htmlString.charAt(posJSONEnd)=='{')
                            brackets++;
                        else if(htmlString.charAt(posJSONEnd)=='}')
                            brackets--;
                        if(brackets==0){
                            posJSONEnd++;
                            break;
                        }
                    }
                    if(brackets!=0){
                        Toast.makeText(getBaseContext(),"JSON代码无效。",Toast.LENGTH_LONG).show();
                        return;
                    }
                    htmlString=htmlString.substring(0,posJSONEnd);
                    JSONObject htmlJson=new JSONObject(htmlString);
                    editDialogCover.setText(htmlJson.getJSONObject("mediaInfo").getString("cover"));
                    editDialogTitle.setText(htmlJson.getJSONObject("mediaInfo").getString("series_title"));
                    editDialogDescription.setText(htmlJson.getJSONObject("mediaInfo").getString("evaluate"));
                    editDialogStartDate.setText(htmlJson.getJSONObject("pubInfo").getString("pub_time").split(" ")[0]);
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
                    editDialogRanking.setText(String.valueOf(Math.round(htmlJson.getJSONObject("mediaRating").getDouble("score")/2)));
                    StringBuilder tagString=new StringBuilder();
                    for(int i=0;i<htmlJson.getJSONObject("mediaInfo").getJSONArray("style").length();i++){
                        if(i!=0)
                            tagString.append(",");
                        tagString.append(htmlJson.getJSONObject("mediaInfo").getJSONArray("style").getString(i));
                    }
                    editDialogCategory.setText(tagString.toString());
                    editDialogWatchUrl.setText(requestUrl);//为避免输入的URL无法被客户端打开把URL统一改成SSID形式
                }catch (JSONException e){
                    Toast.makeText(getBaseContext(),getString(R.string.message_exception,e.getLocalizedMessage()),Toast.LENGTH_LONG).show();
                }catch (IOException e){
                    Toast.makeText(getBaseContext(),getString(R.string.message_error_on_reading_stream,e.getLocalizedMessage()),Toast.LENGTH_LONG).show();
                }
            }
        };
        task.execute(requestUrl);
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
                Pattern p=Pattern.compile("albumId: *[0-9]+,");
                try {
                    String htmlString = StreamUtility.GetStringFromStream(stream);//整个网页的内容
                    Matcher m = p.matcher(htmlString);
                    boolean mfind=false;
                    if(m.find())
                        mfind=true;
                    else{
                        p=Pattern.compile("aid *= *\"[0-9]+\"");
                        m=p.matcher(htmlString);
                        if(m.find())
                            mfind=true;
                        else{
                            if(((String)extra).toLowerCase().startsWith("http:")) {
                                GetIQiyiAnimeIDFromURL(((String) extra).replaceFirst("http", "https"));
                                return;
                            }
                        }
                    }
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
                    editDialogDescription.setText(jsonObject.getJSONObject("data").getJSONArray("vlist").getJSONObject(0).getString("desc"));
                    if(jsonObject.getJSONObject("data").getString("ps").contains("每周")){
                        editDialogUpdatePeriod.setText("7");
                        comboDialogUpdatePeriodUnit.setSelection(0,true);
                    }
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
                    editDialogEpisodeCount.setText(String.valueOf(jsonObject.getInt("es")));//TODO:其他地方也有疑似总集数的属性
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
        Intent intent=new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setData(Uri.fromFile(new File(Values.GetRepositoryPathOnLocal()+"/"+Values.webFiles[0])));
        intent.setClassName("com.android.chrome","com.google.android.apps.chrome.Main");
        try {
            startActivity(intent);
        }catch (ActivityNotFoundException e){
            Toast.makeText(this,R.string.message_cannot_launch_chrome,Toast.LENGTH_LONG).show();
            Intent intentFallback=new Intent();
            intentFallback.setAction(intent.getAction());
            intentFallback.setDataAndType(intent.getData(),"*/*");
            startActivity(intentFallback);
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
        }
    }

    private static final String INTENT_EXTRA_UPDATE_ONLY="onlyUpdate";
    private static final String INTENT_EXTRA_FOUND_UPDATE="foundUpdate";
    private static final String ACTION_CHECK_UPDATE_RESULT=BuildConfig.APPLICATION_ID+".CheckUpdateResult";
    private UpdateChecker updateChecker=null;

    private CheckUpdateReceiver checkUpdateReceiver;
    class CheckUpdateReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean onlyReportNewVersion=intent.getBooleanExtra(INTENT_EXTRA_UPDATE_ONLY,true);
            boolean foundNewVersion=intent.getBooleanExtra(INTENT_EXTRA_FOUND_UPDATE,false);
            AlertDialog.Builder msgBox=new AlertDialog.Builder(context);//这里不能用getApplicationContext.
            msgBox.setPositiveButton(android.R.string.ok,null);
            msgBox.setTitle(R.string.menu_check_update);
            if (foundNewVersion) {
                String msg = String.format(getString(R.string.message_new_version), BuildConfig.VERSION_NAME, updateChecker.GetUpdateVersionName());
                msgBox.setMessage(msg);
                msgBox.setIcon(android.R.drawable.ic_dialog_info);
                msgBox.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Intent intent=new Intent(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse(Values.urlAuthor));
                        startActivity(intent);
                    }
                });
                msgBox.setNegativeButton(android.R.string.cancel,null);
            }else if (onlyReportNewVersion){
                return;
            }else if (updateChecker.IsError()){
                msgBox.setMessage(R.string.error_check_update);
                msgBox.setIcon(android.R.drawable.ic_dialog_alert);
            }else {
                msgBox.setMessage(R.string.message_no_update);
            }
            msgBox.show();
        }
    }

    @Override
    protected void onStop(){
        if(checkUpdateReceiver!=null){
            unregisterReceiver(checkUpdateReceiver);
            checkUpdateReceiver=null;
        }
        super.onStop();
    }

    private void CheckForUpdate(boolean onlyReportNewVersion) {
        if (updateChecker == null) {
            updateChecker = new UpdateChecker().SetCheckURL(Values.GetCheckUpdateURL());
        }
        updateChecker.SetResultHandler(new UpdateChecker.ResultHandler(this) {
            @Override
            protected void OnReceive(boolean foundNewVersion) {
                Intent intent = new Intent(ACTION_CHECK_UPDATE_RESULT);
                intent.putExtra(INTENT_EXTRA_FOUND_UPDATE, foundNewVersion);
                intent.putExtra(INTENT_EXTRA_UPDATE_ONLY, GetOnlyReportUpdate());
                HandlerSendBroadcast(intent);
            }
        }.SetOnlyReportUpdate(onlyReportNewVersion));
        if (checkCallingOrSelfPermission("android.permission.INTERNET") != PackageManager.PERMISSION_GRANTED) {
            if (onlyReportNewVersion)
                return;
            AlertDialog.Builder msgBox = new AlertDialog.Builder(this)
                    .setTitle(R.string.menu_check_update)
                    .setPositiveButton(android.R.string.ok, null)
                    .setMessage(R.string.error_permission_network);
            msgBox.show();
        } else {
            updateChecker.CheckForUpdate();
        }
    }
}

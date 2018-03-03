/*TODO:
* 读取json文件并显示（OK）
* 异步加载图片，更新列表显示
* 根据json中项目的更新日期排序
* 可增加/删除/长按修改项目，并保存至本地文件
* 对于B站链接，可根据链接自动获取所需信息（简介，时间，分类等）
* 可直接在列表上标记观看集数
* 点击打开链接（OK）
* 更新集数提示（用对话框显示，可选择今日不再提示(Neu)/关闭(Posi)）
* JGit上传/下载数据
*/

package com.lxfly2000.animeschedule;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import com.lxfly2000.utilities.AndroidUtility;
import com.lxfly2000.utilities.FileUtility;
import com.lxfly2000.utilities.JSONFormatter;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {
    private JSONObject animeJson,workingJson;//TODO:换成我自己的类
    private ArrayList<Integer>jsonSortTable;
    ListView listAnime;
    FloatingActionButton fab;
    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setSupportActionBar((Toolbar)findViewById(R.id.toolbar));
        fab=(FloatingActionButton)findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                OnActionSettings();
            }
        });

        if(!AndroidUtility.CheckPermissionWithFinishOnDenied(this,
                "android.permission.READ_EXTERNAL_STORAGE","No reading permission."))
            return;
        preferences=Values.GetPreference(this);
        if(preferences.getString(Values.keyRepositoryUrl,Values.vDefaultString).contentEquals(Values.vDefaultString)){
            Toast.makeText(this,R.string.message_build_default_settings,Toast.LENGTH_LONG).show();
            Values.BuildDefaultSettings(this);
        }
        listAnime=(ListView)findViewById(R.id.listAnime);
        listAnime.setOnItemClickListener(listAnimeCallback);
        ReadJsonFile();
        DisplayList();
        GetAnimeUpdateInfo(true);
    }

    AdapterView.OnItemClickListener listAnimeCallback=new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(animeJson.getJSONArray("anime").getJSONObject(i).getString("watch_url"))));
            }catch (JSONException e){
                Toast.makeText(getBaseContext(),e.getMessage(),Toast.LENGTH_LONG).show();
            }
        }
    };

    private void ReadJsonFile(){
        try{
            animeJson=new JSONObject(FileUtility.ReadFile(Values.GetJsonDataFullPath()));
        }catch (JSONException e){
            Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show();
        }
    }

    private void SaveJsonFile(){
        FileUtility.WriteFile(Values.GetJsonDataFullPath(), JSONFormatter.Format(animeJson.toString()));
    }

    private void DisplayList(){
        BuildSortTable(2);
        ArrayList<HashMap<String,Object>>listItems=new ArrayList<>();
        try{
            for(int i=0;i<animeJson.getJSONArray("anime").length();i++){
                HashMap<String,Object>listItem=new HashMap<>();
                JSONObject animeObject=animeJson.getJSONArray("anime").getJSONObject(jsonSortTable.get(i));
                listItem.put("title",animeObject.getString("title"));
                listItem.put("description",animeObject.getString("description"));
                StringBuilder rankingString=new StringBuilder();
                for(int j=0;j<5;j++){
                    rankingString.append(j<animeObject.getInt("rank")?"★":"☆");
                }
                listItem.put("ranking",rankingString.toString());
                listItem.put("schedule",animeObject.getString("start_date")+" 开始放送");
                listItem.put("cover", R.mipmap.ic_launcher);
                listItems.add(listItem);
            }
        }catch (JSONException e){
            Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show();
        }
        String[]keyStrings={"title","description","ranking","schedule","cover"};
        int[]viewIds={R.id.textAnimeTitle,R.id.textAnimeDescription,R.id.textRanking,R.id.textSchedule,R.id.imageCover};
        listAnime.setAdapter(new SimpleAdapter(this,listItems,R.layout.item_anime,keyStrings,viewIds));
    }

    //排序，order:0=不排序，1=升序，2=降序
    private void BuildSortTable(int order){
        try {
            int listCount=animeJson.getJSONArray("anime").length();
            jsonSortTable=new ArrayList<>(listCount);
            for(int i=0;i<listCount;i++)
                jsonSortTable.add(i);
            if(order==0)
                return;
            jsonSortTable.sort(new Comparator<Integer>() {
                @Override
                public int compare(Integer integer, Integer t1) {
                    //TODO：按更新日期排序
                    return 0;
                }
            });
        }catch (JSONException e){
            Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show();
        }
    }

    private void GetAnimeUpdateInfo(boolean onStartup){
        if(onStartup&&preferences.getString(Values.keyAnimeInfoDate,Values.vDefaultString).contentEquals("【Today】"))
            return;
        //TODO:显示番剧的更新信息
        AndroidUtility.MessageBox(this,"TODO:制作中。");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.menu_main,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()){
            case R.id.action_settings:OnActionSettings();return true;
            case R.id.action_show_anime_update:OnShowAnimeUpdate();return true;
            case R.id.action_view_web_page:OnViewWebPage();return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void OnShowAnimeUpdate(){
        GetAnimeUpdateInfo(false);
    }

    private void OnViewWebPage(){
        String url=preferences.getString(Values.keyRepositoryUrl,"");
        if(!url.startsWith("https://github.com")){
            AndroidUtility.MessageBox(this,getString(R.string.message_not_supported_url));
            return;
        }
        url=url.substring(19);
        if(url.endsWith(".git"))
            url=url.substring(0,url.length()-4);
        String[]urlParts=url.split("/");
        url="https://"+urlParts[0]+".github.io/"+urlParts[1];
        startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)));
    }

    private void OnActionSettings(){
        startActivityForResult(new Intent(this,SettingsActivity.class),R.id.action_settings&0xFFFF);
    }
}

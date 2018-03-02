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
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {
    JSONObject animeJson;
    ListView listAnime;
    FloatingActionButton fab;

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
        listAnime=(ListView)findViewById(R.id.listAnime);
        listAnime.setOnItemClickListener(listAnimeCallback);
        try {
            animeJson=new JSONObject(FileUtility.ReadFile(Values.GetJsonDataFullPath()));
        }catch (JSONException e){
            Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show();
        }
        DisplayList();
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

    public void DisplayList(){
        ArrayList<HashMap<String,Object>>listItems=new ArrayList<>();
        try{
            for(int i=0;i<animeJson.getJSONArray("anime").length();i++){
                HashMap<String,Object>listItem=new HashMap<>();
                listItem.put("title",animeJson.getJSONArray("anime").getJSONObject(i).getString("title"));
                listItem.put("description",animeJson.getJSONArray("anime").getJSONObject(i).getString("description"));
                StringBuilder rankingString=new StringBuilder();
                for(int j=0;j<5;j++){
                    rankingString.append(j>animeJson.getJSONArray("anime").getJSONObject(i).getInt("rank")?"☆":"★");
                }
                listItem.put("ranking",rankingString.toString());
                listItem.put("schedule",animeJson.getJSONArray("anime").getJSONObject(i).getString("start_date")+" 开始放送");
                listItem.put("cover", Uri.parse(animeJson.getJSONArray("anime").getJSONObject(i).getString("cover")));
                listItems.add(listItem);
            }
        }catch (JSONException e){
            Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show();
        }
        String[]keyStrings={"title","description","ranking","schedule","cover"};
        int[]viewIds={R.id.textAnimeTitle,R.id.textAnimeDescription,R.id.textRanking,R.id.textSchedule,R.id.imageCover};
        listAnime.setAdapter(new SimpleAdapter(this,listItems,R.layout.item_anime,keyStrings,viewIds));
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
        }
        return super.onOptionsItemSelected(item);
    }

    private void OnActionSettings(){
        startActivityForResult(new Intent(this,SettingsActivity.class),R.id.action_settings&0xFFFF);
    }
}

/*TODO:
* 读取json文件并显示
* 根据json中项目的更新日期排序
* 可增加/删除/修改项目，可直接在列表上标记观看集数，评分
* 更新集数提示
* JGit上传/下载数据
*/

package com.lxfly2000.animeschedule;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import com.lxfly2000.utilities.AndroidUtility;
import com.lxfly2000.utilities.FileUtility;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

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
        try {
            animeJson=new JSONObject(FileUtility.ReadFile(Values.GetJsonDataFullPath()));
            int count=animeJson.getJSONArray("anime").length();
            ArrayList<String>titles=new ArrayList<>();
            for(int i=0;i<count;i++){
                titles.add(animeJson.getJSONArray("anime").getJSONObject(i).getString("title"));
            }
            listAnime.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_list_item_1,titles));
        }catch (JSONException e){
            Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show();
        }
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

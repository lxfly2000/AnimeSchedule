package com.lxfly2000.animeschedule;

import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;

public class AnimeWeb extends AppCompatActivity {
    WebView webAnime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_anime_web);
        getSupportActionBar().hide();
        webAnime=(WebView)findViewById(R.id.webviewAnime);
        webAnime.getSettings().setJavaScriptEnabled(true);
        SharedPreferences preferences=Values.GetPreference(this);
        String starMarks=Values.starMarks[preferences.getInt(Values.keyStarMark,Values.vDefaultStarMark)];
        String starMarkFull=starMarks.substring(0,1),starMarkEmpty=starMarks.substring(1,2);
        webAnime.loadUrl("file://"+Values.GetRepositoryPathOnLocal()+"/"+Values.webFiles[0]+"?mark_full="+starMarkFull+"&mark_empty="+starMarkEmpty);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.menu_web_page,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()){
            case R.id.action_close_anime_box:
                webAnime.loadUrl("javascript:document.getElementsByClassName(\"ButtonCloseBox\")[0].click();");
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

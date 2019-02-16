package com.lxfly2000.animeschedule;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.webkit.WebView;

public class AnimeWeb extends AppCompatActivity {
    WebView webAnime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_anime_web);
        webAnime=(WebView)findViewById(R.id.webviewAnime);
        webAnime.getSettings().setJavaScriptEnabled(true);
        webAnime.loadUrl("file://"+Values.GetRepositoryPathOnLocal()+"/"+Values.webFiles[0]);
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

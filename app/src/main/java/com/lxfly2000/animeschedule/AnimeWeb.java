package com.lxfly2000.animeschedule;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Window;
import android.webkit.WebView;

public class AnimeWeb extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_anime_web);
        WebView webAnime=(WebView)findViewById(R.id.webviewAnime);
        webAnime.getSettings().setJavaScriptEnabled(true);
        webAnime.loadUrl("file://"+Values.GetRepositoryPathOnLocal()+"/"+Values.webFiles[0]);
    }
}

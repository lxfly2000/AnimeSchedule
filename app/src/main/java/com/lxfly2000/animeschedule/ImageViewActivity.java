package com.lxfly2000.animeschedule;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import androidx.annotation.NonNull;
import com.github.chrisbanes.photoview.PhotoView;
import com.lxfly2000.utilities.AndroidUtility;

import java.io.File;

public class ImageViewActivity extends Activity {
    private PhotoView photoView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_view);

        photoView=findViewById(R.id.photo_view);
        photoView.setImageURI(getIntent().getData());
        photoView.setOnClickListener(view -> {
            finish();
        });
        registerForContextMenu(photoView);
        photoView.setOnLongClickListener(view -> {
            openContextMenu(view);
            return true;
        });
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        getMenuInflater().inflate(R.menu.menu_image_view,menu);
        super.onCreateContextMenu(menu, v, menuInfo);
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_image_open_external:
                Intent intent=new Intent(Intent.ACTION_VIEW);
                intent.setData(AndroidUtility.GetImageContentUri(this,new File(getIntent().getData().getPath())));
                startActivity(intent);
                break;
        }
        return super.onContextItemSelected(item);
    }
}
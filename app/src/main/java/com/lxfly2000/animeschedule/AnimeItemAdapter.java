package com.lxfly2000.animeschedule;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnimeItemAdapter extends SimpleAdapter {
    public AnimeItemAdapter(Context context, List<? extends Map<String, ?>> data, int resource, String[] from, int[] to) {
        super(context, data, resource, from, to);
    }

    private int imageId;
    private View.OnClickListener imageOnClickListener;

    public void SetOnImageClickListener(int id, View.OnClickListener listener){
        imageId=id;
        imageOnClickListener=listener;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = super.getView(position, convertView, parent);
        View subItem=view.findViewById(imageId);
        if(subItem!=null){
            subItem.setTag(position);
            subItem.setOnClickListener(imageOnClickListener);
        }
        return view;
    }
}

package com.lxfly2000.utilities;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;

public class JSONUtility {
    public static String JSONArrayToString(JSONArray sa, String divStr){
        StringBuilder sb=new StringBuilder();
        for(int i=0;i<sa.length();i++){
            if(sb.length()>0)
                sb.append(divStr);
            try {
                sb.append(sa.getString(i));
            }catch (JSONException e){
                sb.append(e.getClass().getName());
            }
        }
        return sb.toString();
    }

    public static ArrayList<String> JSONArrayToStringArray(JSONArray sa){
        ArrayList<String> als=new ArrayList<>();
        for(int i=0;i<sa.length();i++){
            try {
                als.add(sa.getString(i));
            }catch (JSONException e){
                als.add(e.getClass().getName());
            }
        }
        return als;
    }
}

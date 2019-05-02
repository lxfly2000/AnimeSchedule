package com.lxfly2000.utilities;

import org.json.JSONArray;
import org.json.JSONException;

public class JSONUtility {
    public static String JSONArrayToString(JSONArray sa, String divStr){
        StringBuilder sb=new StringBuilder();
        for(int i=0;i<sa.length();i++){
            if(sb.length()>0)
                sb.append(divStr);
            try {
                sb.append(sa.getString(i));
            }catch (JSONException e){
                sb.append("[JSONException]");
            }
        }
        return sb.toString();
    }
}

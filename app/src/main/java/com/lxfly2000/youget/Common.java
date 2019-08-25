package com.lxfly2000.youget;

import androidx.annotation.NonNull;
import com.lxfly2000.utilities.AndroidDownloadFileTask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Common {
    public static void SetYouGetHttpHeader(AndroidDownloadFileTask task){
        task.SetUserAgent("Mozilla/5.0 (X11; Linux x86_64; rv:64.0) Gecko/20100101 Firefox/64.0");
        task.SetAcceptCharset("UTF-8,*;q=0.5");
        task.SetAcceptEncoding("gzip,deflate,sdch");
        task.SetAccept("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        task.SetAcceptLanguage("en-US,en;q=0.8");
    }

    //参考：https://github.com/soimort/you-get/blob/develop/src/you_get/common.py#L155
    public static byte[] RC4(byte[]key,byte[]data){
        int[]state=new int[256];
        for(int i=0;i<state.length;i++)
            state[i]=i;
        int i=0,j=0;
        for(i=0;i<256;i++){
            j+=state[i]+key[i%key.length];
            j&=0xFF;
            int t=state[j];
            state[j]=state[i];
            state[i]=t;
        }

        i=0;
        j=0;
        ByteArrayOutputStream output=new ByteArrayOutputStream();
        for(byte ch:data){
            i+=1;
            i&=0xFF;
            j+=state[i];
            j&=0xFF;
            int t=state[j];
            state[j]=state[i];
            state[i]=t;
            int prn=state[(state[i]+state[j])&0xFF];
            output.write(ch^prn);
        }
        try{
            output.flush();
        }catch (IOException e){/*Ignore*/}
        return output.toByteArray();
    }

    //返回正则匹配后的第一组（括号）内的字符串，如果未找到，返回null
    public static String Match1(String text,String regex){
        return Match(text,regex,1);
    }

    public static String Match(String text,String regex,int group){
        Matcher m= Pattern.compile(regex).matcher(text);
        if(m.find())
            return m.group(group);
        return null;
    }

    static class StreamType{
        public String id,container,videoProfile;
        public StreamType(String id,String container,String videoProfile){
            this.id=id;
            this.container=container;
            this.videoProfile=videoProfile;
        }
    }
}

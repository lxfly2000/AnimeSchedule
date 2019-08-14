package com.lxfly2000.acfunget;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class RC4 {
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
}

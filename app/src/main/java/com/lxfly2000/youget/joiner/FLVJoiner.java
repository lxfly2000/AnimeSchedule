package com.lxfly2000.youget.joiner;

import com.chengww.tools.FlvMerge;

import java.io.File;
import java.io.IOException;

public class FLVJoiner extends Joiner {
    @Override
    public int join(String[]inputs,String output){
        FlvMerge flvMerge=new FlvMerge();
        File[]files=new File[inputs.length];
        File outputFile=new File(output);
        for(int i=0;i<inputs.length;i++){
            files[i]=new File(inputs[i]);
        }
        try {
            flvMerge.merge(files,outputFile);
        }catch (IOException e){
            return -1;
        }
        return 0;
    }

    @Override
    public String getExt() {
        return "flv";
    }
}

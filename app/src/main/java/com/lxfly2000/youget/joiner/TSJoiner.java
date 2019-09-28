package com.lxfly2000.youget.joiner;

import java.io.*;

public class TSJoiner extends Joiner {
    @Override
    public int join(String[]inputs,String output){
        //TS文件只需将分段文件按顺序连接就可以了
        try {
            FileOutputStream fos = new FileOutputStream(output);
            byte[] buf =new byte[4096];
            for (String input : inputs) {
                try {
                    FileInputStream fis = new FileInputStream(input);
                    int len=-1;
                    while((len = fis.read(buf))!=-1) {
                        fos.write(buf, 0, len);
                    }
                } catch (IOException e) {
                    return -2;
                }
            }
        }catch (FileNotFoundException e){
            return -1;
        }
        return 0;
    }

    @Override
    public String getExt() {
        return "ts";
    }
}

package com.lxfly2000.youget.joiner;

import androidx.annotation.Nullable;

public abstract class Joiner {
    //将inputs的文件合并输出至output, 不会删除源文件
    public abstract int join(String[]inputs,String output);
    @Nullable public static Joiner AutoChooseJoiner(String[]inputs){
        for (String input : inputs) {
            String[] sp = input.split("\\.");
            if(sp.length>1){
                switch (sp[sp.length-1]){
                    case "flv": case "f4v":return new FLVJoiner();
                    case "mp4":return new MP4Joiner();
                    case "ts":return new TSJoiner();
                }
            }
        }
        return null;
    }

    public abstract String getExt();
}

package com.lxfly2000.utilities;

import java.io.*;

public class FileUtility {
    //读取文件，成功后返回文件文本，出任何错误则返回null
    public static String ReadFile(String path){
        File file=new File(path);
        StringWriter writer=new StringWriter();
        if(file.canRead()){
            try {
                FileReader reader=new FileReader(file);
                int readSingleByte;
                while (true){
                    readSingleByte=reader.read();
                    if(readSingleByte==-1)
                        break;
                    writer.write(readSingleByte);
                }
                reader.close();
            }catch (IOException e){
                return null;
            }
        }
        return writer.toString();
    }

    //保存文本到文件，成功返回true, 失败返回false
    public static boolean WriteFile(String path,String data){
        File file=new File(path);
        try {
            if(!file.exists()){
                File dir=new File(path.substring(0,path.lastIndexOf('/')));
                if(!dir.exists()&&!dir.mkdirs())
                    return false;
                if(!file.createNewFile())
                    return false;
            }
            FileWriter writer=new FileWriter(file);
            writer.write(data);
            writer.close();
        }catch (IOException e){
            return false;
        }
        return true;
    }

    public static boolean IsFileExists(String path){
        File file=new File(path);
        return file.exists();
    }
}

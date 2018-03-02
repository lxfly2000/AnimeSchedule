package com.lxfly2000.animeschedule;

import com.lxfly2000.utilities.FileUtility;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;

public class TestFileUtility {
    @Test
    public void TestReadWrite(){
        String[]texts={"12450\nNext line.\n","114\n514\r\n","今天也是好天气☆",""};
        String[]paths={"file1.txt","file2.txt","file3.txt","file4.txt"};
        String readText;
        File file;
        for(int i=0;i<texts.length;i++){
            Assert.assertEquals(true,FileUtility.WriteFile(paths[i],texts[i]));
            file=new File(paths[i]);
            Assert.assertEquals(true,file.exists());
            System.out.println(paths[i]+" 路径："+file.getAbsolutePath());
            readText=FileUtility.ReadFile(paths[i]);
            Assert.assertEquals(texts[i],readText);
            Assert.assertEquals(true,file.delete());
        }
    }
}

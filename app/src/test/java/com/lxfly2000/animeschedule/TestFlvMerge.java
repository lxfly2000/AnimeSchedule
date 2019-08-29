package com.lxfly2000.animeschedule;

import com.lxfly2000.utilities.FileUtility;
import com.lxfly2000.youget.joiner.FLVJoiner;
import org.junit.Assert;
import org.junit.Test;

public class TestFlvMerge {
    @Test
    public void Merge(){
        String[]inputs={"E:\\gaoboyuan\\lua.flv.bb2api.80\\0.blv",
                "E:\\gaoboyuan\\lua.flv.bb2api.80\\1.blv",
                "E:\\gaoboyuan\\lua.flv.bb2api.80\\2.blv",
                "E:\\gaoboyuan\\lua.flv.bb2api.80\\3.blv",
                "E:\\gaoboyuan\\lua.flv.bb2api.80\\4.blv"};
        for (String input : inputs) {
            if (!FileUtility.IsFileExists(input))
                return;
        }
        Assert.assertEquals(0,new FLVJoiner().join(inputs,"E:\\gaoboyuan\\lua.flv.bb2api.80\\merged.flv"));
    }
}

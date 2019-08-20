package com.lxfly2000.animeschedule;

import com.lxfly2000.utilities.FileUtility;
import com.lxfly2000.youget.joiner.FLVJoiner;
import org.junit.Assert;
import org.junit.Test;

public class TestFlvMerge {
    @Test
    public void Merge(){
        String[]inputs={"D:\\Yueyu\\Bilibili\\download\\s_5800\\98609\\lua.flv.bb2api.80\\0.blv",
                "D:\\Yueyu\\Bilibili\\download\\s_5800\\98609\\lua.flv.bb2api.80\\1.blv",
                "D:\\Yueyu\\Bilibili\\download\\s_5800\\98609\\lua.flv.bb2api.80\\2.blv",
                "D:\\Yueyu\\Bilibili\\download\\s_5800\\98609\\lua.flv.bb2api.80\\3.blv"};
        for (String input : inputs) {
            if (!FileUtility.IsFileExists(input))
                return;
        }
        Assert.assertEquals(0,new FLVJoiner().join(inputs,"TestOutput.flv"));
    }
}

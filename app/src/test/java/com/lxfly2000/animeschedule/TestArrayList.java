package com.lxfly2000.animeschedule;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;

public class TestArrayList {
    @Test
    public void TestArrayListMain(){
        ArrayList<Integer> arrayList=new ArrayList<>();
        arrayList.add(1);
        arrayList.add(1);
        arrayList.add(4);
        arrayList.add(5);
        arrayList.add(1);
        arrayList.add(4);
        Assert.assertEquals(Arrays.asList(1,1,4,5,1,4),arrayList);
        Assert.assertEquals(6,arrayList.size());
        arrayList.clear();
        Assert.assertEquals(0,arrayList.size());
    }
}

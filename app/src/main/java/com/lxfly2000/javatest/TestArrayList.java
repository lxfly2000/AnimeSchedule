package com.lxfly2000.javatest;

import java.util.ArrayList;

public class TestArrayList {
    public static void main(String[] args) {
        ArrayList<Integer> arrayList=new ArrayList<>();
        arrayList.add(1);
        arrayList.add(1);
        arrayList.add(4);
        arrayList.add(5);
        arrayList.add(1);
        arrayList.add(4);
        System.out.print(arrayList);
        System.out.println(arrayList.size());
        arrayList.clear();
        System.out.println(arrayList.size());
    }
}

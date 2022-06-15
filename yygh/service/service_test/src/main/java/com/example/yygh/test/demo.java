package com.example.yygh.test;

import java.util.LinkedList;

public class demo {
    public static void main(String[] args) {
        LinkedList<String> q = new LinkedList<>();
        q.add("1");
        q.add("2");
        q.add("3");
        q.add("4");
        int begin = -1;
        String tmp = null;
        for(int i = 0;i < q.size();i ++){
            if(q.get(i) == "3"){
                begin = i;
                tmp = q.get(i);
            }
        }
        q.remove(begin);
        q.add(0,tmp);
        for(int i = 0;i < q.size();i ++){
            System.out.println(q.get(i));
        }
    }
}

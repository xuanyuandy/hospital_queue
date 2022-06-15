package com.example.yygh.test.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;



public class dateFormatUtil {
//    long time = System.currentTimeMillis();
    //13位毫秒时间戳  -->  yyyy-MM-dd HH:mm:ss
    public static String timeToFormat(long time) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//设置日期格式
        return sdf.format(time);
    }
    //yyyy-MM-dd HH:mm:ss  -->  13位毫秒时间戳
    public static long timeToSecond(String date) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.parse(date).getTime();
    }
}

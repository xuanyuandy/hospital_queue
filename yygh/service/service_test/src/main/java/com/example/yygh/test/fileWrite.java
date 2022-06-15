package com.example.yygh.test;


import java.io.File;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.IOException;
public class fileWrite {
    public static void clear(){
        try{
            File file =new File("test_appendfile.txt");

            if(!file.exists()){
                file.createNewFile();
            }

            //使用true，即进行append file

            FileWriter fileWritter = new FileWriter(file.getName(),false);


            fileWritter.write("");
            fileWritter.flush();
            fileWritter.close();

        }catch(IOException e){

            e.printStackTrace();

        }
    }

    public static void wr(String s){
        try{


            File file =new File("test_appendfile.txt");

            if(!file.exists()){
                file.createNewFile();
            }

            //使用true，即进行append file

            FileWriter fileWritter = new FileWriter(file.getName(),true);


            fileWritter.write(s);

            fileWritter.close();

//            System.out.println("finish");

        }catch(IOException e){

            e.printStackTrace();

        }
    }

    public static void main(String[] args) {
        clear();
        wr("a");
        wr("b\n");
    }
}

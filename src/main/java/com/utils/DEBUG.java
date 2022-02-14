package com.utils;

import java.io.*;


public class DEBUG {

    public static boolean flag = true;
    public static String fileName = "log.txt";
    public static BufferedWriter writer;
    public static FileWriter fileWriter;

    public static void init(){
        try {
            new FileWriter(fileName, false).close();
            fileWriter = new FileWriter(fileName, true);
            writer = new BufferedWriter(new FileWriter(fileName, true));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void log(String ...strings) {
        if (!flag)
            return ;

        for (String string : strings) {
            System.out.print(string + " | ");
        }
            System.out.println();
    }

    public static void loge(String ...strings) {
        if (!flag)
            return ;
        for (String string : strings) {
            System.err.print(string + " | ");
        }
        System.err.println();
    }

    public static void logf(String ...strings) {
        writer = new BufferedWriter(fileWriter);
        if (!flag)
            return ;
        for (String string : strings) {
            try {
                writer.append(string).append(" | ");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            writer.append("\n");
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void switchIOToFile(){
        PrintStream fileOut = null;
        PrintStream fileErr = null;
        try {
            fileOut = new PrintStream("out.txt");
            fileErr = new PrintStream("err.txt");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        System.setOut(fileOut);
        System.setErr(fileErr);
    }

    public static void printError(Exception e, String name){
        DEBUG.loge("error exception : ",name, e.getMessage(), e.getLocalizedMessage());
        e.printStackTrace();
    }
}

package misc.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;

public class DEBUG {

    public static boolean flag = true;

    public static void log(String ...strings) {
        if (!flag)
            return ;

            for (int i = 0; i < strings.length ; i++) {
                System.out.print(strings[i] + " | ");
            }
            System.out.println();
    }

    public static void loge(String ...strings) {
        if (!flag)
            return ;
        for (int i = 0; i < strings.length ; i++) {
            System.err.print(strings[i] + " | ");
        }
        System.err.println();
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

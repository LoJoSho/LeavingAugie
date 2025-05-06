package me.lojosho.leavingaugie.util;

import java.text.SimpleDateFormat;

public class Logging {

    private static final SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static void print(String message) {
        String date = sdfDate.format(System.currentTimeMillis());

        System.out.println("[" + date + "] " + message);
    }

}

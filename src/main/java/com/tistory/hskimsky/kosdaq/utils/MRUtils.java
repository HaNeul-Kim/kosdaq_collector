package com.tistory.hskimsky.kosdaq.utils;

/**
 * @author Haneul, Kim
 */
public class MRUtils {

    public static int getMemoryMB(double gigabytes) {
        return (int) (gigabytes * 1024);
    }

    public static String getMemoryOpts(double gigabytes) {
        return "-Xmx" + (int) (Math.floor(gigabytes * 1024 / 100) * 100) + "m";
    }
}

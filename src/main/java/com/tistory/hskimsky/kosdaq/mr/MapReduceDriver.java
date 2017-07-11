package com.tistory.hskimsky.kosdaq.mr;

import com.tistory.hskimsky.kosdaq.mr.load.KosdaqLoadDriver;
import com.tistory.hskimsky.kosdaq.utils.Constants;
import org.apache.hadoop.util.ProgramDriver;

public class MapReduceDriver {

    public static void main(String[] args) {
        ProgramDriver pgd = new ProgramDriver();
        try {
            pgd.addClass(KosdaqLoadDriver.DRIVER_NAME, KosdaqLoadDriver.class, "KOSDAQ quotes load");
            pgd.driver(args);
            System.exit(Constants.JOB_SUCCESS);
        } catch (Throwable e) {
            e.printStackTrace();
            System.exit(Constants.JOB_FAIL);
        }
    }
}

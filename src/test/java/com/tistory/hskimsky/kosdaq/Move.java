package com.tistory.hskimsky.kosdaq;

import com.tistory.hskimsky.kosdaq.utils.Constants;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileFilter;

/**
 * @author Haneul, Kim
 */
public class Move {

    private File[] dateHomeDirs;

    @Before
    public void setup() {
        this.dateHomeDirs = new File(Constants.KOSDAQ_HOME).listFiles(new FileFilter() {
            @Override
            public boolean accept(File dateDir) {
                return dateDir.isDirectory();
            }
        });
    }

    @Test
    public void move() {
        for (File dateHomeDir : this.dateHomeDirs) {
            File quoteDir = new File(dateHomeDir, "quote");
            File[] codeDirs = quoteDir.listFiles(new FileFilter() {
                @Override
                public boolean accept(File codeDir) {
                    return codeDir.isDirectory();
                }
            });
            for (File codeDir : codeDirs) {
                File[] files = codeDir.listFiles();
                for (File file : files) {
                    String fileName = file.getName();
                    if ("file".equals(fileName)) {
                        fileName = "quotes";
                    }
                    file.renameTo(new File(quoteDir, fileName + "_" + codeDir.getName()));
                }
                codeDir.delete();
            }
            System.out.println(dateHomeDir + " move finish");
        }
    }
}

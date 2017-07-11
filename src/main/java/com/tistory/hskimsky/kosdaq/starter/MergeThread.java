package com.tistory.hskimsky.kosdaq.starter;

import java.io.*;
import java.util.List;
import java.util.zip.GZIPOutputStream;

/**
 * 가져온 주가 데이터를 gzip 파일로 합치는 Thread
 *
 * @author Haneul, Kim
 */
public class MergeThread extends Thread {

    public static final int ALREADY_EXISTS_SUCCESS_FILE = -2;

    public static final int ERROR_CONNECTION = -1;

    private List<File> mergeFiles;

    private File outFile;

    public MergeThread(List<File> mergeFiles, File outFile) throws IOException {
        this.mergeFiles = mergeFiles;
        this.outFile = outFile;
        if (this.outFile.exists()) {
            this.outFile.delete();
        }
    }

    @Override
    public void run() {
        try (OutputStream os = new GZIPOutputStream(new FileOutputStream(this.outFile, true))) {
            BufferedReader br = null;
            String line = "";
            long writeBytes = 0;
            for (File quotesFile : this.mergeFiles) {
                br = new BufferedReader(new FileReader(quotesFile));
                while ((line = br.readLine()) != null) {
                    os.write((line + "\n").getBytes());
                    writeBytes += line.length() + 1;// \n
                }
                if (br != null) {
                    br.close();
                }
            }
            if (os != null) {
                System.out.printf("%s write %d bytes.\n",
                        this.outFile.toString(),
                        writeBytes
                );
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

package com.tistory.hskimsky.kosdaq.starter;

import com.tistory.hskimsky.kosdaq.model.Company;
import com.tistory.hskimsky.kosdaq.model.Quote;
import com.tistory.hskimsky.kosdaq.utils.Constants;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * KOSDAQ 주가를 가져오는 Thread
 *
 * @author Haneul, Kim
 */
public class GetQuoteThread extends Thread {

    public static final int ALREADY_EXISTS_SUCCESS_FILE = -2;

    public static final int ERROR_CONNECTION = -1;

    private int lineNumber;

    private String rawDate;

    private Company company;

    private File quoteOutDir;

    private boolean collectingAll;

    public GetQuoteThread(int lineNumber, String rawDate, Company company, File quoteOutDir, boolean collectingAll) {
        this.lineNumber = lineNumber;
        this.rawDate = rawDate;
        this.company = company;
        this.quoteOutDir = quoteOutDir;
        this.collectingAll = collectingAll;
    }

    @Override
    public void run() {
        try {
            String companyCode = company.getCode();
            File quoteOutSuccessFile = new File(this.quoteOutDir, GetQuote.PREFIX_SUCCESS_FILE + "_" + companyCode);
            if (quoteOutSuccessFile.exists()) {
                System.out.printf("already exists %s. line number = %04d, code = %s, name = %s\n",
                        quoteOutSuccessFile.getName(),
                        lineNumber,
                        companyCode,
                        company.getName()
                );
                return;
            }

            File quotesOutFile = new File(this.quoteOutDir, GetQuote.PREFIX_QUOTES_FILE + "_" + companyCode + ".out");
            if (quotesOutFile.exists()) {
                quotesOutFile.delete();
            }
            quotesOutFile.createNewFile();


            long writeStartTime = System.nanoTime();
            boolean finish = false;
            List<Quote> quotes = null;
            while (!finish) {
                try {
                    quotes = company.getQuotes(this.collectingAll ? null : this.rawDate);
                    finish = true;
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.printf("retry line number = %04d, code = %s, name = %s\n",
                            lineNumber,
                            companyCode,
                            company.getName()
                    );
                }
            }

            OutputStream os = new FileOutputStream(quotesOutFile);
            for (Quote quote : quotes) {
                String outLine = companyCode + Constants.Delimiters.HAT.getDelimiter() + quote.makeString();
                os.write((outLine + "\n").getBytes());
            }
            if (os != null) {
                os.close();
            }

            quoteOutSuccessFile.createNewFile();
            os = new FileOutputStream(quoteOutSuccessFile);
            os.write((quotes.size() + "\n").getBytes());
            if (os != null) {
                os.close();
            }
            long writeEndTime = System.nanoTime();

            System.out.printf("line number = %04d, code = %s, name = %s, write size = %d, write elapsed = %,d ms\n",
                    lineNumber,
                    companyCode,
                    company.getName(),
                    quotes.size(),
                    (writeEndTime - writeStartTime) / 1000000
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

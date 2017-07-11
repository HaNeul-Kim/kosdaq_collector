package com.tistory.hskimsky.kosdaq;

import com.tistory.hskimsky.kosdaq.model.Company;
import com.tistory.hskimsky.kosdaq.model.Quote;
import com.tistory.hskimsky.kosdaq.utils.Constants;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author Haneul, Kim
 */
public class GetQuote {

    private static final String CODE_EXEM = "205100";

    private static final String CODE_SAMSUNG = "005930";

    private File companyListOutFile;
    private File quoteOutFile;

    private List<Company> companies;

    @Before
    public void setup() throws IOException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        // String kosdaqHome = Constants.KOSDAQ_HOME + "/" + sdf.format(new Date());
        String kosdaqHome = Constants.KOSDAQ_HOME + "/20170707";

        this.companyListOutFile = new File(kosdaqHome, "/company_list.out");
        this.quoteOutFile = new File(kosdaqHome, "quote");

        this.companies = new ArrayList<>();
        BufferedReader br = new BufferedReader(new FileReader(this.companyListOutFile));
        String line = "";
        while ((line = br.readLine()) != null) {
            String[] tokens = line.split(String.format("\\%s", Constants.Delimiters.HAT.getDelimiter()), Integer.MAX_VALUE);
            String name = tokens[0];
            String code = tokens[1];
            String type = tokens[2];
            String product = tokens[3];
            String registDate = tokens[4];
            String settlementMonth = tokens[5];
            String ceoName = tokens[6];
            String homepage = tokens[7];
            String location = tokens[8];

            Company company1 = new Company(name, code, type, product, registDate, settlementMonth, ceoName, homepage, location);
            Company company2 = new Company(name, code, type, product, registDate, settlementMonth, ceoName, homepage, location);
            this.companies.add(company2);
        }
        br.close();
    }

    @Test
    public void getLastPageNumberExem() throws IOException {
        int lastPageNumber = this.getLastPageNumber(CODE_EXEM, 1);
        System.out.println("lastPageNumber = " + lastPageNumber);
    }

    private int getLastPageNumber(String code, int page) throws IOException {
        String url = "http://finance.naver.com/item/sise_day.nhn?code=" + code + "&page=" + page;
        Document doc = Jsoup.connect(url).get();
        Element aTag = doc.select("td.pgRR > a").get(0);
        String href = aTag.attr("href");// /item/sise_day.nhn?code=205100&page=66

        String[] tokens = href.split("=", Integer.MAX_VALUE);
        int lastPageNumber = Integer.parseInt(tokens[tokens.length - 1]);
        return lastPageNumber;
    }

    @Test
    public void getQuotes() throws IOException {
        System.out.println("start time = " + new Date());
        long startTime = System.nanoTime();
        String line = "";
        FileOutputStream fos = null;
        int size = companies.size();
        for (int i = 0; i < size; i++) {
            Company company = companies.get(i);
            File quoteOutCodeDir = new File(this.quoteOutFile, company.getCode());
            File quoteOutCodeSuccessFile = new File(quoteOutCodeDir, "_SUCCESS");
            if (quoteOutCodeSuccessFile.exists()) {
                continue;
            }
            if (!quoteOutCodeDir.exists()) {
                quoteOutCodeDir.mkdirs();
            }
            File quoteOutCodeFile = new File(quoteOutCodeDir, "file");
            fos = new FileOutputStream(quoteOutCodeFile);
            long writeStartTime = System.nanoTime();

            List<Quote> quotes = null;
            try {
                quotes = company.getQuotes();
            } catch (Exception e) {
                i--;
                continue;
            }
            for (Quote quote : quotes) {
                String outLine = company.getCode() + Constants.Delimiters.HAT.getDelimiter() + quote.makeString();
                fos.write((outLine + "\n").getBytes());
            }
            fos.close();
            long writeEndTime = System.nanoTime();

            quoteOutCodeSuccessFile.createNewFile();
            fos = new FileOutputStream(quoteOutCodeSuccessFile);
            fos.write((quotes.size() + "\n").getBytes());
            fos.close();

            System.out.printf("name = %s, write elapsed = %,d ms\n", company.getName(), (writeEndTime - writeStartTime) / 1000000);
        }

        long endTime = System.nanoTime();

        System.out.println("elapsed = " + (endTime - startTime) / 1000000);
        System.out.println("end time = " + new Date());
    }

    @Test
    public void getQuotes2() throws IOException {
        System.out.println("start time = " + new Date());
        long startTime = System.nanoTime();
        String line = "";
        FileOutputStream fos = null;
        int size = companies.size();
        for (int i = 0; i < size; i++) {
            Company company2 = companies.get(i);
            File quoteOutCodeDir = new File(this.quoteOutFile, company2.getCode());
            File quoteOutCodeSuccessFile = new File(quoteOutCodeDir, "_SUCCESS");
            if (quoteOutCodeSuccessFile.exists()) {
                System.out.printf("already exists _SUCCESS. line number = %04d, name = %s\n",
                        i + 1,
                        company2.getName()
                );
                continue;
            }
            if (!quoteOutCodeDir.exists()) {
                quoteOutCodeDir.mkdirs();
            }
            File quoteOutCodeFile = new File(quoteOutCodeDir, "file");
            fos = new FileOutputStream(quoteOutCodeFile);
            long writeStartTime = System.nanoTime();

            List<Quote> quotes = null;
            try {
                quotes = company2.getQuotes();
            } catch (Exception e) {
                System.out.printf("retry line number = %04d, name = %s\n",
                        i + 1,
                        company2.getName()
                );
                i--;
                continue;
            }
            for (Quote quote : quotes) {
                String outLine = company2.getCode() + Constants.Delimiters.HAT.getDelimiter() + quote.makeString();
                fos.write((outLine + "\n").getBytes());
            }
            fos.close();
            long writeEndTime = System.nanoTime();

            quoteOutCodeSuccessFile.createNewFile();
            fos = new FileOutputStream(quoteOutCodeSuccessFile);
            fos.write((quotes.size() + "\n").getBytes());
            fos.close();

            System.out.printf("line number = %04d, name = %s, write elapsed = %,d ms\n",
                    i + 1,
                    company2.getName(),
                    (writeEndTime - writeStartTime) / 1000000
            );
        }

        long endTime = System.nanoTime();

        System.out.println("elapsed = " + (endTime - startTime) / 1000000);
        System.out.println("end time = " + new Date());
    }

    @Test
    public void splits() {
        Map<Integer, Long> counters = new HashMap<>();
        for (Company company : this.companies) {
            int dist_key = Integer.parseInt(company.getCode()) % 3;
            counters.put(dist_key, counters.containsKey(dist_key) ? counters.get(dist_key) + 1 : 1);
        }
        System.out.println("all companies size = " + this.companies.size());
        System.out.println("counters           = " + counters);
    }

    @Test
    public void ts() throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        System.out.println(sdf.parse("2014-11-07").getTime());
    }
}

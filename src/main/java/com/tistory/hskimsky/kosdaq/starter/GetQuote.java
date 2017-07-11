package com.tistory.hskimsky.kosdaq.starter;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.tistory.hskimsky.kosdaq.core.AbstractJob;
import com.tistory.hskimsky.kosdaq.model.Company;
import com.tistory.hskimsky.kosdaq.utils.Constants;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * java -cp kosdaq-0.1-SNAPSHOT.jar com.tistory.hskimsky.kosdaq.starter.GetQuote --date ${date}
 *
 * @author Haneul, Kim
 */
public class GetQuote extends AbstractJob {

    public static final int THREADS_NUMBER = 30;

    public static final int FILE_OUTPUT_SPLIT_SIZE = 3;

    public static final String PREFIX_SUCCESS_FILE = "_SUCCESS";

    public static final String PREFIX_QUOTES_FILE = "quotes";

    private Map<String, String> params;

    private boolean collectingAll;

    private String rawDate;

    private File dateHomeDir;

    private File quoteOutDir;

    private List<Company> companies;

    public static void main(String[] args) throws Exception {
        GetQuote job = new GetQuote();
        job.setup(args);
        int result = job.run(args);
        job.cleanup();
        System.exit(result);
    }

    @Override
    protected void setup(String[] args) throws Exception {
        addOption("date", "d", "실행 날짜(yyyyMMdd)", true);
        addOption("all", "a", "전체 수집", "false");
        this.params = parseArguments(args);

        if (params == null || params.size() == 0) {
            throw new IllegalArgumentException("arguments must not null");
        }

        this.collectingAll = Boolean.parseBoolean(this.params.get(keyFor("all")));

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        this.rawDate = this.params.get(keyFor("date"));

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(sdf.parse(this.rawDate));
        if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY || calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
            System.err.println(this.rawDate + " is not collecting date. " + this.rawDate + " is SUNDAY or SATURDAY.");
        }
        String date = sdf.format(sdf.parse(this.rawDate));
        String suffix = this.collectingAll ? "_all" : "";
        this.dateHomeDir = new File(Constants.KOSDAQ_HOME, date + suffix);
        this.quoteOutDir = new File(this.dateHomeDir, "quote_tmp");
        if (!this.quoteOutDir.exists()) {
            this.quoteOutDir.mkdirs();
            System.err.println("mkdirs " + this.quoteOutDir);
        }
        this.companies = new ArrayList<>();

        BufferedReader br = new BufferedReader(new FileReader(new File(this.dateHomeDir, GetCompany.OUTPUT_COMPANY_LIST_FILE_NAME)));
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

            Company company = new Company(name, code, type, product, registDate, settlementMonth, ceoName, homepage, location);
            this.companies.add(company);
        }
        if (br != null) {
            br.close();
        }
    }

    @Override
    protected void cleanup() {

    }

    @Override
    protected int run(String[] args) throws Exception {
        try {
            File jobSuccessFile = new File(this.dateHomeDir, PREFIX_SUCCESS_FILE);
            if (jobSuccessFile.exists()) {
                return Constants.JOB_FAIL;
            }

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSSSS");

            System.out.println("write start time = " + sdf.format(new Date()));
            long writeStartTime = System.nanoTime();
            this.writeQuotes();
            long writeEndTime = System.nanoTime();
            System.out.println("write elapsed  = " + (writeEndTime - writeStartTime) / 1000000 + " ms");
            System.out.println("write end time = " + sdf.format(new Date()));

            File[] successCodeDirs = this.quoteOutDir.listFiles(new FileFilter() {
                @Override
                public boolean accept(File file) {
                    return file.getName().matches(PREFIX_SUCCESS_FILE + "_[0-9]{6}");
                }
            });

            boolean complete = successCodeDirs.length == this.companies.size();
            if (!complete) {
                System.err.println(GetQuote.class.getSimpleName() + " job is not completed." + sdf.format(new Date()));
                return Constants.JOB_FAIL;
            }

            System.out.println("merge start time = " + sdf.format(new Date()));
            long mergeStartTime = System.nanoTime();
            this.mergeQuotesFiles();
            long mergeEndTime = System.nanoTime();
            System.out.println("merge elapsed  = " + (mergeEndTime - mergeStartTime) / 1000000 + " ms");
            System.out.println("merge end time = " + sdf.format(new Date()));
        } catch (Exception e) {
            e.printStackTrace();
            return Constants.JOB_FAIL;
        }

        return Constants.JOB_SUCCESS;
    }

    private void writeQuotes() throws IOException, InterruptedException, ExecutionException {
        ExecutorService executorService = Executors.newFixedThreadPool(THREADS_NUMBER);

        int size = companies.size();
        for (int i = 0; i < size; i++) {
            Company company = companies.get(i);
            String companyCode = company.getCode();
            executorService.execute(new GetQuoteThread(i + 1, this.rawDate, company, this.quoteOutDir, this.collectingAll));
        }

        executorService.shutdown();
        executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
    }

    private void mergeQuotesFiles() throws IOException, InterruptedException {
        File[] quotesFiles = this.quoteOutDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                // quotes_205100.out
                return name.matches("^" + PREFIX_QUOTES_FILE + "_[0-9]{6}\\.out$");
            }
        });

        Multimap<Integer, File> partitionedFiles = ArrayListMultimap.create();
        for (File quotesFile : quotesFiles) {
            String fileName = quotesFile.getName();
            int startIndex = fileName.lastIndexOf("_") + 1;
            int endIndex = fileName.lastIndexOf(".");
            int fileSplitNumber = Integer.parseInt(fileName.substring(startIndex, endIndex)) % FILE_OUTPUT_SPLIT_SIZE;
            partitionedFiles.put(fileSplitNumber, quotesFile);
        }

        ExecutorService executorService = Executors.newFixedThreadPool(FILE_OUTPUT_SPLIT_SIZE);
        for (int splitNumber : partitionedFiles.keySet()) {
            List<File> mergeFiles = (List<File>) partitionedFiles.get(splitNumber);
            File quotesOutFile = new File(this.dateHomeDir, PREFIX_QUOTES_FILE + "_" + splitNumber + ".gz");
            executorService.execute(new MergeThread(mergeFiles, quotesOutFile));
        }

        executorService.shutdown();
        executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);

        FileUtils.forceDelete(this.quoteOutDir);
        File jobSuccessFile = new File(this.dateHomeDir, PREFIX_SUCCESS_FILE);
        jobSuccessFile.createNewFile();
    }
}

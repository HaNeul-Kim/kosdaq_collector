package com.tistory.hskimsky.kosdaq.starter;

import com.tistory.hskimsky.kosdaq.core.AbstractJob;
import com.tistory.hskimsky.kosdaq.model.Company;
import com.tistory.hskimsky.kosdaq.utils.Constants;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * java -cp kosdaq-0.1-SNAPSHOT.jar com.tistory.hskimsky.kosdaq.starter.GetCompany --date ${date}
 *
 * @author Haneul, Kim
 */
public class GetCompany extends AbstractJob {

    public static final String COLLECT_COMPANY_LIST_FILE_NAME = "company_list.html";

    public static final String OUTPUT_COMPANY_LIST_FILE_NAME = "company_list.out";

    private Map<String, String> params;

    private File companyListFile;

    private File companyListOutFile;

    private Document doc;

    public static void main(String[] args) throws Exception {
        GetCompany job = new GetCompany();
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

        boolean collectingAll = Boolean.parseBoolean(this.params.get(keyFor("all")));

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        String rawDate = this.params.get(keyFor("date"));

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(sdf.parse(rawDate));
        if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY || calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
            System.err.println(rawDate + " is not collecting date. " + rawDate + " is SUNDAY or SATURDAY.");
        }
        String suffix = collectingAll ? "_all" : "";

        File dateHomeDir = new File(Constants.KOSDAQ_HOME, rawDate + (collectingAll ? "_all" : ""));
        if (!dateHomeDir.exists()) {
            dateHomeDir.mkdirs();
        }

        this.companyListFile = new File(dateHomeDir, COLLECT_COMPANY_LIST_FILE_NAME);
        this.companyListOutFile = new File(dateHomeDir, OUTPUT_COMPANY_LIST_FILE_NAME);
    }

    @Override
    protected void cleanup() {

    }

    @Override
    protected int run(String[] args) throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSSSS");

        System.out.println("get start time = " + sdf.format(new Date()));
        long getStartTime = System.nanoTime();
        this.getCompanyList();
        long getEndTime = System.nanoTime();
        System.out.println("get elapsed  = " + (getEndTime - getStartTime) / 1000000 + " ms");
        System.out.println("get end time = " + sdf.format(new Date()));

        System.out.println("out start time = " + sdf.format(new Date()));
        long outStartTime = System.nanoTime();
        this.outCompanyList();
        long outEndTime = System.nanoTime();
        System.out.println("out elapsed  = " + (outEndTime - outStartTime) / 1000000 + " ms");
        System.out.println("out end time = " + sdf.format(new Date()));

        boolean complete = this.companyListOutFile.exists() && this.companyListOutFile.length() > 0;
        if (complete) {
            System.out.println("output file path = " + this.companyListOutFile.toString());
        }
        return complete ? Constants.JOB_SUCCESS : Constants.JOB_FAIL;
    }

    private void getCompanyList() throws IOException {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        try {
            HttpPost httppost = new HttpPost("http://kind.krx.co.kr/corpgeneral/corpList.do");

            HttpEntity reqEntity = MultipartEntityBuilder.create()
                    .addPart("currentPageSize", new StringBody("5000", ContentType.TEXT_PLAIN))
                    .addPart("pageIndex", new StringBody("1", ContentType.TEXT_PLAIN))
                    .addPart("method", new StringBody("download", ContentType.TEXT_PLAIN))
                    // .addPart("marketType", new StringBody("kosdaqMkt", ContentType.TEXT_PLAIN))
                    // .addPart("orderMode", new StringBody("3", ContentType.TEXT_PLAIN))
                    // .addPart("orderStat", new StringBody("D", ContentType.TEXT_PLAIN))
                    // .addPart("searchType", new StringBody("13", ContentType.TEXT_PLAIN))
                    // .addPart("fiscalYearEnd", new StringBody("all", ContentType.TEXT_PLAIN))
                    .build();

            httppost.setEntity(reqEntity);

            System.out.println("executing request " + httppost.getRequestLine());
            CloseableHttpResponse response = httpclient.execute(httppost);

            try {
                System.out.println("----------------------------------------");
                System.out.println(response.getStatusLine());
                HttpEntity resEntity = response.getEntity();

                if (this.companyListFile.exists()) {
                    this.companyListFile.delete();
                }
                FileOutputStream fos = new FileOutputStream(this.companyListFile);
                IOUtils.copy(new InputStreamReader(resEntity.getContent(), "euc-kr"), fos, "UTF-8");
                IOUtils.closeQuietly(fos);

                if (resEntity != null) {
                    System.out.println("Response content length: " + resEntity.getContentLength());
                }
                EntityUtils.consume(resEntity);
            } finally {
                response.close();
            }
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } finally {
            httpclient.close();
        }
    }

    private void outCompanyList() throws IOException {
        this.doc = Jsoup.parse(this.companyListFile, "UTF-8");
        Elements columns = doc.select("body table tr");

        List<Company> companies = new ArrayList<>();
        for (Element tr : columns) {
            Elements tds = tr.children();

            if ("td".equals(tds.get(0).nodeName())) {
                String name = tds.get(0).text();
                String code = tds.get(1).text();
                String type = tds.get(2).text();
                String product = tds.get(3).text();
                String registDate = tds.get(4).text();
                String settlementMonth = tds.get(5).text();
                String ceoName = tds.get(6).text();
                String homepage = tds.get(7).text();
                String location = tds.get(8).text();

                Company company = new Company(name, code, type, product, registDate, settlementMonth, ceoName, homepage, location);
                // System.out.println("company = " + company);
                companies.add(company);
            }
        }
        System.out.println("companies.size = " + companies.size());

        if (this.companyListOutFile.exists()) {
            this.companyListOutFile.delete();
        }

        BufferedWriter writer = new BufferedWriter(new FileWriter(this.companyListOutFile));
        for (Company company : companies) {
            writer.write(company.makeString());
            writer.newLine();
        }
        writer.close();
    }
}

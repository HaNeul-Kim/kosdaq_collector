package com.tistory.hskimsky.kosdaq;

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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author Haneul, Kim
 */
public class GetCode {

    private File companyListFile;

    private File companyListOutFile;

    private Document doc;

    @Before
    public void setup() throws IOException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        File kosdaqDailyHomeDir = new File(Constants.KOSDAQ_HOME + "/" + sdf.format(new Date()));
        if (!kosdaqDailyHomeDir.exists()) {
            kosdaqDailyHomeDir.mkdirs();
        }

        this.companyListFile = new File(kosdaqDailyHomeDir, "/company_list.html");
        this.companyListOutFile = new File(kosdaqDailyHomeDir, "/company_list.out");
    }

    @After
    public void cleanup() {
    }

    @Test
    public void getCompanyList() throws IOException {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        try {
            HttpPost httppost = new HttpPost("http://kind.krx.co.kr/corpgeneral/corpList.do");

            HttpEntity reqEntity = MultipartEntityBuilder.create()
                    .addPart("currentPageSize", new StringBody("5000", ContentType.TEXT_PLAIN))
                    .addPart("pageIndex", new StringBody("1", ContentType.TEXT_PLAIN))
                    .addPart("method", new StringBody("download", ContentType.TEXT_PLAIN))
                    .addPart("marketType", new StringBody("kosdaqMkt", ContentType.TEXT_PLAIN))
                    /*.addPart("marketType", new StringBody("kosdaqMkt", ContentType.TEXT_PLAIN))
                    .addPart("orderMode", new StringBody("3", ContentType.TEXT_PLAIN))
                    .addPart("orderStat", new StringBody("D", ContentType.TEXT_PLAIN))
                    .addPart("searchType", new StringBody("13", ContentType.TEXT_PLAIN))
                    .addPart("fiscalYearEnd", new StringBody("all", ContentType.TEXT_PLAIN))*/
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

    @Test
    public void outCompanyList() throws IOException {
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
        System.out.println(companies.size());

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

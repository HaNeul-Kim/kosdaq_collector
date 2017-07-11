package com.tistory.hskimsky.kosdaq.model;

import com.tistory.hskimsky.kosdaq.utils.Constants;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * 회사
 * <p>
 * 회사명<br/>
 * 종목코드<br/>
 * 업종<br/>
 * 주요제품<br/>
 * 상장일<br/>
 * 결산월<br/>
 * 대표자명<br/>
 * 홈페이지<br/>
 * 지역
 *
 * @author Haneul, Kim
 */
public class Company {

    private String name;

    private String code;

    private String type;

    private String product;

    private String registDate;

    private String settlementMonth;

    private String ceoName;

    private String homepage;

    private String location;

    public Company() {

    }

    public Company(String name, String code, String type, String product, String registDate, String settlementMonth, String ceoName, String homepage, String location) {
        this.name = name;
        this.code = code;
        this.type = type;
        this.product = product;
        this.registDate = registDate;
        this.settlementMonth = settlementMonth;
        this.ceoName = ceoName;
        this.homepage = homepage;
        this.location = location;
    }

    public List<Quote> getQuotes() {
        return this.getQuotes(null);
    }

    /**
     * collecting quotes.
     * If rawDate is null then collecting all quotes.
     *
     * @param rawDate or null
     * @return
     */
    public List<Quote> getQuotes(String rawDate) {
        SimpleDateFormat sdf1 = new SimpleDateFormat("yy.MM.dd");
        SimpleDateFormat sdf2 = new SimpleDateFormat("yyyyMMdd");
        List<Quote> quotes = new ArrayList<>();

        String nextPage = "1";
        PAGE_LOOP:
        while (!"".equals(nextPage)) {
            String url = "http://finance.daum.net/item/quote_yyyymmdd_sub.daum?modify=1&code=" + this.code + "&page=" + nextPage;

            Document doc = null;
            try {
                doc = Jsoup.connect(url).get();
                // doc = Jsoup.parse(new File(Constants.FINANCE_HOME + "/daum_day_exem.html"), "UTF-8");
            } catch (IOException e) {
                // e.printStackTrace();
                System.err.printf("%s, retry code = %s, name = %s, url = %s\n", e.getMessage(), this.code, this.name, url);
                continue;
            }

            Elements columns = doc.select("table#bbsList tr");
            for (Element tr : columns) {
                Elements tds = tr.children();

                if (tds.size() == 8 && "td".equals(tds.get(0).nodeName()) && tds.get(0).text().length() == 8) {
                    String date = tds.get(0).text().trim();
                    String start = tds.get(1).text().trim();
                    String high = tds.get(2).text().trim();
                    String low = tds.get(3).text().trim();
                    String finish = tds.get(4).text().trim();
                    String diffQuantity = tds.get(5).text().trim();
                    String diffRate = tds.get(6).text().trim();
                    String quantity = tds.get(7).text().trim();

                    Quote quote = new Quote(date, start, high, low, finish, diffQuantity, diffRate, quantity);
                    // System.out.println(quote);
                    try {
                        if (rawDate == null) {
                            quotes.add(quote);
                        } else if (rawDate.equals(sdf2.format(sdf1.parse(date)))) {
                            quotes.add(quote);
                            break PAGE_LOOP;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            Elements nextSpan = doc.select("div.listPaging table.pagingTable tbody tr td span.on").next();
            nextPage = nextSpan.text().split("~", Integer.MAX_VALUE)[0];// 1 2 3 4 5 6 7 8 9 10 11~20
        }

        return quotes;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public String getRegistDate() {
        return registDate;
    }

    public void setRegistDate(String registDate) {
        this.registDate = registDate;
    }

    public String getSettlementMonth() {
        return settlementMonth;
    }

    public void setSettlementMonth(String settlementMonth) {
        this.settlementMonth = settlementMonth;
    }

    public String getCeoName() {
        return ceoName;
    }

    public void setCeoName(String ceoName) {
        this.ceoName = ceoName;
    }

    public String getHomepage() {
        return homepage;
    }

    public void setHomepage(String homepage) {
        this.homepage = homepage;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    @Override
    public String toString() {
        return "Company{" +
                "name='" + name + '\'' +
                ", code='" + code + '\'' +
                ", type='" + type + '\'' +
                ", product='" + product + '\'' +
                ", registDate='" + registDate + '\'' +
                ", settlementMonth='" + settlementMonth + '\'' +
                ", ceoName='" + ceoName + '\'' +
                ", homepage='" + homepage + '\'' +
                ", location='" + location + '\'' +
                '}';
    }

    public String makeString() {
        List<String> columns = new ArrayList<>();

        columns.add(this.name);
        columns.add(this.code);
        columns.add(this.type);
        columns.add(this.product);
        columns.add(this.registDate);
        columns.add(this.settlementMonth);
        columns.add(this.ceoName);
        columns.add(this.homepage);
        columns.add(this.location);

        return StringUtils.join(columns, Constants.Delimiters.HAT.getDelimiter());
    }
}

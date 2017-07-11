package com.tistory.hskimsky.kosdaq.model;

import com.tistory.hskimsky.kosdaq.utils.Constants;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 시세
 * <p>
 * 일자<br/>
 * 시가<br/>
 * 고가<br/>
 * 저가<br/>
 * 종가<br/>
 * 전일비<br/>
 * 등락률<br/>
 * 거래량
 *
 * @author Haneul, Kim
 */
public class Quote {

    private String date;

    private String start;

    private String high;

    private String low;

    private String finish;

    private String diffQuantity;

    private String diffRate;

    private String quantity;

    public Quote() {
    }

    public Quote(String date, String start, String high, String low, String finish, String diffQuantity, String diffRate, String quantity) {
        this.date = date;
        this.start = start;
        this.high = high;
        this.low = low;
        this.finish = finish;
        this.diffQuantity = diffQuantity;
        this.diffRate = diffRate;
        this.quantity = quantity;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getStart() {
        return start;
    }

    public void setStart(String start) {
        this.start = start;
    }

    public String getHigh() {
        return high;
    }

    public void setHigh(String high) {
        this.high = high;
    }

    public String getLow() {
        return low;
    }

    public void setLow(String low) {
        this.low = low;
    }

    public String getFinish() {
        return finish;
    }

    public void setFinish(String finish) {
        this.finish = finish;
    }

    public String getDiffQuantity() {
        return diffQuantity;
    }

    public void setDiffQuantity(String diffQuantity) {
        this.diffQuantity = diffQuantity;
    }

    public String getDiffRate() {
        return diffRate;
    }

    public void setDiffRate(String diffRate) {
        this.diffRate = diffRate;
    }

    public String getQuantity() {
        return quantity;
    }

    public void setQuantity(String quantity) {
        this.quantity = quantity;
    }

    @Override
    public String toString() {
        return "Quote2{" +
                "date='" + date + '\'' +
                ", start='" + start + '\'' +
                ", high='" + high + '\'' +
                ", low='" + low + '\'' +
                ", finish='" + finish + '\'' +
                ", diffQuantity='" + diffQuantity + '\'' +
                ", diffRate='" + diffRate + '\'' +
                ", quantity='" + quantity + '\'' +
                '}';
    }

    public String makeString() {
        List<String> columns = new ArrayList<>();

        columns.add(this.date);
        columns.add(this.start);
        columns.add(this.high);
        columns.add(this.low);
        columns.add(this.finish);
        columns.add(this.diffQuantity);
        columns.add(this.diffRate);
        columns.add(this.quantity);

        return StringUtils.join(columns, Constants.Delimiters.HAT.getDelimiter());
    }
}

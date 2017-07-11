package com.tistory.hskimsky.kosdaq.mr.load;

import com.tistory.hskimsky.kosdaq.model.Schema;
import com.tistory.hskimsky.kosdaq.utils.Constants;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * KOSDAQ 주가를 hbase table 에 BulkLoad 하는 MapReduce Mapper
 *
 * @author Haneul, Kim
 */
public class KosdaqLoadMapper extends Mapper<LongWritable, Text, ImmutableBytesWritable, Put> {

    private static final String COLUMN_FAMILY_QUOTE = "quote";

    private String inputSeparator;

    private String outputSeparator;

    private SimpleDateFormat inSdf;

    private SimpleDateFormat outSdf;

    @Override
    protected void setup(Context context) throws IOException, InterruptedException {
        this.inputSeparator = Constants.Delimiters.HAT.getDelimiter();
        this.outputSeparator = Constants.Delimiters.HAT.getDelimiter();

        this.inSdf = new SimpleDateFormat("yy.MM.dd");
        this.outSdf = new SimpleDateFormat("yyyyMMdd");
    }

    @Override
    protected void cleanup(Context context) throws IOException, InterruptedException {
        context.getCounter(Constants.Counters.GROUP_NAME.name(), Constants.Counters.MAP_READ_FILE_COUNTER_NAME.name()).increment(1);
    }

    private String toYYYYMMDD(String date) throws ParseException {
        return this.outSdf.format(this.inSdf.parse(date));
    }

    @Override
    protected void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
        context.getCounter(Constants.Counters.GROUP_NAME.name(), Constants.Counters.MAP_INPUT_RECORDS_COUNTER_NAME.name()).increment(1);
        try {
            String[] tokens = value.toString().split(String.format("\\%s", this.inputSeparator), Integer.MAX_VALUE);

            String code = tokens[Schema.Quote.code.ordinal()];
            int dist_key = Integer.parseInt(code) % 3;
            ImmutableBytesWritable rowKey = new ImmutableBytesWritable(Bytes.toBytes(dist_key + this.outputSeparator + code));

            Put put = new Put(rowKey.get());
            Date date = this.inSdf.parse(tokens[Schema.Quote.date.ordinal()]);
            StringBuilder map_out_val = new StringBuilder();
            map_out_val.append(tokens[Schema.Quote.start.ordinal()].replaceAll(",", "")).append(this.outputSeparator);
            map_out_val.append(tokens[Schema.Quote.high.ordinal()].replaceAll(",", "")).append(this.outputSeparator);
            map_out_val.append(tokens[Schema.Quote.low.ordinal()].replaceAll(",", "")).append(this.outputSeparator);
            map_out_val.append(tokens[Schema.Quote.finish.ordinal()].replaceAll(",", "")).append(this.outputSeparator);
            map_out_val.append(tokens[Schema.Quote.quantity.ordinal()].replaceAll(",", ""));

            // put 'finance', '2^205100', 'quote:20170703', '4880^4935^4785^4855^412321', 1499007600000
            // [code%region number]^[code]
            // quote:[date]
            // [start]^[high]^[low]^[finish]^[quantity]
            put.addColumn(Bytes.toBytes(COLUMN_FAMILY_QUOTE), Bytes.toBytes(this.outSdf.format(date)), date.getTime(), Bytes.toBytes(map_out_val.toString()));
            context.write(rowKey, put);
            context.getCounter(Constants.Counters.GROUP_NAME.name(), Constants.Counters.MAP_OUT_COUNTER_NAME.name()).increment(1);
        } catch (ParseException e) {
            new RuntimeException("map parse error!! value = " + value.toString());
        }
    }
}
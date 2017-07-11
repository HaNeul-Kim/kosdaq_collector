package com.tistory.hskimsky.kosdaq.mr.load;

import com.tistory.hskimsky.kosdaq.utils.Constants;
import com.tistory.hskimsky.kosdaq.utils.MRUtils;
import nl.basjes.hadoop.io.compress.SplittableGzipCodec;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.HFileOutputFormat2;
import org.apache.hadoop.hbase.mapreduce.LoadIncrementalHFiles;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.io.compress.*;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.MRJobConfig;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

/**
 * KOSDAQ 주가를 hbase table 에 BulkLoad 하는 MapReduce Driver
 *
 * @author Haneul, Kim
 */
public class KosdaqLoadDriver extends Configured implements Tool {

    public static final String DRIVER_NAME = "kosdaqload";

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            printUsage();
            System.exit(Constants.JOB_FAIL);
        }
        System.exit(ToolRunner.run(new KosdaqLoadDriver(), args));
    }

    private static void printUsage() {
        System.out.println("yarn jar kosdaq-0.1-SNAPSHOT.jar " + KosdaqLoadDriver.class.getName() + " <rawDate> [<tableName>]");
    }

    @Override
    public int run(String[] args) throws Exception {
        String rawDate = args[0];
        String tableName = args.length > 1 ? args[1] : "finance";

        Path inputPath = new Path("hdfs://nn/data/finance/" + rawDate + "/quotes*.gz");
        Path outputPath = new Path("hdfs://nn/user/flamingo/mr/output/kosdaq/" + rawDate);

        Configuration conf = this.newConf();
        // hbase conf
        conf.set("hbase.table.name", tableName);
        conf.set("hbase.master", "master2.hdp.local");
        conf.set("hbase.zookeeper.quorum", "master1.hdp.local,master2.hdp.local,admin.hdp.local");
        conf.set("zookeeper.znode.parent", "/hbase-unsecure");
        HBaseConfiguration.addHbaseResources(conf);

        FileSystem fs = outputPath.getFileSystem(conf);
        if (fs.exists(outputPath)) {
            System.out.println("try delete output path " + outputPath);
            fs.delete(outputPath, true);
            System.out.println("output path " + outputPath + " deleted.");
        }

        Job job = Job.getInstance(conf, KosdaqLoadDriver.class.getSimpleName() + " " + rawDate);
        job.setJarByClass(KosdaqLoadDriver.class);

        job.setInputFormatClass(TextInputFormat.class);
        job.setOutputFormatClass(TextOutputFormat.class);
        TextInputFormat.addInputPath(job, inputPath);

        // mapper
        job.setMapperClass(KosdaqLoadMapper.class);
        job.setMapOutputKeyClass(ImmutableBytesWritable.class);
        job.setMapOutputValueClass(Put.class);

        Connection hbCon = ConnectionFactory.createConnection(conf);
        Table table = hbCon.getTable(TableName.valueOf(tableName));
        RegionLocator regionLocator = hbCon.getRegionLocator(TableName.valueOf(tableName));
        Admin admin = hbCon.getAdmin();

        HFileOutputFormat2.configureIncrementalLoad(job, table, regionLocator);
        HFileOutputFormat2.setOutputPath(job, outputPath);

        boolean completion = job.waitForCompletion(true);

        if (completion) {
            LoadIncrementalHFiles loadIncrementalHFiles = new LoadIncrementalHFiles(conf);

            System.out.println("Starting bulk load...");
            loadIncrementalHFiles.doBulkLoad(outputPath, admin, table, regionLocator);
            System.out.println("Finished bulk load.");
        }

        System.out.println("output path = " + outputPath);

        return completion ? Constants.JOB_SUCCESS : Constants.JOB_FAIL;
    }

    private Configuration newConf() {
        Configuration conf = new Configuration();

        conf.set("fs.defaultFS", "hdfs://nn");
        conf.set("fs.hdfs.impl", DistributedFileSystem.class.getName());
        conf.set("mapreduce.job.reduce.slowstart.completedmaps", "0.9");

        // conf.setInt("io.file.buffer.size", 4 * 1024);
        conf.setStrings("io.compression.codecs",
                DefaultCodec.class.getName(),
                SplittableGzipCodec.class.getName(),
                BZip2Codec.class.getName(),
                DeflateCodec.class.getName(),
                SnappyCodec.class.getName(),
                Lz4Codec.class.getName()
        );

        conf.set(TextOutputFormat.SEPERATOR, Constants.Delimiters.HAT.getDelimiter());

        double mapMemory = .5;
        double redMemory = .5;
        conf.setInt(MRJobConfig.MAP_MEMORY_MB, MRUtils.getMemoryMB(mapMemory));
        conf.set(MRJobConfig.MAP_JAVA_OPTS, MRUtils.getMemoryOpts(mapMemory));
        conf.setInt(MRJobConfig.REDUCE_MEMORY_MB, MRUtils.getMemoryMB(redMemory));
        conf.set(MRJobConfig.REDUCE_JAVA_OPTS, MRUtils.getMemoryOpts(redMemory));

        return conf;
    }
}

package com.tistory.hskimsky.kosdaq.utils;

/**
 * 상수 모음
 *
 * @author Haneul, Kim
 */
public class Constants {

    public static final int JOB_SUCCESS = 0;

    public static final int JOB_FAIL = 1;

    public static final String KOSDAQ_HOME = System.getProperty("user.home") + "/Downloads/kosdaq";

    public enum Delimiters {
        COMMA(","),
        HAT("^"),
        PIPE("|");

        /**
         * 컬럼을 구분하는 delimiter
         */
        private String delimiter;

        /**
         * Delimiter를 설정한다.
         *
         * @param delimiter delimiter
         */
        Delimiters(String delimiter) {
            this.delimiter = delimiter;
        }

        /**
         * Delimiter를 반환한다.
         *
         * @return delimiter
         */
        public String getDelimiter() {
            return delimiter;
        }
    }

    public enum Counters {
        GROUP_NAME,

        MAP_INPUT_RECORDS_COUNTER_NAME,
        MAP_OUT_COUNTER_NAME,
        MAP_READ_FILE_COUNTER_NAME,
        REDUCE_INPUT_GROUPS_COUNTER_NAME,
        REDUCE_INPUT_RECORDS_COUNTER_NAME,
        REDUCE_OUT_COUNTER_NAME,
        REDUCE_READ_FILE_COUNTER_NAME
    }
}

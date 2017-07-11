package com.tistory.hskimsky.kosdaq.core;

import org.apache.commons.cli.Options;
import org.apache.commons.cli2.CommandLine;
import org.apache.commons.cli2.Group;
import org.apache.commons.cli2.Option;
import org.apache.commons.cli2.OptionException;
import org.apache.commons.cli2.builder.ArgumentBuilder;
import org.apache.commons.cli2.builder.DefaultOptionBuilder;
import org.apache.commons.cli2.builder.GroupBuilder;
import org.apache.commons.cli2.commandline.Parser;
import org.apache.commons.cli2.util.HelpFormatter;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

/**
 * @author Haneul Kim
 * @version 0.1
 */
public abstract class AbstractJob {

    /**
     * Job을 동작시키기 위한 파라미터의 Key Value Map.
     */
    private static Map<String, String> argMap;

    /**
     * 내부적으로 사용하기 위한 옵션 목록.
     */
    private static List<Option> options = new LinkedList<Option>();

    /**
     * 기본 생성자.
     */
    protected AbstractJob() {
    }

    protected void setup(String[] args) throws Exception {

    }

    protected void cleanup() {

    }

    protected abstract int run(String[] args) throws Exception;

    /**
     * Hadoop MapReduce에 옵션을 추가한다. 옵션 추가는 커맨드 라인을 통해서 가능하며
     * 커맨드 라인은 {@link #parseArguments(String[])} 메소드를 호출하여 파싱하게 된다.
     *
     * @param name        파라미터명(예; <tt>inputPath</tt>)
     * @param shortName   출약 파라미터명(예; <tt>i</tt>)
     * @param description 파라미터에 대한 설명문
     * @param required    이 값이 <tt>true</tt>인 경우 커맨드 라인에서 입력 파라미터를 지정하지 않는 경우
     *                    예외를 발생시킨다. 사용법과 에러 메시지를 포함한 예외를 던진다.
     */
    protected static void addOption(String name, String shortName, String description, boolean required) {
        options.add(buildOption(name, shortName, description, true, required, null));
    }

    /**
     * Hadoop MapReduce에 옵션을 추가한다. 옵션 추가는 커맨드 라인을 통해서 가능하며
     * 커맨드 라인은 {@link #parseArguments(String[])} 메소드를 호출하여 파싱하게 된다.
     *
     * @param name         파라미터명(예; <tt>inputPath</tt>)
     * @param shortName    출약 파라미터명(예; <tt>i</tt>)
     * @param description  파라미터에 대한 설명문
     * @param defaultValue 커맨드 라인에서 입력 인자를 지정하지 않는 경우 설정할 기본값으로써 null을 허용한다.
     */
    protected static void addOption(String name, String shortName, String description, String defaultValue) {
        options.add(buildOption(name, shortName, description, true, false, defaultValue));
    }

    /**
     * Hadoop MapReduce에 옵션을 추가한다. 옵션 추가는 커맨드 라인을 통해서 가능하며
     * 커맨드 라인은 {@link #parseArguments(String[])} 메소드를 호출하여 파싱하게 된다.
     * 만약에 옵션이 인자를 가지고 있지 않다면 {@code parseArguments} 메소드 호출을 통해 리턴한
     * map의 {@code containsKey} 메소드를 통해서 옵션의 존재 여부를 확인하게 된다.
     * 그렇지 않은 경우 옵션의 영문 옵션명 앞에 '--'을 붙여서 map에서 해당 키가 존재하는지 확인한 후
     * 존재하는 경우 해당 옵션의 문자열값을 사용한다.
     *
     * @param option 추가할 옵션
     * @return 추가한 옵션
     */
    protected static Option addOption(Option option) {
        options.add(option);
        return option;
    }

    /**
     * 지정한 파라미터를 가진 옵션을 구성한다. 이름과 설명은 필수로 입력해야 한다.
     * required.
     *
     * @param name         커맨드 라인에서 '--'을 prefix로 가진 옵션의 이름
     * @param shortName    커맨드 라인에서 '--'을 prefix로 가진 옵션의 짧은 이름
     * @param description  도움말에 출력할 옵션에 대한 상세 설명
     * @param hasArg       인자를 가진다면 <tt>true</tt>
     * @param required     필수 옵션이라면 <tt>true</tt>
     * @param defaultValue 인자의 기본값. <tt>null</tt>을 허용한다.
     * @return 옵션
     */
    public static Option buildOption(String name,
                                     String shortName,
                                     String description,
                                     boolean hasArg,
                                     boolean required,
                                     String defaultValue) {

        DefaultOptionBuilder optBuilder = new DefaultOptionBuilder().withLongName(name).withDescription(description)
                .withRequired(required);

        if (shortName != null) {
            optBuilder.withShortName(shortName);
        }

        if (hasArg) {
            ArgumentBuilder argBuilder = new ArgumentBuilder().withName(name).withMinimum(1).withMaximum(1);

            if (defaultValue != null) {
                argBuilder = argBuilder.withDefault(defaultValue);
            }

            optBuilder.withArgument(argBuilder.create());
        }

        return optBuilder.create();
    }

    /**
     * 사용자가 입력한 커맨드 라인을 파싱한다.
     * 만약에 <tt>-h</tt>를 지정하거나 예외가 발생하는 경우 도움말을 출력하고 <tt>null</tt>을 반환한다.
     *
     * @param args 커맨드 라인 옵션
     * @return 인자와 인자에 대한 값을 포함하는 {@code Map<String,String>}.
     * 인자의 key는 옵션명에 되며 옵션명은 '--'을 prefix로 갖는다.
     * 따라서 옵션을 기준으로 {@code Map<String,String>} 에서 찾고자 하는 경우 반드시 옵션명에 '--'을 붙이도록 한다.
     */
    public static Map<String, String> parseArguments(String[] args) throws Exception {
        Option helpOpt = addOption(new DefaultOptionBuilder().withLongName("help").withDescription("도움말을 출력합니다.").withShortName("h").create());

        GroupBuilder groupBuilder = new GroupBuilder().withName("Job 옵션:");

        for (Option opt : options) {
            groupBuilder = groupBuilder.withOption(opt);
        }

        Group group = groupBuilder.create();

        CommandLine cmdLine;
        try {
            Parser parser = new Parser();
            parser.setGroup(group);
            parser.setHelpOption(helpOpt);
            cmdLine = parser.parse(args);
        } catch (OptionException e) {
            System.err.println(e.getMessage());
            printHelpWithGenericOptions(group, e);
            return null;
        }

        if (cmdLine.hasOption(helpOpt)) {
            printHelpWithGenericOptions(group);
            return null;
        }

        argMap = new TreeMap<>();
        maybePut(argMap, cmdLine, options.toArray(new Option[options.size()]));
        System.out.println("Command line arguments: ");
        Set<String> keySet = argMap.keySet();
        for (Iterator<String> iterator = keySet.iterator(); iterator.hasNext(); ) {
            String key = iterator.next();
            System.out.println("   " + key + " = " + argMap.get(key));
        }
        return argMap;
    }

    /**
     * 지정한 옵션명에 대해서 옵션 키를 구성한다. 예를 들여 옵션명이 <tt>name</tt> 이라면 실제 옵션 키는 <tt>--name</tt>이 된다.
     *
     * @param optionName 옵션명
     */
    public static String keyFor(String optionName) {
        return "--" + optionName;
    }

    protected static void maybePut(Map<String, String> args, CommandLine cmdLine, Option... opt) {
        for (Option o : opt) {

            // 커맨드 라인에 옵션이 있거나 기본값과 같은 값이 있는 경우
            if (cmdLine.hasOption(o) || cmdLine.getValue(o) != null) {

                // 커맨드 라인의 옵션에 값이 OK
                // nulls are ok, for cases where options are simple flags.
                Object vo = cmdLine.getValue(o);
                String value = vo == null ? null : vo.toString();
                args.put(o.getPreferredName(), value);
            }
        }
    }

    /**
     * 커맨드 라인의 사용법을 구성한다.
     *
     * @param group Option Group
     * @param oe    예외
     * @throws IOException
     */
    public static void printHelpWithGenericOptions(Group group, OptionException oe) throws IOException {
        Options ops = new Options();
        org.apache.commons.cli.HelpFormatter fmt = new org.apache.commons.cli.HelpFormatter();
        fmt.printHelp("<command> [Generic Options] [Job-Specific Options]", "Generic Options:", ops, "");
        PrintWriter pw = new PrintWriter(System.out, true);
        HelpFormatter formatter = new HelpFormatter();
        formatter.setGroup(group);
        formatter.setPrintWriter(pw);
        formatter.setException(oe);
        formatter.print();
        pw.flush();
    }

    public static void printHelpWithGenericOptions(Group group) throws IOException {
        Options ops = new Options();
        org.apache.commons.cli.HelpFormatter fmt = new org.apache.commons.cli.HelpFormatter();
        fmt.printHelp("<command> [Generic Options] [Job-Specific Options]", "Generic Options:", ops, "");

        PrintWriter pw = new PrintWriter(System.out, true);
        HelpFormatter formatter = new HelpFormatter();
        formatter.setGroup(group);
        formatter.setPrintWriter(pw);
        formatter.printHelp();
        formatter.setFooter("Job을 실행하는데 필요한 디렉토리를 지정하십시오.");
        formatter.printFooter();

        pw.flush();
    }
}

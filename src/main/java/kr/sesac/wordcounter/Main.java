package kr.sesac.wordcounter;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * 첫 실행용 코드입니다. TXT 내용을 읽은 뒤 TODO를 채우며 기능을 추가하세요.
 * 구현 기준은 docs/requirements.md에 있습니다.
 */
public class Main {
    /*public static void main(String[] args) throws IOException {
        Path input = Path.of("samples/equivalent/basic.txt");

        System.out.println("문서 단어 분석기 - 시작 코드");
        System.out.println("입력 파일: " + input);
        System.out.println();

        // TODO 1: 단어별 출현 횟수를 저장할 자료구조를 준비하세요. (요구사항 4. 단어별 횟수 집계)
        Map<String, Long> wordCount = new HashMap<>();

        try (BufferedReader reader =
                     Files.newBufferedReader(input, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                //System.out.println(line);
                // TODO 2: line을 요구사항 3. 단어 처리 규칙대로 단어(토큰)로 나누세요.
                // TODO 3: 숫자만 있는 단어는 제외하고 단어별 횟수를 늘리세요.
                Pattern tokenPattern = Pattern.compile("[a-zA-Z0-9가-힣ㄱ-ㅎㅏ-ㅣ]+");
                Matcher matcher = tokenPattern.matcher(line);
                while (matcher.find()) {
                    String token = matcher.group();
                    token = token.toLowerCase(Locale.ROOT);
                    if (token.matches("[0-9]+")) {
                        continue;
                    }

                    long current = wordCount.getOrDefault(token, 0L);
                    wordCount.put(token, current + 1);
                }
            }
        }

        //System.out.println();
        //System.out.println("파일 읽기 성공. 다음 단계는 단어 분리와 카운팅입니다.");
        //System.out.println("구현 후 전체 9개·6종인지 expected/basic-counts.tsv와 비교하세요.");
        // TODO 4: 원문 출력 대신 집계 결과를 출력하세요.
        // TXT 카운팅 완성 후 다른 형식, 메뉴, 오류 처리, 저장을 추가하세요.
        for (Map.Entry<String, Long> entry : wordCount.entrySet()) {
            System.out.println(entry.getKey() + " : " + entry.getValue());
        }
    }*/

    public static final Pattern tokenPattern = Pattern.compile("[a-zA-Z0-9가-힣ㄱ-ㅎㅏ-ㅣ]+");

    public static void main(String[] args) throws IOException {
        Path input = Path.of("samples/equivalent/basic.html");

        System.out.println("문서 단어 분석기 - 시작 코드");
        System.out.println("입력 파일: " + input);
        System.out.println();

        //Scanner scanner = new Scanner(System.in);
        Map<String, Long> wordCount = new HashMap<>();

        //processTxt(input, wordCount);
        //processTsv(input, wordCount);
        processHtml(input, wordCount);


        for (Map.Entry<String, Long> entry : wordCount.entrySet()) {
            System.out.println(entry.getKey() + " : " + entry.getValue());
        }
    }

    private static void processTxt(Path input, Map<String, Long> wordCount) throws IOException{
        try (BufferedReader reader =
                     Files.newBufferedReader(input, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                countWords(line, wordCount);
            }
        }
    }

    private static void countWords(String line, Map<String, Long> wordCount){
        Matcher matcher = tokenPattern.matcher(line);
        while (matcher.find()) {
            String token = matcher.group();
            token = token.toLowerCase(Locale.ROOT);
            if (token.matches("[0-9]+")) {
                continue;
            }

            long current = wordCount.getOrDefault(token, 0L);
            wordCount.put(token, current + 1);
        }
    }

    private static void processCsv(Path input, Map<String, Long> wordCount) throws IOException {
        var format = CSVFormat.RFC4180.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .get();

        try (var reader = Files.newBufferedReader(input, StandardCharsets.UTF_8);
             CSVParser parser = format.parse(reader)) {
            for (CSVRecord record : parser) {
                String cell = record.get("text");
                if (!cell.isBlank()){
                    countWords(cell, wordCount);
                }
            }
        }
    }

    private static void processTsv(Path input, Map<String, Long> wordCount) throws IOException {
        var format = CSVFormat.RFC4180.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setDelimiter('\t')
                .setQuote(null)
                .get();

        try (var reader = Files.newBufferedReader(input, StandardCharsets.UTF_8);
             CSVParser parser = format.parse(reader)) {
            for (CSVRecord record : parser) {
                String cell = record.get("document");
                if (!cell.isBlank()){
                    countWords(cell, wordCount);
                }
            }
        }
    }
    private static void processHtml(Path input, Map<String, Long> wordCount) throws IOException {
        Document document = Jsoup.parse(input.toFile(), "UTF-8");
        Elements matches = document.select("#content");

        if (matches.size() != 1) {
            throw new IOException("본문 요소는 정확히 하나여야 합니다: " + input);
        }

        Element content = matches.first();
        content.select("script, style, nav, header, footer").remove();
        countWords(content.text(), wordCount);
    }
}

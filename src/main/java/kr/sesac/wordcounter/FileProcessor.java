package kr.sesac.wordcounter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class FileProcessor {
    public static boolean isSupported(Path path) {
        String name = path.toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".txt") || name.endsWith(".csv") || name.endsWith(".tsv") || name.endsWith(".html") || name.endsWith(".htm");
    }

    public static boolean processFile(Path file, Map<String, Long> total) {
        Map<String, Long> fileCount = new HashMap<>();
        String name = file.toString().toLowerCase(Locale.ROOT);
        try {
            if (name.endsWith(".txt")) processTxt(file, fileCount);
            else if (name.endsWith(".csv")) processCsv(file, fileCount);
            else if (name.endsWith(".tsv")) processTsv(file, fileCount);
            else processHtml(file, fileCount);
        } catch (IOException | UncheckedIOException | IllegalArgumentException e) {
            System.out.println("실패: " + file + " (" + e.getMessage() + ")");
            return false;
        }

        for (Map.Entry<String, Long> e : fileCount.entrySet()) {
            total.merge(e.getKey(), e.getValue(), Long::sum);
        }
        return true;
    }

    private static void processTxt(Path input, Map<String, Long> wordCount) throws IOException{
        try (BufferedReader reader =
                     Files.newBufferedReader(input, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                WordCounter.countWords(line, wordCount);
            }
        }
    }

    /*
     * 문제점 : CSV와 TSV의 파일 처리 코드가 중복되어 있음.
     * 원인 : 구분자와 분석 대상 열만 다른데도
     *        각각 별도의 처리 로직을 구현함.
     * 수정자 : 원대호
     */
    private static void processCsv(
            Path input,
            Map<String, Long> wordCount
    ) throws IOException {

        processDelimitedFile(
                input, wordCount, ',', List.of("text"), true
        );
    }

    private static void processTsv(
            Path input,
            Map<String, Long> wordCount
    ) throws IOException {

        processDelimitedFile(
                input, wordCount, '\t', List.of("document"), false
        );
    }

    /*
     * 문제점 : 공통 처리 로직이 없어 중복 코드가 발생하고, 필수 열이 없는 파일의 검증도 부족함.
     * 원인 : 파일 형식별로 처리 로직을 따로 구현하고, 데이터 순회 전에 헤더를 검증하지 않음.
     * 수정자 : 원대호
     */
    private static void processDelimitedFile(
            Path input,
            Map<String, Long> wordCount,
            char delimiter,
            List<String> columns,
            boolean useQuotes
    ) throws IOException {

        var builder = CSVFormat.RFC4180.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setDelimiter(delimiter);

        if (!useQuotes) {
            builder.setQuote(null);
        }

        var format = builder.get();

        try (var reader = Files.newBufferedReader(
                input, StandardCharsets.UTF_8);
             CSVParser parser = format.parse(reader)) {

            Map<String, Integer> headerMap = new HashMap<>();

            for (var entry : parser.getHeaderMap().entrySet()) {
                headerMap.put(
                        entry.getKey().trim(),
                        entry.getValue()
                );
            }

            // 필수 열이 모두 존재하는지 확인
            for (String column : columns) {
                if (!headerMap.containsKey(column)) {
                    throw new IOException(
                            "필수 열이 없습니다: " + column
                    );
                }
            }

            // 데이터 처리
            for (CSVRecord record : parser) {

                if (!record.isConsistent()) {
                    throw new IOException(
                            "레코드의 셀 수가 헤더와 다릅니다"
                    );
                }

                for (String column : columns) {
                    String cell = record.get(
                            headerMap.get(column)
                    );

                    if (!cell.isBlank()) {
                        WordCounter.countWords(
                                cell, wordCount
                        );
                    }
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
        WordCounter.countWords(content.text(), wordCount);
    }
}

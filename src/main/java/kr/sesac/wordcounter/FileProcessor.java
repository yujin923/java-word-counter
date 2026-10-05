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

    private static void processCsv(Path input, Map<String, Long> wordCount) throws IOException {
        var format = CSVFormat.RFC4180.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .get();

        try (var reader = Files.newBufferedReader(input, StandardCharsets.UTF_8);
             CSVParser parser = format.parse(reader)) {
            for (CSVRecord record : parser) {
                if (!record.isConsistent()) { //레코드 셀 수가 헤더와 같은지 알려주는 Commons CSV 메서드!!
                    throw new IOException("레코드의 셀 수가 헤더와 다릅니다");
                }
                String cell = record.get("text");
                if (!cell.isBlank()){
                    WordCounter.countWords(cell, wordCount);
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
                if (!record.isConsistent()) {
                    throw new IOException("레코드의 셀 수가 헤더와 다릅니다");
                }
                String cell = record.get("document");
                if (!cell.isBlank()){
                    WordCounter.countWords(cell, wordCount);
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

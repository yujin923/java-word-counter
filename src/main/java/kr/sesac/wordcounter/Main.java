package kr.sesac.wordcounter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
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
        //Path input = Path.of("samples/equivalent/basic.html");

        //System.out.println("문서 단어 분석기 - 시작 코드");
        //System.out.println("입력 파일: " + input);
        //System.out.println();

        Scanner scanner = new Scanner(System.in);
        Map<String, Long> wordCount = new HashMap<>();

        //processTxt(input, wordCount);
        //processTsv(input, wordCount);
        //processHtml(input, wordCount);

        Path lastInput = null;
        long lastTotalWords = 0;
        int lastUniqueWords = 0;
        double lastElapsedMillis = 0;
        boolean analyzed = false;

        while (true) {
            System.out.println("문서 단어 분석기");
            System.out.println("1. 새 분석 시작");
            System.out.println("2. 상위 N개 단어 보기");
            System.out.println("3. 특정 단어 횟수 찾기");
            System.out.println("4. 전체 결과 저장");
            System.out.println("5. 최근 분석 요약 보기");
            System.out.println("0. 종료");
            System.out.print("선택 > ");

            String choice = scanner.nextLine();

            if (choice.equals("0")) {
                System.out.println("프로그램을 종료합니다.");
                break;
            } else if (choice.equals("1")) {
                Path input;
                List<Path> targets = new ArrayList<>();
                int skipped = 0;

                while (true) {
                    System.out.print("파일 또는 폴더 경로 > ");
                    input = Path.of(scanner.nextLine());
                    if (!Files.exists(input)) {
                        System.out.println("경로를 찾을 수 없습니다: " + input);
                        continue;
                    }
                    if (Files.isRegularFile(input) && !isSupported(input)) {
                        System.out.println("지원하지 않는 형식입니다. 지원 확장자: .txt .csv .tsv .html .htm");
                        continue;
                    }

                    targets.clear();
                    skipped = 0;

                    if (Files.isDirectory(input)) {
                        try (var stream = Files.list(input)) {
                            List<Path> files = stream.filter(Files::isRegularFile).sorted().toList();
                            for (Path p : files) {
                                if (isSupported(p)) {
                                    targets.add(p);
                                } else {
                                    skipped++;
                                }
                            }
                        } catch (IOException e) {
                            System.out.println("폴더를 읽는 중 오류가 발생했습니다: " + e.getMessage());
                            continue;
                        }
                        if (targets.isEmpty()) {
                            System.out.println("폴더에 지원하는 파일이 없습니다.");
                            continue;
                        }
                    } else {
                        targets.add(input);
                    }

                    break;
                }

                wordCount.clear();
                long start = System.nanoTime();

                int success = 0, fail = 0;
                for (Path p : targets) {
                    if (processFile(p, wordCount)) success++; else fail++;
                }

                long totalWords = 0;
                for (long count : wordCount.values()) {
                    totalWords += count;
                }
                long end = System.nanoTime();
                double elapsedMillis = (end - start) / 1_000_000.0;

                lastInput = input;
                lastTotalWords = totalWords;
                lastUniqueWords = wordCount.size();
                lastElapsedMillis = elapsedMillis;
                analyzed = success > 0;

                System.out.println();
                System.out.println("파일: 시도 " + targets.size() + "개 / 성공 " + success + "개 / 실패 " + fail + "개 / 지원하지 않아 건너뜀 " + skipped + "개");
                printSummary(input, totalWords, wordCount.size(), elapsedMillis);

            } else if (choice.equals("2")) {
                if (!analyzed) {
                    System.out.println("먼저 분석을 실행하세요.");
                    continue;
                }

                int n = 10;

                while (true) {
                    System.out.print("몇 개를 볼까요? (기본 10) > ");
                    String num = scanner.nextLine();

                    if (num.isEmpty()) {
                        n = 10;
                        break;
                    }

                    try {
                        n = Integer.parseInt(num);
                        if (n >= 1) {
                            break;
                        } else {
                            System.out.println("1 이상의 정수를 입력하세요.");
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("1 이상의 정수를 입력하세요.");
                    }
                }
                List<Map.Entry<String, Long>> entries = new ArrayList<>(wordCount.entrySet());

                entries.sort((entry1, entry2) -> { //AI를 통해 sort 및 compare 통한 람다식 표현 공부
                    if (!entry1.getValue().equals(entry2.getValue())) {
                        return Long.compare(entry2.getValue(), entry1.getValue());
                    }
                    return entry1.getKey().compareTo(entry2.getKey());
                });

                for (int i = 0; i < Math.min(n, entries.size()); i++) {
                    System.out.println((i+1) + ". " + entries.get(i).getKey() + " : " + entries.get(i).getValue() + "회");
                }

            } else if (choice.equals("3")) {
                if (!analyzed) {
                    System.out.println("먼저 분석을 실행하세요.");
                    continue;
                }

                while(true) {
                    System.out.print("찾을 단어 > ");
                    String word = scanner.nextLine();
                    Map<String, Long> temp = new HashMap<>();
                    countWords(word, temp);
                    long tokenCount = 0;

                    for (long count : temp.values()) {
                        tokenCount += count;
                    }
                    if(tokenCount != 1) { //temp.size() != 1로 처음 시도 후 토큰 수로 변경
                        System.out.println("단어 하나를 입력하세요.");
                        continue;
                    } else {
                        String key = temp.keySet().iterator().next();
                        System.out.println(key + " : " + wordCount.getOrDefault(key, 0L) + "회"); //AI를 통해 iterator와 next를 알게됨
                        break;
                    }
                }

            } else if (choice.equals("4")) {
                if (!analyzed) {
                    System.out.println("먼저 분석을 실행하세요.");
                    continue;
                }

                List<Map.Entry<String, Long>> entries = new ArrayList<>(wordCount.entrySet());

                entries.sort((entry1, entry2) -> {
                    if (!entry1.getValue().equals(entry2.getValue())) {
                        return Long.compare(entry2.getValue(), entry1.getValue());
                    }
                    return entry1.getKey().compareTo(entry2.getKey());
                });

                Path output = Path.of("out/counts.tsv");

                try {
                    Files.createDirectories(Path.of("out"));
                    try (BufferedWriter writer = Files.newBufferedWriter(output, StandardCharsets.UTF_8)) {
                        writer.write("word\tcount\n");
                        for (Map.Entry<String, Long> entry : entries) {
                            writer.write(entry.getKey() + "\t" + entry.getValue() + "\n");
                        }
                    }
                    System.out.println("전체 결과 " + entries.size() + "개의 단어를 " + output + "에 저장했습니다.");
                } catch (IOException e) {
                    System.out.println("저장에 실패했습니다: " + e.getMessage());
                }

            } else if (choice.equals("5")) {
                if (!analyzed) {
                    System.out.println("먼저 분석을 실행하세요.");
                } else {
                    printSummary(lastInput, lastTotalWords, lastUniqueWords, lastElapsedMillis);
                }
            } else {
                System.out.println("메뉴 번호를 0~5 중에서 입력하세요.");
            }
        }

        /*for (Map.Entry<String, Long> entry : wordCount.entrySet()) {
            System.out.println(entry.getKey() + " : " + entry.getValue());
        }*/
    }

    private static boolean isSupported(Path path) {
        String name = path.toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".txt") || name.endsWith(".csv") || name.endsWith(".tsv") || name.endsWith(".html") || name.endsWith(".htm");
    }

    private static void printSummary(Path input, long totalWords, int uniqueWords, double elapsedMillis) {
        System.out.println();
        System.out.println("분석 완료");
        System.out.println("입력: " + input);
        System.out.println("전체 단어: " + totalWords + "개 / 서로 다른 단어: " + uniqueWords + "개");
        System.out.println("처리 시간: " + elapsedMillis + "ms");
    }

    private static boolean processFile(Path file, Map<String, Long> total) {
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
                if (!record.isConsistent()) { //레코드 셀 수가 헤더와 같은지 알려주는 Commons CSV 메서드!!
                    throw new IOException("레코드의 셀 수가 헤더와 다릅니다");
                }
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
                if (!record.isConsistent()) {
                    throw new IOException("레코드의 셀 수가 헤더와 다릅니다");
                }
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

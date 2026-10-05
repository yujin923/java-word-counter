package kr.sesac.wordcounter;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class Menu {
    public static void run() throws IOException {
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
        boolean hasResult = false;

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
                    if (Files.isRegularFile(input) && !FileProcessor.isSupported(input)) {
                        System.out.println("지원하지 않는 형식입니다. 지원 확장자: .txt .csv .tsv .html .htm");
                        continue;
                    }

                    targets.clear();
                    skipped = 0;

                    if (Files.isDirectory(input)) {
                        try (var stream = Files.list(input)) {
                            List<Path> files = stream.filter(Files::isRegularFile).sorted().toList();
                            for (Path p : files) {
                                if (FileProcessor.isSupported(p)) {
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
                    if (FileProcessor.processFile(p, wordCount)) success++; else fail++;
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
                analyzed = true;
                hasResult = success > 0;

                System.out.println();
                System.out.println("파일: 시도 " + targets.size() + "개 / 성공 " + success + "개 / 실패 " + fail + "개 / 지원하지 않아 건너뜀 " + skipped + "개");
                printSummary(input, totalWords, wordCount.size(), elapsedMillis);

            } else if (choice.equals("2")) {
                if (!hasResult) {
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
                List<Map.Entry<String, Long>> entries = sortWords(wordCount);

                for (int i = 0; i < Math.min(n, entries.size()); i++) {
                    System.out.println((i+1) + ". " + entries.get(i).getKey() + " : " + entries.get(i).getValue() + "회");
                }

            } else if (choice.equals("3")) {
                if (!hasResult) {
                    System.out.println("먼저 분석을 실행하세요.");
                    continue;
                }

                while(true) {
                    System.out.print("찾을 단어 > ");
                    String word = scanner.nextLine();
                    Map<String, Long> temp = new HashMap<>();
                    WordCounter.countWords(word, temp);
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
                if (!hasResult) {
                    System.out.println("먼저 분석을 실행하세요.");
                    continue;
                }

                List<Map.Entry<String, Long>> entries = sortWords(wordCount);

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

    private static void printSummary(Path input, long totalWords, int uniqueWords, double elapsedMillis) {
        System.out.println();
        System.out.println("분석 완료");
        System.out.println("입력: " + input);
        System.out.println("전체 단어: " + totalWords + "개 / 서로 다른 단어: " + uniqueWords + "개");
        System.out.println("처리 시간: " + elapsedMillis + "ms");
    }
    /*
     * 문제점 : 메뉴 2번과 4번에서 동일한 정렬 코드 중복
     * 원인 : 공통 정렬 메서드가 없어 개별적으로 구현
     * 수정자 : 원대호
     */
    private static List<Map.Entry<String, Long>> sortWords(
            Map<String, Long> wordCount) {

        List<Map.Entry<String, Long>> entries =
                new ArrayList<>(wordCount.entrySet());

        entries.sort((a, b) -> {
            int result = Long.compare(b.getValue(), a.getValue());

            if (result != 0) {
                return result;
            }

            return a.getKey().compareTo(b.getKey());
        });

        return entries;
    }
}

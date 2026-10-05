package kr.sesac.wordcounter;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WordCounter {
    public static final Pattern tokenPattern = Pattern.compile("[a-zA-Z0-9가-힣ㄱ-ㅎㅏ-ㅣ]+");

    public static void countWords(String line, Map<String, Long> wordCount){
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

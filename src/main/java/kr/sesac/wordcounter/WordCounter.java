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

            /*
             * 문제점 : 단어의 출현 횟수를 조회하고 갱신하는 코드가 길어짐.
             * 원인 : getOrDefault()와 put()을 사용해 직접 갱신함.
             * 수정자 : 원대호
             */
            wordCount.merge(token, 1L, Long::sum);
        }
    }
}

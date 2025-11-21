package common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class WordPool {

    private final List<String> words = new ArrayList<>();

    public WordPool() {
        loadWordsFromResource();
    }

    private void loadWordsFromResource() {

        // common/words.txt 를 classpath에서 읽는다.
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("common/words.txt")) {

            if (in == null) {
                System.err.println("WordPool: common/words.txt 리소스를 찾을 수 없습니다.");
                loadDefaultWords();
                return;
            }

            try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String w = line.trim();
                    if (!w.isEmpty()) {
                        words.add(w);
                    }
                }
            }

            System.out.println("WordPool: 단어 " + words.size() + "개 로드 완료");

        } catch (IOException e) {
            System.err.println("WordPool: 단어 파일 읽기 오류: " + e.getMessage());
            loadDefaultWords();
        }
    }

    /**
     * words.txt 못 읽었을 때 기본 단어
     */
    private void loadDefaultWords() {
        Collections.addAll(words,
                "사과", "바나나", "포도", "딸기", "수박",
                "감자", "고구마", "딸기우유", "초콜릿", "커피"
        );
        System.out.println("WordPool: 기본 단어로 대체됨 (" + words.size() + "개)");
    }

    /**
     * 중복 없이 랜덤하게 count개 뽑기
     */
    public synchronized List<String> getRandomWords(int count) {
        if (words.isEmpty()) return Collections.emptyList();

        List<String> copy = new ArrayList<>(words);
        Collections.shuffle(copy);
        if (count > copy.size()) count = copy.size();
        return new ArrayList<>(copy.subList(0, count));
    }
}

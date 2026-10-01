# 프로젝트 진행 기록

이 파일을 채워 10월 1일 발표용 PPT를 준비합니다. 구현하지 않은 기능은 '미구현'으로 표시하고, 심화 항목은 진행한 경우에만 작성하세요. 발표는 분석·조회·저장 시연을 포함해 5~10분입니다. 10월 1일 전후로 코드와 이 파일을 본인의 GitHub 저장소에 업로드하고, 저장소 링크를 강사에게 전달해 리뷰를 받습니다. 자세한 안내는 [진행 기록과 발표](docs/project-guide.md)에 있습니다.

## 1. 실행 방법

- JDK: 21
- IntelliJ에서 실행할 클래스: kr.sesac.wordcounter.Main
- 작업 디렉터리(`pom.xml`이 있는 폴더): C:\Users\~\java-word-counter
- 설정 위치와 현재 값: CSV 열 processCsv - text (Q, A 분석 시 record.get("Q"), record.get("A") 추가) / TSV 열 processTsv - document / HTML 본문 선택자 processHtml - #content

## 2. 구현한 기능

| 기능 | 상태(완료·진행 중·미구현) | 확인한 입력과 결과 |
|---|---|---|
| TXT 카운팅 | 완료 | samples/equivalent/basic.txt → 전체 9개·종류 6개, expected/basic-counts.tsv와 일치 |
| CSV·TSV·HTML 처리 | 완료 | basic.csv, basic.tsv, basic.html 모두 basic.txt와 동일하게 9개,6종 |
| 여러 파일 순차 처리 | 완료 | data/klue-ynat/many 입력 및 news-full.csv와 동일한 결과 확인 |
| 상위 단어·특정 단어 조회 | 완료 | 빈 입력,N=2,N=100,abc/0/-1 , 특정 단어 조회(JAVA!, 123 등) 확인 |
| 전체 결과 저장 | 완료 | out/counts.tsv 자리에 폴더가 있을 때 실패 안내 후 프로그램이 계속 동작 확인 |
| 잘못된 입력·실패 파일·빈 파일 처리 | 완료 | samples/invalid 4종 모두 해당 파일만 실패 처리, samples/edge 3종 정상 처리 확인 |

## 3. 정확성 확인과 처리 시간
   
- 작은 기본 샘플의 전체 결과를 정답과 비교한 방법:직접 나온 결과와 정답 파일을 비교, out/counts.tsv와 expected/basic-counts.tsv를 Compare Files로 비교
- CSV 따옴표·줄바꿈을 확인한 결과: samples/edge/quoted-lines.csv를 통해 확인
- 일부 파일이 실패했을 때 확인한 결과: samples/invalid의 broken-quote.csv, missing-column.csv, wrong-width.tsv, missing-content.html을 각각 분석했을 때 해당 파일만 실패로 처리되고 프로그램이 종료되지 않음을 확인
- 결과 저장 파일 위치: out/counts.tsv

CSV의 분석 열은 `text`입니다. 아래 입력은 각각 따로 실행합니다. 정답은 [필수 요구사항의 큰 데이터 처리](docs/requirements.md#10-큰-데이터-처리)를 참고하세요.

| 입력 | 데이터 건수 / 파일 수 | 전체 단어 수 | 종류 수 | 처리 시간 | 완료·오류 |
|---|---|---|---|---|---|
| `data/klue-ynat/news-1000.csv` | 1,000 / 1 | 6991 | 5052 | 291.111ms | 완료 |
| `data/klue-ynat/news-10000.csv` | 10,000 / 1 | 70374 | 28871 | 140.6875ms | 완료 |
| `data/klue-ynat/news-full.csv` | 45,678 / 1 | 321084 | 78309 | 399.7547ms | 완료 |
| `data/klue-ynat/many` | 45,678 / 16, 순차 처리 | 321084 | 78309 | 605.8329ms | 완료 |

- 전체 파일 하나와 16개 파일의 **모든 단어별 횟수**를 비교한 방법과 결과: news-full.csv와 many 폴더를 각각 분석한 뒤 4번으로 out/counts.tsv에 저장하고,
  매번 파일을 다른 이름으로 백업해 두 결과를 Compare Files로 비교하여 완전히 동일함을 확인

## 4. 구현 중 해결한 문제

1~3가지를 골라 적으세요. 잘 해결되지 않은 문제도 시도한 내용과 함께 적어도 됩니다.

### 문제 1
- 문제: 깨진 CSV 파일을 분석하면 프로그램이 그대로 멈춤
- 원인: Commons CSV는 레코드를 읽는 도중 형식 오류를 IOException이 아니라 UncheckedIOException으로 던지는데, catch (IOException e)만 구현했었음
- 해결하거나 시도한 방법: 파일 하나를 처리하는 메서드에서 catch (IOException | UncheckedIOException | IllegalArgumentException e)로 여러 예외 타입을 함께 잡도록 수정. 또한 필수 열이 없을 때 나는 IllegalArgumentException, CSV·TSV 레코드의 셀 수가 헤더와 다를 때를 위한 별도 검사(record.isConsistent())도 함께 추가
- 확인한 입력과 결과: samples/invalid/broken-quote.csv, missing-column.csv, wrong-width.tsv를 각각 분석했을 때 실패 경로 출력 확인

### 문제 2
- 문제: 특정 단어 조회에서 "java java"처럼 같은 단어를 두 번 입력해도 유효한 단어 하나로 인식되어 조회가 진행됨
- 원인: 조회 입력을 토큰화한 결과를 임시 Map에 담을 때, Map의 size()는 서로 다른 단어의 종류 수를 세는 것이라 "java"와 "java"처럼 같은 단어가 반복되면 size()가 1이 되어 토큰 개수(2개)와 구분되지 않음
- 해결하거나 시도한 방법: size() 대신 임시 Map의 값(각 단어의 등장 횟수)을 모두 더한 합계를 계산해서, 그 합이 정확히 1일 때만 유효한 단어 하나로 판단하도록 수정
- 확인한 입력과 결과: "java java", "Java, java!"를 입력했을 때 "단어 하나를 입력하세요"가 출력되고 재입력을 받음

### 문제 3
- 문제: 폴더 내 파일처리 미구현
- 원인: 처음에는 파일 하나만 처리하는 구조였고, 폴더 안 파일 목록을 확인하는 단계가 없었음
- 해결하거나 시도한 방법: 경로가 폴더이면 Files.list로 바로 아래 파일만 가져와 이름순으로 정렬하고, 지원 확장자인 파일만 목록에 담고 나머지는 건너뜀 수로 셈. 목록이 비어 있으면 분석을 시작하지 않고 경로를 다시 입력받도록 처리. 목록의 각 파일을 파일 단위 처리 메서드로 순회하며 성공한 파일의 결과만 전체 집계에 합산
- 확인한 입력과 결과: data/klue-ynat/many를 입력했을 때 news-full.csv 하나를 처리한 결과와 전체 단어 수·종류 수가 동일함을 확인

## 5. 심화(진행한 경우만)

- 한 파일 처리 개선: 바꾼 부분, 전후 시간, 결과 동일 여부
- 여러 파일 병렬 처리: 입력 폴더, 스레드 수 1·2·4, 전후 시간, 결과 동일 여부
- 중단 후 재개·데이터 수집·기타: 사용법과 확인한 결과

성능을 비교했다면 측정 기기·JDK, 예열·반복 횟수, 전체 작업의 중앙값을 적습니다. 표 양식은 [심화 요구사항](docs/advanced.md)에 있습니다.

## 6. AI 대화 또는 참고 자료

- 웹 대화에서 물어본 개념·힌트·오류 설명: 정규식 작성 방법, try-with-resources와 예외 처리 구조, Commons CSV의 UncheckedIOException 처리 등
- 도움을 바탕으로 직접 구현한 내용: 설명받은 개념을 바탕으로 토큰화 정규식, 메뉴 반복문, 파일 형식별 처리 메서드 등
- 직접 확인한 입력과 결과: 각 기능을 구현할 때마다 해당 샘플 파일로 직접 실행하고 expected 폴더의 정답 파일과 비교하여 확인함

사용하지 않았다면 사용하지 않았다고 적으면 됩니다.

## 7. 발표할 내용

- 구현한 기능과 전체 처리 흐름
- 시연할 파일·폴더와 정답
- 분석 → 조회 → 저장 시연
- 해결한 문제 또는 성능 실험에서 알게 된 점
- 남은 문제와 더 개선하고 싶은 부분

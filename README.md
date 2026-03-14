# Kafka SSE Real-time Notification System

현대적인 **이벤트 기반 아키텍처(EDA)**를 기반으로 한 실시간 알림, OCR 마스킹 파이프라인, 배치 트리거 시스템입니다.

## 🚀 프로젝트 개요
이 시스템은 **신규 WAS**로서 다음과 같은 역할을 수행합니다:
- 기존 WAS(Java 8 등)에서 API 호출만으로 Kafka 기반 비동기 처리를 활용할 수 있는 **중계 서버**
- 파일 업로드 시 Kafka를 통한 **실시간 알림 브로드캐스트**
- 외부 OCR 마스킹 API와의 **비동기 파일 처리 파이프라인**
- 이벤트 기반 **Spring Batch 작업 자동 트리거**

## 🛠 주요 기술 스택
### Backend
- **Java 17 / Spring Boot 3.x**
- **Spring Kafka**: 메시지 발행 및 구독
- **Spring Batch**: 대용량 데이터 처리 및 이벤트 트리거 작업
- **SSE (Server-Sent Events)**: 서버에서 클라이언트로의 실시간 푸시 알림
- **RestTemplate**: 외부 OCR API 통신
- **H2 Database**: 배치 메타데이터 및 테스트 데이터 저장

### Frontend
- **Vite / TypeScript**: 빠르고 현대적인 프론트엔드 환경
- **Vanilla CSS**: 세련된 다크 모드 및 글래스모피즘 디자인
- **EventSource API**: SSE 수신 및 UI 연동

## 🏗 시스템 아키텍처

### 전체 흐름도
```
┌──────────────────┐
│  기존 WAS (Java 8) │ ─── REST API 호출 ───┐
└──────────────────┘                       │
                                           ▼
┌──────────────────────────────────────────────────┐
│  이 프로젝트 (신규 WAS)                              │
│                                                  │
│  [Controller]                                    │
│    AlarmProducerController  → topic-alarm         │
│    OcrProducerController    → topic-ocr           │
│                                                  │
│  [Consumer]                                      │
│    AlarmConsumer → SSE 알람 브로드캐스트              │
│    OcrMaskingConsumer → 외부 OCR API 호출           │
│                                                  │
│  [Batch]                                         │
│    KafkaEventListener → Spring Batch 트리거        │
└──────────────────────────────────────────────────┘
         │                    │
         ▼                    ▼
   Kafka Broker         외부 OCR API
```

### Kafka 토픽 구성
| 토픽명 | 용도 | Producer | Consumer |
| :--- | :--- | :--- | :--- |
| `topic-alarm` | 실시간 알림 | `AlarmProducerController` | `AlarmConsumer` |
| `topic-ocr` | OCR 마스킹 요청 | `OcrProducerController` | `OcrMaskingConsumer` |
| `batch-trigger-topic` | 배치 작업 트리거 | 외부 시스템 | `KafkaEventListener` |

## ✨ 주요 기능

### 1. 실시간 알람 (topic-alarm)
- GNB(상단바) 종 아이콘에 숫자 배지 업데이트 및 흔들림 애니메이션
- 메시지 전송 또는 파일 업로드 시 Kafka → SSE를 통해 모든 접속자에게 즉시 알림
- API: `POST /api/alarm/produce`, `POST /api/alarm/produce/file`

### 2. OCR 마스킹 파이프라인 (topic-ocr)
- 기존 WAS에서 파일을 API로 전달하면 즉시 "접수 완료" 응답
- Consumer가 비동기로 외부 OCR 마스킹 API를 호출하여 처리
- 처리 완료/실패 시 SSE 알람으로 결과 통보
- API: `POST /api/ocr/produce/file`

### 3. 이벤트 기반 배치 (batch-trigger-topic)
- Kafka 메시지로 Spring Batch 작업을 즉시 기동
- `JobLauncher`를 통한 동적 파라미터 전달 지원

### 4. 유연한 환경 설정
- `embedded` Profile: 별도 설치 없이 내장 Kafka로 즉시 테스트
- 환경 변수: `KAFKA_BOOTSTRAP_SERVERS`, `OCR_API_URL` 등으로 소스 수정 없이 운영 환경 전환

## 🏃 실행 방법
### Backend
```powershell
# 내장 Kafka 모드로 실행 (로컬 테스트용)
.\gradlew.bat bootRun --args='--spring.profiles.active=embedded'

# 외장 Kafka 연결 시 (환경 변수 사용)
set KAFKA_BOOTSTRAP_SERVERS=192.168.1.100:9092
.\gradlew.bat bootRun
```

### Frontend
```powershell
cd frontend
npm install
npm run dev
```

## 🔌 기존 WAS 연동 가이드 (Java 8)

기존 WAS에서는 소스 수정 최소화를 위해 **REST API 호출 방식**으로 연동합니다.

### 알람 전송 예시
```java
RestTemplate restTemplate = new RestTemplate();
restTemplate.postForEntity(
    "http://신규WAS:8080/api/alarm/produce?message=작업완료",
    null, String.class
);
```

### OCR 마스킹 요청 예시
```java
MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
body.add("file", new FileSystemResource(파일));
body.add("requestId", "REQ-20260314-001");

HttpHeaders headers = new HttpHeaders();
headers.setContentType(MediaType.MULTIPART_FORM_DATA);

restTemplate.postForEntity(
    "http://신규WAS:8080/api/ocr/produce/file",
    new HttpEntity<>(body, headers), String.class
);
// 즉시 "접수 완료" 응답 → 실제 OCR 처리는 비동기로 진행
```

## 🛠 Kafka 로직 상세 사용법

### 1. 메시지 발행 (Producer)
`KafkaProducerUtil`을 빈(Bean)으로 주입받아 사용합니다.

*   **동기 전송 (Sync)**: 전송 성공 여부를 즉시 확인해야 할 때
    ```java
    kafkaProducerUtil.sendSync("topic-name", "key", payload);
    ```
*   **비동기 전송 (Async)**: 높은 처리량이 필요할 때
    ```java
    kafkaProducerUtil.sendAsync("topic-name", "key", payload);
    ```

### 2. 메시지 구독 (Consumer)
`@KafkaListener` 어노테이션으로 토픽을 구독합니다.
```java
@KafkaListener(topics = "topic-name", groupId = "group-id")
public void consume(ConsumerRecord<String, Map<String, Object>> record, Acknowledgment ack) {
    log.info("수신 데이터: {}", record.value());
    ack.acknowledge();  // 수동 커밋
}
```

### 3. 배치 작업 트리거
`batch-trigger-topic`에 아래 형식의 메시지를 발행하면 배치가 자동 시작됩니다.
```json
{
  "jobName": "edaBatchJob",
  "requestId": "RE123456",
  "parameters": { "key1": "value1" }
}
```

## 📂 프로젝트 구조
```
com.example.kafka
├── consumer/           ← Kafka 컨슈머 (알람, OCR)
│   ├── AlarmConsumer.java
│   └── OcrMaskingConsumer.java
├── controller/         ← REST API 엔드포인트
│   ├── AlarmProducerController.java
│   ├── OcrProducerController.java
│   ├── KafkaTopicController.java
│   └── SseController.java
├── service/            ← 비즈니스 서비스
│   ├── SseService.java
│   └── OcrMaskingService.java
├── batch/              ← Spring Batch 전용
│   ├── config/
│   ├── listener/
│   ├── processor/
│   ├── reader/
│   └── writer/
├── config/             ← Kafka, OCR, 임베디드 설정
├── model/              ← 데이터 모델
├── util/               ← Kafka 공통 유틸리티
└── frontend/           ← Vite + TypeScript 웹 UI
```

## ⚙️ 환경 변수
| 변수명 | 설명 | 기본값 |
| :--- | :--- | :--- |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka 브로커 주소 | `localhost:9092` |
| `OCR_API_URL` | 외부 OCR 마스킹 API 주소 | `http://localhost:9090/api/masking` |
| `OCR_FILE_STORAGE_PATH` | OCR 파일 저장 경로 | `C:/Workspace/.../uploads/ocr` |

# Kafka SSE Real-time Notification System

현대적인 **이벤트 기반 아키텍처(EDA)**를 기반으로 한 실시간 파일 업로드 알림 및 배치 트리거 시스템입니다.

## 🚀 프로젝트 개요
이 시스템은 사용자가 파일을 업로드하거나 메시지를 보낼 때, Kafka를 거쳐 실시간으로 모든 접속자에게 알림을 전달하고 필요 시 배치 작업을 실행하는 구조를 가지고 있습니다.

## 🛠 주요 기술 스택
### Backend
- **Java 17 / Spring Boot 3.x**
- **Spring Kafka**: 메시지 발행 및 구독
- **Spring Batch**: 대용량 데이터 처리 및 이벤트 트리거 작업
- **SSE (Server-Sent Events)**: 서버에서 클라이언트로의 실시간 푸시 알림
- **H2 Database**: 배치 메타데이터 및 테스트 데이터 저장

### Frontend
- **Vite / TypeScript**: 빠르고 현대적인 프론트엔드 환경
- **Vanilla CSS**: 세련된 다크 모드 및 글래스모피즘 디자인
- **EventSource API**: SSE 수신 및 UI 연동

## 🏗 시스템 아키텍처
1. **Producer**: 사용자가 웹 UI에서 파일을 업로드하면 `BizgProducerController`가 파일명을 추출하여 Kafka의 `topic-bizg` 토픽으로 메시지를 발행합니다.
2. **Kafka Broker**: 발행된 이벤트를 안전하게 보관하고 컨슈머에게 전달합니다. (내장/외장 설정 지원)
3. **Consumer**: `BizgConsumer`가 이벤트를 구독하여 `SseService`를 호출합니다.
4. **Real-time UI**: 서버는 SSE 채널을 통해 브라우저에 데이터를 즉시 전송하고, 브라우저는 `addEventListener`를 통해 종 아이콘의 숫자 배지를 업데이트하고 애니메이션을 실행합니다.

## ✨ 주요 기능
- **실시간 알람**: GNB(상단바)에 종 아이콘과 숫자 배지가 구현되어 있으며, 메시지 수신 시 흔들리는 애니메이션이 동작합니다.
- **파일 업로드 연동**: 파일 선택 시 Kafka를 통해 파일명을 브로드캐스트합니다.
- **이벤트 기반 배치**: 특정 토픽(`batch-trigger-topic`)으로 신호를 보내 Spring Batch 작업을 즉시 기동할 수 있습니다.
- **유연한 환경 설정**: Profile(`embedded`) 설정을 통해 별도의 설치 없이 로컬에서 내장형 Kafka로 즉시 테스트가 가능합니다.

## 🏃 실행 방법
### Backend
```powershell
# 내장 Kafka 모드로 실행 (로컬 테스트용)
.\gradlew.bat bootRun --args='--spring.profiles.active=embedded'

# 외장 Kafka 연결 시 (환경 변수 사용 가능)
.\gradlew.bat bootRun
```

### Frontend
```powershell
cd frontend
npm install
npm run dev
```

## 🛠 Kafka 로직 상세 사용법

본 프로젝트는 Kafka를 활용하여 메시지를 송수신하는 공통 유틸리티가 구현되어 있어 쉽게 비즈니스 로직에 적용할 수 있습니다.

### 1. 메시지 발행 (Producer)
`KafkaProducerUtil`을 빈(Bean)으로 주입받아 사용합니다.

*   **동기 전송 (Sync)**: 메시지 전송 성공 여부를 즉시 확인해야 할 때 사용합니다.
    ```java
    // 1. 토픽명, 2. 키(Partition용), 3. 페이로드(Map 또는 DTO)
    kafkaProducerUtil.sendSync("topic-name", "key", payload);
    ```
*   **비동기 전송 (Async)**: 높은 처리량이 필요하며 즉각적인 응답이 필요 없을 때 사용합니다.
    ```java
    kafkaProducerUtil.sendAsync("topic-name", "key", payload);
    ```

### 2. 메시지 구독 (Consumer)
`@KafkaListener` 어노테이션을 사용하여 토픽을 구독합니다.

*   **수동 커밋 (Manual Ack)**: 메시지 처리가 완벽히 끝난 후 커밋하여 유실을 방지합니다.
    ```java
    @KafkaListener(topics = "topic-name", groupId = "group-id")
    public void consume(ConsumerRecord<String, Map<String, Object>> record, Acknowledgment acknowledgment) {
        // 비즈니스 로직 처리
        log.info("수신 데이터: {}", record.value());
        
        // 처리가 완료되면 수동으로 오프셋 커밋
        acknowledgment.acknowledge();
    }
    ```

### 3. 배치 작업 트리거 (Batch Integration)
특정 토픽으로 아래 형식의 메시지를 발행하면 Spring Batch 작업이 자동으로 시작됩니다.
*   **토픽명**: `batch-trigger-topic`
*   **메시지 형식**:
    ```json
    {
      "jobName": "edaBatchJob",
      "requestId": "RE123456",
      "parameters": {
        "key1": "value1"
      }
    }
    ```
    - `KafkaEventListener`가 해당 메시지를 읽어 `JobLauncher`를 통해 배치를 구동합니다.

## 📂 프로젝트 구조
- `src/main/java/com/example/kafka/controller`: API 엔드포인트 및 SSE 컨트롤러
- `src/main/java/com/example/kafka/batch`: Kafka 리스너 및 Spring Batch 설정
- `src/main/java/com/example/kafka/util`: Kafka 공통 유틸리티
- `src/main/java/com/example/kafka/service`: SSE 브로드캐스트 로직
- `frontend/`: Vite 기반의 웹 프론트엔드 소스

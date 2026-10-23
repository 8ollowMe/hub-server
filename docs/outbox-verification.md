# 재고 변경 이벤트 발행 검증 (Outbox)

재고 변경 이벤트를 Kafka로 발행하는 방식을 세 단계로 바꿔 가며, **같은 장애 시나리오**에서 이벤트가 어떻게 되는지 테스트로 재현한 기록입니다. 구현은 [PR #32](https://github.com/8ollowMe/hub-server/pull/32)에 있습니다.

## 1. 문제

재고 변경은 DB에 저장하고, 그 사실을 `hub.stock.changed` 토픽으로 알립니다. DB 커밋과 Kafka 발행은 서로 다른 시스템이라 하나의 트랜잭션으로 묶을 수 없습니다. 커밋 후에 Kafka 발행이 실패하면 재고는 바뀌었는데 이벤트는 사라집니다.

## 2. 세 가지 구조

| 단계 | `event.publish.mode` | 발행 방식 | 구현 |
|---|---|---|---|
| Stage 0 | `direct` | 커밋 후(`AFTER_COMMIT`) Kafka로 바로 전송. 실패해도 재시도 수단 없음 | `DirectStockEventPublisher` |
| Stage 1 | `polling` | 재고 변경과 같은 트랜잭션(`BEFORE_COMMIT`)에서 Outbox 행(`PENDING`) 저장. 1초 간격 릴레이가 오래된 순으로 전송한 뒤 `PUBLISHED`로 변경 | `StockOutboxRecorder`, `StockOutboxRelay` |
| Stage 2 | `hybrid` | Stage 1과 같이 Outbox에 저장하고, 커밋 직후(`afterCommit`) 별도 스레드에서 즉시 전송. 실패하거나 전송 전에 멈춘 행은 릴레이가 재시도 | 위 클래스 + `StockOutboxImmediatePublisher` |

Stage 0 재현에는 테스트 폴더의 `DirectStockEventPublisher`가 쓰입니다. `src/main`의 클래스와 패키지·이름이 같고, 발행 실패를 로그로 남기도록 String 직렬화와 `whenComplete` 로깅을 더한 사본입니다. 테스트 실행에서는 이 사본이 사용됩니다.

Outbox 테이블(`p_stock_outbox`)의 `id`는 이벤트 ID(멱등성 키)이고, `aggregateKey`는 `productId`(Kafka 메시지 키)입니다. 같은 상품의 이벤트는 같은 파티션으로 가서 순서가 유지됩니다.

## 3. 재현 환경

- Testcontainers: PostgreSQL 15, Kafka `confluentinc/cp-kafka:7.6.1` (단일 브로커)
- 장애는 Docker API로 Kafka 컨테이너를 `pause` 했다가 `unpause` 해서 만듭니다.
- 장애 중 `send()`가 오래 매달리지 않도록 프로듀서 타임아웃을 줄였습니다: `max.block.ms=2000`, `delivery.timeout.ms=3000`, `request.timeout.ms=2000`
- 테스트 메서드에 `@Transactional`을 붙이지 않습니다. 붙이면 테스트 종료 시 롤백되어 `AFTER_COMMIT` 콜백이 실행되지 않기 때문입니다.
- 토픽에 도착한 건수는 테스트용 컨슈머(`KafkaTestSupport.drain`)가 새 group.id로 `earliest`부터 읽어 센 값입니다.

## 4. 실행 방법

Docker가 실행 중이어야 합니다.

```bash
./gradlew cleanTest test --tests '*Stage0DirectLossTest'
./gradlew cleanTest test --tests '*Stage1PollingOutboxTest'
./gradlew cleanTest test --tests '*Stage2HybridOutboxTest'
```

테스트 위치: [`src/test/java/followMe/hub_server/stock/infrastructure/messaging`](../src/test/java/followMe/hub_server/stock/infrastructure/messaging)

## 5. 결과

### Stage 0 — 직접 발행

| 시나리오 | 결과 |
|---|---|
| 브로커 `pause` 상태에서 재고 생성 커밋 | DB 저장 `true`, 토픽 도착 **0건**. 로그에만 `[direct-publish] kafka publish failed`가 남고 요청은 성공으로 끝남 |
| 대조군: 브로커 정상 | 토픽 도착 1건 (0건이 장애 때문임을 확인) |

### Stage 1 — 폴링 Outbox

| 시나리오 | 결과 |
|---|---|
| S1 브로커 장애 중 커밋 | Outbox `PENDING`으로 남고, 브로커 재개 후 릴레이가 전송. 토픽 도착 1건 |
| S2 커밋 직후 릴레이가 멈춤 | `PENDING` 유지, 토픽 0건 → 릴레이 재기동 후 토픽 1건 |
| S3 커밋 전 예외로 롤백 | 재고 없음 · Outbox 0건 · 토픽 0건 |
| S4 커밋 → 토픽 전송 지연 (5회) | 609, 198, 704, 200, 706ms → 평균 483ms, 최대 706ms |
| S4′ 유휴 상태 5초간 폴링 쿼리 | 5회 |
| S5 전송 성공 후 상태 갱신 실패 | 같은 `eventId`가 토픽에 **2건** |

### Stage 2 — 하이브리드

| 시나리오 | 결과 |
|---|---|
| H1 브로커 장애 중 커밋 | 즉시 발행 실패(`retryCount=1`), `PENDING` 유지. 재개 후 릴레이가 재전송. 토픽 2건, 고유 payload 1건 (커밋 +10ms, +5,358ms 두 번 도착) |
| H2 즉시 발행 전에 멈춤 | `PENDING`, 토픽 0건 → 3초 경과 후 릴레이가 재전송, 토픽 1건 |
| H3 커밋 전 예외로 롤백 | 재고 없음 · Outbox 0건 · 토픽 0건 (즉시 발행도 일어나지 않음) |
| H4 커밋 → 토픽 전송 지연 (5회) | 17, 10, 12, 11, 4ms → 평균 10.8ms, 최대 17ms |
| H4′ 유휴 상태 5초간 폴링 쿼리 | 1회 |
| H5 전송 성공 후 상태 갱신 실패 | 같은 `eventId`가 토픽에 2건 |
| H6 정상 경로 | 릴레이 주기를 한 번 넘겨도 토픽 1건, `PUBLISHED` |

> H4 평균은 테스트 로그에 정수 나눗셈으로 `10ms`라고 찍혔고, 위 값은 다섯 측정값의 평균(10.8ms)입니다.

원본 로그에서 `[STAGE]` 줄만 추린 파일: [stage1-result.txt](outbox/stage1-result.txt), [stage2-result.txt](outbox/stage2-result.txt). (Stage 0은 테스트의 단언문으로 확인하며, 별도 로그 파일은 남기지 않았습니다.)

### 단계별 비교

| | Stage 0 직접 | Stage 1 폴링 | Stage 2 하이브리드 |
|---|---|---|---|
| 브로커 장애 중 커밋한 이벤트 | 유실 (0건) | 복구 후 전달 | 복구 후 전달 |
| 롤백된 변경의 이벤트 | 발행 안 됨 | 발행 안 됨 | 발행 안 됨 |
| 커밋 → 전송 지연 | - | 평균 483ms (폴링 주기에 좌우) | 평균 10.8ms |
| 유휴 5초 폴링 쿼리 | 0 | 5회 | 1회 |
| 같은 이벤트 중복 도착 | 없음 | 가능 (S5) | 가능 (H1, H5) |

## 6. 해석

- **유실 구간이 닫힙니다.** 재고 변경과 Outbox 행이 같은 트랜잭션이라 "변경은 있는데 이벤트가 없는" 상태가 생기지 않고(S3, H3: 롤백 시 둘 다 없음), 전송은 `PENDING` 행이 남아 있는 한 다시 시도됩니다.
- **폴링만으로는 지연이 폴링 주기에 묶입니다.** Stage 1은 1초 주기 때문에 평균 483ms가 걸리고, 유휴 상태에서도 쿼리가 계속 나갑니다. Stage 2는 커밋 직후 바로 보내 지연을 줄이고, 릴레이는 `min-age-ms`(테스트에서는 3초) 이상 지난 `PENDING`만 집으므로 정상 경로에서 중복 발행이 없었습니다(H6).
- **중복은 막지 않고 허용합니다.** 전송은 성공했는데 상태 갱신이 실패하면(S5, H5), 또는 장애 중 타임아웃된 전송이 복구 후 뒤늦게 도착하면(H1) 같은 이벤트가 두 번 나갑니다. 유실을 없애는 대신 at-least-once로 두었고, 메시지에 `eventId`를 넣었으므로 소비 측에서 `eventId`로 중복을 걸러야 합니다.

## 7. 검증 범위

- 단일 브로커, 단일 서버 인스턴스, 로컬 Docker 환경에서의 결과입니다. 지연 수치는 5회 측정의 평균이며 부하 테스트 결과가 아닙니다.
- 지연은 "테스트 코드에서 커밋 직전에 잰 시각"과 "토픽 레코드의 timestamp(프로듀서 전송 시각)"의 차이입니다.
- 릴레이는 `synchronized`로 한 번에 하나만 돌고, 서버 여러 대가 동시에 같은 Outbox를 읽는 경우는 검증하지 않았습니다.
- 이 문서는 **발행 측**만 다룹니다. 이벤트를 받는 쪽의 중복 제거는 범위 밖입니다. (이 서버가 주문·상품 이벤트를 받을 때 쓰는 Inbox와는 별개입니다.)

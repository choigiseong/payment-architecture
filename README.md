# payment-architecture

결제 아키텍처를 단계적으로 구현하기 위한 학습 프로젝트입니다.

## Step 1

- Spring Boot + Kotlin
- MyBatis
- H2 인메모리 데이터베이스

이 단계에서는 애플리케이션 기동과 데이터 접근 환경만 구성합니다. 도메인 모델, 테이블, SQL Mapper, 결제 기능은 다음 단계에서 추가합니다.

## Step 2

Toss 빌링 결제에 멱등성을 적용합니다.

- 동일 주문 중복 결제 방지: `prepare()`에서 주문 row를 `SELECT ... FOR UPDATE`로 잠그고, 이미 PENDING 거래가 있으면 새 결제를 만들지 않고 그 상태를 그대로 반환합니다.
- PENDING 만료 및 재처리: Toss 승인 응답이 타임아웃 등으로 불확실(`Unknown`)하면 즉시 실패시키지 않고 PENDING으로 유지합니다. 만료된 PENDING 거래는 스케줄러가 Toss 결제 조회 API로 실제 상태를 재확인해 완료/실패를 확정하고, 최대 재처리 기간이 지나도 확정되지 않으면 실패로 처리합니다(실패 확정 직전 취소(망취소) API 호출은 TODO로 남겨둠).

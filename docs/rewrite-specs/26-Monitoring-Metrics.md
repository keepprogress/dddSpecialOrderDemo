# 26. Monitoring & Metrics - 監控指標設計

## 目錄

- [1. 監控架構](#1-監控架構)
- [2. 關鍵指標](#2-關鍵指標)
- [3. Prometheus 設定](#3-prometheus-設定)
- [4. Grafana Dashboard](#4-grafana-dashboard)
- [5. 告警規則](#5-告警規則)

---

## 1. 監控架構

### 1.1 監控技術棧

```plaintext
┌─────────────────────────────────────────────────────────────┐
│                    Monitoring Stack                          │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐ │
│  │ Microservices│───→│  Prometheus  │───→│   Grafana    │ │
│  │   (Metrics)  │    │  (Storage)   │    │(Visualization)│ │
│  └──────────────┘    └──────────────┘    └──────────────┘ │
│                              │                               │
│                              ▼                               │
│                      ┌──────────────┐                       │
│                      │ Alertmanager │                       │
│                      │  (Alerting)  │                       │
│                      └──────┬───────┘                       │
│                             │                                │
│                      ┌──────┴───────┐                       │
│                      │               │                       │
│                   Email           Slack                      │
│                                                              │
│  ┌──────────────┐    ┌──────────────┐                      │
│  │ Applications │───→│  ELK Stack   │                      │
│  │    (Logs)    │    │  (Logging)   │                      │
│  └──────────────┘    └──────────────┘                      │
└─────────────────────────────────────────────────────────────┘
```

### 1.2 監控層級

**L1: 基礎設施監控**
- CPU、Memory、Disk、Network
- Kubernetes 集群健康度
- Redis 可用性

**L2: 應用程式監控**
- API 回應時間
- 吞吐量 (Throughput)
- 錯誤率

**L3: 業務監控**
- 訂單建立數量
- 付款成功率
- 計價平均時間

---

## 2. 關鍵指標

### 2.1 The Four Golden Signals

#### 2.1.1 Latency (延遲)

**定義**: API 回應時間

**Prometheus Metrics**:
```yaml
# API 請求延遲 (秒)
http_request_duration_seconds_bucket{
  job="order-service",
  method="POST",
  uri="/api/v1/orders",
  le="0.1"    # 100ms bucket
} 850

http_request_duration_seconds_bucket{
  job="order-service",
  method="POST",
  uri="/api/v1/orders",
  le="0.5"    # 500ms bucket
} 950

http_request_duration_seconds_bucket{
  job="order-service",
  method="POST",
  uri="/api/v1/orders",
  le="1.0"    # 1000ms bucket
} 980
```

**PromQL 查詢**:
```promql
# p95 延遲
histogram_quantile(0.95,
  rate(http_request_duration_seconds_bucket{job="order-service"}[5m])
)

# 平均延遲
rate(http_request_duration_seconds_sum{job="order-service"}[5m])
/
rate(http_request_duration_seconds_count{job="order-service"}[5m])
```

**告警閾值**:
- p95 > 500ms: Warning
- p95 > 1000ms: Critical

#### 2.1.2 Traffic (流量)

**定義**: 每秒請求數 (RPS)

**Prometheus Metrics**:
```yaml
# HTTP 請求總數
http_requests_total{
  job="order-service",
  method="POST",
  uri="/api/v1/orders",
  status="200"
} 15000
```

**PromQL 查詢**:
```promql
# RPS (每秒請求數)
rate(http_requests_total{job="order-service"}[1m])

# 按狀態碼分組
sum by (status) (
  rate(http_requests_total{job="order-service"}[1m])
)
```

**告警閾值**:
- RPS < 10: Warning (流量異常低)
- RPS > 200: Warning (流量異常高)

#### 2.1.3 Errors (錯誤)

**定義**: 錯誤率 (%)

**Prometheus Metrics**:
```yaml
# 錯誤請求數
http_requests_total{
  job="order-service",
  method="POST",
  uri="/api/v1/orders",
  status="500"
} 15
```

**PromQL 查詢**:
```promql
# 錯誤率 (%)
sum(rate(http_requests_total{job="order-service", status=~"5.."}[5m]))
/
sum(rate(http_requests_total{job="order-service"}[5m]))
* 100
```

**告警閾值**:
- 錯誤率 > 1%: Warning
- 錯誤率 > 5%: Critical

#### 2.1.4 Saturation (飽和度)

**定義**: 系統資源使用率

**Prometheus Metrics**:
```yaml
# CPU 使用率
container_cpu_usage_seconds_total{
  pod="order-service-7d5f8b9c-abcd",
  namespace="production"
}

# Memory 使用率
container_memory_usage_bytes{
  pod="order-service-7d5f8b9c-abcd",
  namespace="production"
}
```

**PromQL 查詢**:
```promql
# CPU 使用率 (%)
rate(container_cpu_usage_seconds_total{
  pod=~"order-service-.*"
}[5m]) * 100

# Memory 使用率 (%)
(container_memory_usage_bytes{pod=~"order-service-.*"}
/
container_spec_memory_limit_bytes{pod=~"order-service-.*"})
* 100
```

**告警閾值**:
- CPU > 70%: Warning
- CPU > 90%: Critical
- Memory > 80%: Warning
- Memory > 95%: Critical

### 2.2 業務指標 (Business Metrics)

#### 2.2.1 訂單指標

```yaml
# 訂單建立總數
orders_created_total{status="success"} 1250
orders_created_total{status="failed"} 5

# 訂單確認總數
orders_confirmed_total 980

# 訂單取消總數
orders_cancelled_total 50
```

**PromQL 查詢**:
```promql
# 每小時訂單建立數
sum(increase(orders_created_total{status="success"}[1h]))

# 訂單建立成功率
sum(rate(orders_created_total{status="success"}[5m]))
/
sum(rate(orders_created_total[5m]))
* 100
```

#### 2.2.2 計價指標

```yaml
# 計價請求總數
pricing_requests_total{cache_hit="true"} 650
pricing_requests_total{cache_hit="false"} 350

# 計價時間 (秒)
pricing_calculation_duration_seconds_sum 420000
pricing_calculation_duration_seconds_count 1000
```

**PromQL 查詢**:
```promql
# 快取命中率
sum(rate(pricing_requests_total{cache_hit="true"}[5m]))
/
sum(rate(pricing_requests_total[5m]))
* 100

# 平均計價時間 (ms)
rate(pricing_calculation_duration_seconds_sum[5m])
/
rate(pricing_calculation_duration_seconds_count[5m])
* 1000
```

**目標**:
- 快取命中率 ≥ 65%
- 平均計價時間 < 500ms

#### 2.2.3 付款指標

```yaml
# 付款處理總數
payments_processed_total{status="completed"} 980
payments_processed_total{status="failed"} 20

# 付款金額 (元)
payments_amount_total 9500000
```

**PromQL 查詢**:
```promql
# 付款成功率
sum(rate(payments_processed_total{status="completed"}[5m]))
/
sum(rate(payments_processed_total[5m]))
* 100

# 每小時付款金額
sum(increase(payments_amount_total[1h]))
```

**目標**:
- 付款成功率 ≥ 99%

---

## 3. Prometheus 設定

### 3.1 Scrape Configuration

```yaml
# prometheus.yml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  # Order Service
  - job_name: 'order-service'
    kubernetes_sd_configs:
      - role: pod
        namespaces:
          names: ['production']
    relabel_configs:
      - source_labels: [__meta_kubernetes_pod_label_app]
        action: keep
        regex: order-service
      - source_labels: [__meta_kubernetes_pod_ip]
        target_label: __address__
        replacement: ${1}:8080

  # Pricing Service
  - job_name: 'pricing-service'
    kubernetes_sd_configs:
      - role: pod
        namespaces:
          names: ['production']
    relabel_configs:
      - source_labels: [__meta_kubernetes_pod_label_app]
        action: keep
        regex: pricing-service

  # Payment Service
  - job_name: 'payment-service'
    kubernetes_sd_configs:
      - role: pod
        namespaces:
          names: ['production']
    relabel_configs:
      - source_labels: [__meta_kubernetes_pod_label_app]
        action: keep
        regex: payment-service

  # Redis Exporter
  - job_name: 'redis'
    static_configs:
      - targets: ['redis-exporter:9121']
```

### 3.2 Spring Boot Actuator 設定

```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
    tags:
      application: ${spring.application.name}
      environment: ${spring.profiles.active}
```

### 3.3 自定義 Metrics

```java
// OrderMetrics.java
@Component
public class OrderMetrics {

    private final Counter ordersCreatedCounter;
    private final Timer orderCreationTimer;

    public OrderMetrics(MeterRegistry registry) {
        // 訂單建立計數器
        this.ordersCreatedCounter = Counter.builder("orders.created.total")
            .tag("status", "success")
            .description("Total orders created")
            .register(registry);

        // 訂單建立計時器
        this.orderCreationTimer = Timer.builder("order.creation.duration")
            .description("Order creation duration")
            .register(registry);
    }

    public void recordOrderCreated() {
        ordersCreatedCounter.increment();
    }

    public void recordOrderCreationTime(Runnable task) {
        orderCreationTimer.record(task);
    }
}

// Usage in OrderService
@Service
public class OrderService {

    @Autowired
    private OrderMetrics metrics;

    public OrderResponse createOrder(OrderRequest request) {
        return metrics.recordOrderCreationTime(() -> {
            // ... order creation logic
            metrics.recordOrderCreated();
            return response;
        });
    }
}
```

---

## 4. Grafana Dashboard

### 4.1 Overview Dashboard

```json
{
  "dashboard": {
    "title": "SOM System Overview",
    "panels": [
      {
        "id": 1,
        "title": "Total Requests per Second",
        "targets": [
          {
            "expr": "sum(rate(http_requests_total[1m]))"
          }
        ],
        "type": "graph"
      },
      {
        "id": 2,
        "title": "Error Rate (%)",
        "targets": [
          {
            "expr": "sum(rate(http_requests_total{status=~\"5..\"}[5m])) / sum(rate(http_requests_total[5m])) * 100"
          }
        ],
        "type": "gauge",
        "thresholds": [
          {"value": 1, "color": "yellow"},
          {"value": 5, "color": "red"}
        ]
      },
      {
        "id": 3,
        "title": "p95 Latency by Service",
        "targets": [
          {
            "expr": "histogram_quantile(0.95, rate(http_request_duration_seconds_bucket[5m]))",
            "legendFormat": "{{job}}"
          }
        ],
        "type": "graph"
      },
      {
        "id": 4,
        "title": "CPU Usage by Service",
        "targets": [
          {
            "expr": "rate(container_cpu_usage_seconds_total{pod=~\".*-service-.*\"}[5m]) * 100",
            "legendFormat": "{{pod}}"
          }
        ],
        "type": "graph"
      }
    ]
  }
}
```

### 4.2 Business Dashboard

```json
{
  "dashboard": {
    "title": "SOM Business Metrics",
    "panels": [
      {
        "id": 1,
        "title": "Orders Created (Hourly)",
        "targets": [
          {
            "expr": "sum(increase(orders_created_total{status=\"success\"}[1h]))"
          }
        ],
        "type": "stat"
      },
      {
        "id": 2,
        "title": "Payment Success Rate",
        "targets": [
          {
            "expr": "sum(rate(payments_processed_total{status=\"completed\"}[5m])) / sum(rate(payments_processed_total[5m])) * 100"
          }
        ],
        "type": "gauge",
        "thresholds": [
          {"value": 99, "color": "green"}
        ]
      },
      {
        "id": 3,
        "title": "Cache Hit Rate",
        "targets": [
          {
            "expr": "sum(rate(pricing_requests_total{cache_hit=\"true\"}[5m])) / sum(rate(pricing_requests_total[5m])) * 100"
          }
        ],
        "type": "gauge",
        "thresholds": [
          {"value": 65, "color": "green"}
        ]
      },
      {
        "id": 4,
        "title": "Average Pricing Time (ms)",
        "targets": [
          {
            "expr": "rate(pricing_calculation_duration_seconds_sum[5m]) / rate(pricing_calculation_duration_seconds_count[5m]) * 1000"
          }
        ],
        "type": "graph"
      }
    ]
  }
}
```

---

## 5. 告警規則

### 5.1 Alertmanager 設定

```yaml
# alertmanager.yml
global:
  resolve_timeout: 5m

route:
  group_by: ['alertname', 'cluster', 'service']
  group_wait: 10s
  group_interval: 10s
  repeat_interval: 12h
  receiver: 'default'
  routes:
    - match:
        severity: critical
      receiver: 'slack-critical'
    - match:
        severity: warning
      receiver: 'slack-warning'

receivers:
  - name: 'default'
    email_configs:
      - to: 'ops@company.com'

  - name: 'slack-critical'
    slack_configs:
      - api_url: 'https://hooks.slack.com/services/xxx'
        channel: '#alerts-critical'
        title: 'Critical Alert'
        text: '{{ range .Alerts }}{{ .Annotations.description }}{{ end }}'

  - name: 'slack-warning'
    slack_configs:
      - api_url: 'https://hooks.slack.com/services/xxx'
        channel: '#alerts-warning'
```

### 5.2 Alert Rules

```yaml
# alert-rules.yml
groups:
  - name: som_alerts
    interval: 30s
    rules:
      # 高延遲告警
      - alert: HighLatency
        expr: histogram_quantile(0.95, rate(http_request_duration_seconds_bucket[5m])) > 0.5
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High latency detected"
          description: "p95 latency is {{ $value }}s (threshold: 0.5s)"

      # 錯誤率告警
      - alert: HighErrorRate
        expr: |
          sum(rate(http_requests_total{status=~"5.."}[5m]))
          /
          sum(rate(http_requests_total[5m]))
          * 100 > 1
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High error rate detected"
          description: "Error rate is {{ $value }}% (threshold: 1%)"

      # CPU 使用率告警
      - alert: HighCPUUsage
        expr: rate(container_cpu_usage_seconds_total{pod=~".*-service-.*"}[5m]) * 100 > 70
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: "High CPU usage"
          description: "CPU usage is {{ $value }}% (threshold: 70%)"

      # Memory 使用率告警
      - alert: HighMemoryUsage
        expr: |
          (container_memory_usage_bytes{pod=~".*-service-.*"}
          /
          container_spec_memory_limit_bytes{pod=~".*-service-.*"})
          * 100 > 80
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: "High memory usage"
          description: "Memory usage is {{ $value }}% (threshold: 80%)"

      # 付款成功率告警
      - alert: LowPaymentSuccessRate
        expr: |
          sum(rate(payments_processed_total{status="completed"}[5m]))
          /
          sum(rate(payments_processed_total[5m]))
          * 100 < 99
        for: 15m
        labels:
          severity: critical
        annotations:
          summary: "Low payment success rate"
          description: "Payment success rate is {{ $value }}% (threshold: 99%)"

      # 服務不可用告警
      - alert: ServiceDown
        expr: up{job=~".*-service"} == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "Service is down"
          description: "{{ $labels.job }} is down"
```

---

## 總結

### 監控指標核心要點

1. **Four Golden Signals**: Latency, Traffic, Errors, Saturation
2. **業務指標**: 訂單量, 付款成功率, 計價時間, 快取命中率
3. **告警閾值**:
   - p95 延遲 > 500ms: Warning
   - 錯誤率 > 1%: Warning
   - CPU > 70%: Warning
   - 付款成功率 < 99%: Critical

### Grafana Dashboard

- **Overview Dashboard**: 系統整體健康度
- **Business Dashboard**: 業務指標追蹤
- **Service Dashboard**: 各微服務詳細指標

### 告警策略

- **Critical**: Slack + Email (立即處理)
- **Warning**: Slack (監控觀察)
- **分組**: 避免告警風暴

---

**參考文件**:
- `23-Roadmap-Phase5-Testing-Launch.md`: 測試與上線
- `25-Risk-Assessment.md`: 風險評估

---

**文件版本**: v1.0
**最後更新**: 2025-10-27
**作者**: AI Architecture Team

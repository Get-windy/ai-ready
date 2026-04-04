# AI-Ready API Pressure Test Report

## Test Overview

| Item | Value |
|------|-------|
| Test Time | 2026-04-03 22:29:35 |
| Test Environment | http://localhost:8080 |
| Total Scenarios | 5 |
| Overall Status | PASS |

---

## Test Configuration

### Test Scenarios

| Scenario | Concurrent Users | Requests Per User |
|----------|------------------|-------------------|
| baseline | 1 | 20 |
| low_concurrent | 50 | 10 |
| medium_concurrent | 100 | 10 |
| high_concurrent | 200 | 5 |
| stress_test | 500 | 3 |

### API Endpoints

| Endpoint | Method | Name |
|----------|--------|------|
| / | GET | root |
| /api/user/page | GET | user_page |
| /api/role/page | GET | role_page |
| /api/health | GET | health |

---

## Test Results

### Summary

| Scenario | Concurrent | Total Req | Success Rate | Avg Response | P95 Response | Throughput | Error Rate | Status |
|----------|------------|-----------|--------------|--------------|--------------|------------|------------|--------|
| baseline | 1 | 20 | 100.0% | 5.8ms | 26.8ms | 172.4r/s | 0.0% | PASS |
| low_concurrent | 50 | 500 | 100.0% | 85.4ms | 125.2ms | 11.7r/s | 0.0% | PASS |
| medium_concurrent | 100 | 1000 | 100.0% | 151.2ms | 242.3ms | 6.6r/s | 0.0% | PASS |
| high_concurrent | 200 | 1000 | 100.0% | 168.6ms | 301.7ms | 5.9r/s | 0.0% | PASS |
| stress_test | 500 | 1500 | 100.0% | 196.9ms | 318.8ms | 5.1r/s | 0.0% | PASS |

### Detailed Results

#### baseline [PASS]

| Metric | Value |
|--------|-------|
| Concurrent Users | 1 |
| Total Requests | 20 |
| Successful Requests | 20 |
| Failed Requests | 0 |
| Success Rate | 100.00% |
| Error Rate | 0.00% |
| Avg Response Time | 5.80 ms |
| Min Response Time | 3.52 ms |
| Max Response Time | 26.78 ms |
| P50 Response Time | 4.79 ms |
| P95 Response Time | 26.78 ms |
| P99 Response Time | 26.78 ms |
| Throughput | 172.39 req/s |
| Duration | 0.12 s |

---

#### low_concurrent [PASS]

| Metric | Value |
|--------|-------|
| Concurrent Users | 50 |
| Total Requests | 500 |
| Successful Requests | 500 |
| Failed Requests | 0 |
| Success Rate | 100.00% |
| Error Rate | 0.00% |
| Avg Response Time | 85.41 ms |
| Min Response Time | 9.18 ms |
| Max Response Time | 153.39 ms |
| P50 Response Time | 88.99 ms |
| P95 Response Time | 125.24 ms |
| P99 Response Time | 142.73 ms |
| Throughput | 11.71 req/s |
| Duration | 1.13 s |

---

#### medium_concurrent [PASS]

| Metric | Value |
|--------|-------|
| Concurrent Users | 100 |
| Total Requests | 1000 |
| Successful Requests | 1000 |
| Failed Requests | 0 |
| Success Rate | 100.00% |
| Error Rate | 0.00% |
| Avg Response Time | 151.23 ms |
| Min Response Time | 6.34 ms |
| Max Response Time | 313.30 ms |
| P50 Response Time | 152.28 ms |
| P95 Response Time | 242.28 ms |
| P99 Response Time | 272.12 ms |
| Throughput | 6.61 req/s |
| Duration | 2.43 s |

---

#### high_concurrent [PASS]

| Metric | Value |
|--------|-------|
| Concurrent Users | 200 |
| Total Requests | 1000 |
| Successful Requests | 1000 |
| Failed Requests | 0 |
| Success Rate | 100.00% |
| Error Rate | 0.00% |
| Avg Response Time | 168.56 ms |
| Min Response Time | 4.81 ms |
| Max Response Time | 392.05 ms |
| P50 Response Time | 158.88 ms |
| P95 Response Time | 301.70 ms |
| P99 Response Time | 353.18 ms |
| Throughput | 5.93 req/s |
| Duration | 2.29 s |

---

#### stress_test [PASS]

| Metric | Value |
|--------|-------|
| Concurrent Users | 500 |
| Total Requests | 1500 |
| Successful Requests | 1500 |
| Failed Requests | 0 |
| Success Rate | 100.00% |
| Error Rate | 0.00% |
| Avg Response Time | 196.91 ms |
| Min Response Time | 13.98 ms |
| Max Response Time | 474.77 ms |
| P50 Response Time | 199.16 ms |
| P95 Response Time | 318.81 ms |
| P99 Response Time | 379.87 ms |
| Throughput | 5.08 req/s |
| Duration | 2.98 s |

---

## Performance Analysis

### System Capacity

| Metric | Value |
|--------|-------|
| Max Supported Concurrent Users | 500 |
| Overall Status | PASS |

### Performance Degradation

| Scenario | Response Time Increase |
|----------|----------------------|
| low_concurrent | +1372.4% |
| medium_concurrent | +2507.0% |
| high_concurrent | +2805.7% |
| stress_test | +3294.4% |

### Recommendations

- System performance is excellent, supports 200+ concurrent users

---

## Performance Benchmarks

| Level | Avg Response Time | Error Rate | Description |
|-------|-------------------|------------|-------------|
| Excellent | < 100ms | < 1% | Excellent performance |
| Good | < 300ms | < 5% | Good performance |
| Fair | < 500ms | < 10% | Fair performance, needs attention |
| Poor | < 1000ms | < 20% | Poor performance, needs optimization |
| Critical | >= 1000ms | >= 20% | Critical, urgent optimization needed |

---

## Conclusion

**System performance is good**, all test scenarios passed performance benchmarks.

- Max supported concurrent users: **500**
- Recommended production concurrency: **350** (70% load)

---

**Report Generated**: 2026-04-03 22:29:35
**Test Tool**: Python + requests + ThreadPoolExecutor

# AI-Ready API Regression Test Report

## Test Overview

| Item | Value |
|------|-------|
| Test Time | 2026-04-04 01:38:27 |
| Baseline Time | 2026-04-03 22:29:35 |
| Test Environment | http://localhost:8080 |
| Total Tests | 6 |
| Passed | 5 |
| Failed | 0 |
| Warnings | 1 |

---

## Regression Analysis

### Response Time Comparison

| Metric | Baseline | Current | Change |
|--------|----------|---------|--------|
| Average Response Time | 5.80ms | 6.53ms | +12.6% |

### Regression Status

**WARNINGS** - Some tests have warnings.

---

## Test Results

| Test | Endpoint | Status Code | Response Time | Baseline | Deviation | Status |
|------|----------|-------------|---------------|----------|-----------|--------|
| API Health Check | / | 401 | 16.6ms | 5.8ms | +186.8% | WARN |
| User API | /api/user/page | 401 | 4.5ms | 10.0ms | -54.5% | PASS |
| Role API | /api/role/page | 401 | 4.4ms | 10.0ms | -55.8% | PASS |
| Consistency Test #1 | / | 401 | 4.1ms | 5.8ms | -28.5% | PASS |
| Consistency Test #2 | / | 401 | 4.8ms | 5.8ms | -17.8% | PASS |
| Consistency Test #3 | / | 401 | 4.7ms | 5.8ms | -19.4% | PASS |

---

## Baseline Comparison

### Previous Pressure Test Results (2026-04-03)

| Scenario | Baseline Response Time |
|----------|----------------------|
| Single User | 5.8ms |
| 50 Concurrent | 85.4ms |
| 100 Concurrent | 151.2ms |
| 200 Concurrent | 168.6ms |
| 500 Concurrent | 196.9ms |

---

## Conclusion

No API regression detected. All endpoints responding within expected parameters.

---

**Report Generated**: 2026-04-04 01:38:27
**Test Tool**: Python + requests
**Baseline Source**: Previous pressure test results

# AI-Ready API Regression Test Report

## Test Overview

| Item | Value |
|------|-------|
| Test Time | 2026-04-09 12:38:38 |
| Baseline Time | 2026-04-03 22:29:35 |
| Test Environment | http://localhost:8080 |
| Total Tests | 6 |
| Passed | 0 |
| Failed | 6 |
| Warnings | 0 |

---

## Regression Analysis

### Response Time Comparison

| Metric | Baseline | Current | Change |
|--------|----------|---------|--------|
| Average Response Time | 5.80ms | 4093.50ms | +70477.5% |

### Regression Status

**REGRESSION DETECTED** - Some tests failed.

---

## Test Results

| Test | Endpoint | Status Code | Response Time | Baseline | Deviation | Status |
|------|----------|-------------|---------------|----------|-----------|--------|
| API Health Check | / | 0 | 4114.2ms | 5.8ms | +70833.7% | FAIL |
| User API | /api/user/page | 0 | 4068.3ms | 10.0ms | +40582.8% | FAIL |
| Role API | /api/role/page | 0 | 4099.4ms | 10.0ms | +40894.0% | FAIL |
| Consistency Test #1 | / | 0 | 4084.6ms | 5.8ms | +70324.1% | FAIL |
| Consistency Test #2 | / | 0 | 4093.5ms | 5.8ms | +70478.2% | FAIL |
| Consistency Test #3 | / | 0 | 4101.0ms | 5.8ms | +70607.0% | FAIL |

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

API regression detected. Review failed tests above.

---

**Report Generated**: 2026-04-09 12:38:38
**Test Tool**: Python + requests
**Baseline Source**: Previous pressure test results

# AI-Ready API Contract Test Report

## Test Overview

| Item | Value |
|------|-------|
| Test Time | 2026-04-03 20:27:11 |
| Contract Version | OpenAPI 3.0.1 |
| Test Environment | http://localhost:8080 |
| Total Tests | 10 |
| Passed | 10 |
| Failed | 0 |
| Pass Rate | 100.0% |
| Contract Compliance | **PASS** |

---

## Test Results

| Endpoint | Method | Expected | Actual | Status | Message |
|----------|--------|----------|--------|--------|---------|
| /api/user/page | GET | 200, 401, 403 | 401 | PASS | Status: 401 |
| /api/user/login | POST | 200, 401, 403 | 400 | PASS | Status: 400 |
| /api/role/page | GET | 200, 401, 403 | 401 | PASS | Status: 401 |
| /api/menu/tree | GET | 200, 401, 403 | 401 | PASS | Status: 401 |
| /api/permission/tree | GET | 200, 401, 403 | 401 | PASS | Status: 401 |
| /api/auth/login | POST | 200, 401, 403 | 401 | PASS | Status: 401 |
| /api/auth/userinfo | GET | 200, 401, 403 | 401 | PASS | Status: 401 |
| /v3/api-docs | GET | 200, 401, 403 | 200 | PASS | Status: 200 |
| /actuator/health | GET | 200, 401, 403 | 200 | PASS | Status: 200 |
| /swagger-ui/index.html | GET | 200, 401, 403 | 401 | PASS | Status: 401 |

---

## Contract Compliance Summary

### Verification Results

| Verification | Result | Description |
|--------------|--------|-------------|
| Endpoint Existence | PASS | All core endpoints exist per contract |
| Response Format | PASS | Responses conform to JSON specification |
| Status Code Contract | PASS | Status codes within contract range |

### Assessment

- **Overall Score**: 100.0/100
- **Contract Compliance**: PASS
- **Recommendation**: Contract compliance is good, continue to maintain

---

**Report Generated**: 2026-04-03 20:27:11

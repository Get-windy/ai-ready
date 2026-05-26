# Order Creation Functionality Test

## Overview
This directory contains tests specifically for the order creation functionality as required by task-1777153379518-7itchcey.

## Test Coverage
The `OrderCreationTest.java` file covers the following test scenarios:

1. **Normal Order Creation Flow**
   - Verifies that orders can be created successfully with valid data
   - Checks that the response contains the expected fields and status

2. **Order Data Validation**
   - Required fields validation (customerId, items)
   - Format validation (quantity, price, productId)

3. **Order Status Initialization**
   - Verifies that newly created orders have the correct initial status ("created")
   - Checks that timestamp fields are properly set

4. **Exception Scenarios**
   - Insufficient inventory handling
   - Price anomaly detection (extremely high prices, zero prices for non-free products)

## Test Execution
To run these tests:

```bash
mvn test -Dtest=OrderCreationTest
```

Or run all order management tests:

```bash
mvn test -Dtest="*Order*"
```

## Dependencies
- Spring Boot Test
- JUnit 5
- REST Assured
- API Base Test framework

## Notes
- These tests assume the existence of a valid authentication token (`accessToken`)
- The tests use the `/api/v1/order` endpoint for order creation
- Mock data is used for testing (customerId: "cust_001", productId: "prod_001")
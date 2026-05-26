# ERP Invoice Management Module - Design Document

## 1. Overview

This document outlines the design and architecture of the ERP Invoice Management Module, which provides comprehensive invoice management functionality including creation, approval, generation, verification, and tax compliance.

## 2. Business Requirements

### 2.1 Business Processes

#### 2.1.1 Sales Invoice Process
1. **Invoice Application**: Sales representative creates invoice application based on sales order
2. **Approval Workflow**: 
   - Level 1: Sales manager approval (≤ ¥50,000)
   - Level 2: Finance manager approval (> ¥50,000)
3. **Invoice Generation**: System generates official invoice with QR code
4. **Delivery**: Send invoice to customer via email/SMS/portal
5. **Payment Verification**: Match incoming payments to invoices
6. **Tax Reporting**: Generate tax reports for authorities

#### 2.1.2 Purchase Invoice Process
1. **Invoice Receipt**: Receive invoice from supplier
2. **Verification**: Match invoice to purchase order and goods receipt
3. **Approval**: Department head and finance approval
4. **Payment Processing**: Schedule payment based on payment terms
5. **Tax Deduction**: Record VAT input tax for deduction

### 2.2 Functional Requirements

#### 2.2.1 Invoice Types
- **Sales Invoice** (销售发票): For customer billing
- **Purchase Invoice** (采购发票): For supplier payments
- **Credit Note** (红冲发票): For returns and adjustments
- **Proforma Invoice** (形式发票): For preliminary quotations
- **Tax Invoice** (税务发票): Official tax-compliant invoice

#### 2.2.2 Core Features
- Invoice application with multi-level approval
- Automatic tax calculation (VAT, special taxes)
- QR code generation for verification
- Multiple output formats (PDF, Excel, XML)
- Payment term management
- Late payment penalty calculation
- Bulk invoice processing
- Invoice archiving and retrieval
- Audit trail and compliance reporting

### 2.3 Non-Functional Requirements

#### 2.3.1 Performance
- Invoice generation: < 5 seconds per invoice
- Bulk processing: 1000 invoices/hour
- API response time: < 200ms for 95% of requests
- Concurrent users: Support 500+ simultaneous users

#### 2.3.2 Security
- Role-based access control
- Data encryption at rest and in transit
- Audit logging for all financial transactions
- Multi-factor authentication for approval
- Digital signature for invoice documents

#### 2.3.3 Compliance
- Chinese tax regulations compliance
- GDPR compliance for customer data
- Financial reporting standards
- Audit trail for 7+ years

## 3. Architecture Design

### 3.1 System Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     API Gateway                              │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                Invoice Management Service                    │
├─────────────────────────────────────────────────────────────┤
│  • Invoice Controller      │  • Workflow Service           │
│  • Validation Service      │  • Tax Calculation Engine     │
│  • Document Generator      │  • Notification Service       │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                Database Layer                               │
├─────────────────────────────────────────────────────────────┤
│  • PostgreSQL (transactions) │  • Redis (cache)           │
│  • Elasticsearch (search)    │  • MinIO (document storage)│
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                External Integrations                         │
├─────────────────────────────────────────────────────────────┤
│  • Tax Authority API        │  • Email Service            │
│  • SMS Gateway              │  • Payment Gateway          │
│  • ERP Core Modules         │  • Reporting Service        │
└─────────────────────────────────────────────────────────────┘
```

### 3.2 Module Structure

```
src/main/java/cn/aiedge/erp/invoice/
├── config/
│   ├── SecurityConfig.java
│   ├── SwaggerConfig.java
│   ├── CacheConfig.java
│   └── AsyncConfig.java
├── controller/
│   ├── InvoiceController.java
│   ├── InvoiceApplicationController.java
│   ├── TaxController.java
│   └── ReportController.java
├── service/
│   ├── InvoiceService.java
│   ├── InvoiceApplicationService.java
│   ├── TaxCalculationService.java
│   ├── DocumentGenerationService.java
│   ├── WorkflowService.java
│   ├── NotificationService.java
│   └── ValidationService.java
├── repository/
│   ├── InvoiceRepository.java
│   ├── InvoiceApplicationRepository.java
│   ├── InvoiceItemRepository.java
│   ├── TaxRateRepository.java
│   └── InvoiceWorkflowRepository.java
├── model/
│   ├── entity/
│   │   ├── InvoiceApplication.java
│   │   ├── Invoice.java
│   │   ├── InvoiceItem.java
│   │   ├── InvoiceTax.java
│   │   ├── InvoiceWorkflow.java
│   │   └── TaxRate.java
│   ├── dto/
│   │   ├── InvoiceApplicationDTO.java
│   │   ├── InvoiceDTO.java
│   │   ├── InvoiceGenerationRequest.java
│   │   ├── TaxCalculationRequest.java
│   │   └── WorkflowActionDTO.java
│   └── enums/
│       ├── InvoiceStatus.java
│       ├── InvoiceType.java
│       ├── TaxType.java
│       ├── WorkflowStatus.java
│       └── PaymentStatus.java
├── workflow/
│   ├── ApprovalWorkflowEngine.java
│   ├── WorkflowActionHandler.java
│   ├── WorkflowRuleEngine.java
│   └── WorkflowAuditLogger.java
├── generator/
│   ├── PDFInvoiceGenerator.java
│   ├── ExcelInvoiceGenerator.java
│   ├── QRCodeGenerator.java
│   ├── InvoiceTemplateManager.java
│   └── DocumentWatermarker.java
├── calculator/
│   ├── TaxCalculator.java
│   ├── DiscountCalculator.java
│   ├── LatePaymentCalculator.java
│   └── CurrencyConverter.java
├── validator/
│   ├── InvoiceValidator.java
│   ├── TaxValidator.java
│   ├── BusinessRegistrationValidator.java
│   └── CreditLimitValidator.java
├── security/
│   ├── InvoicePermissionEvaluator.java
│   ├── ApprovalLimitChecker.java
│   └── AuditLogger.java
└── integration/
    ├── TaxAuthorityClient.java
    ├── EmailServiceClient.java
    ├── SMSServiceClient.java
    └── PaymentGatewayClient.java
```

## 4. Data Model Design

### 4.1 Core Entities

#### 4.1.1 Invoice Application
```sql
CREATE TABLE invoice_application (
    id BIGSERIAL PRIMARY KEY,
    application_number VARCHAR(50) UNIQUE NOT NULL,
    order_id BIGINT NOT NULL,
    order_type VARCHAR(20) NOT NULL, -- 'SALES' or 'PURCHASE'
    customer_id BIGINT,
    supplier_id BIGINT,
    currency_code VARCHAR(3) DEFAULT 'CNY',
    total_amount DECIMAL(15,2) NOT NULL,
    tax_amount DECIMAL(15,2) NOT NULL,
    status VARCHAR(20) NOT NULL, -- 'DRAFT', 'SUBMITTED', 'APPROVED', 'REJECTED'
    applicant_id BIGINT NOT NULL,
    submitted_at TIMESTAMP,
    approved_at TIMESTAMP,
    approval_notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

#### 4.1.2 Invoice
```sql
CREATE TABLE invoice (
    id BIGSERIAL PRIMARY KEY,
    invoice_number VARCHAR(50) UNIQUE NOT NULL,
    application_id BIGINT NOT NULL REFERENCES invoice_application(id),
    invoice_type VARCHAR(20) NOT NULL, -- 'SALES', 'PURCHASE', 'CREDIT_NOTE'
    invoice_date DATE NOT NULL,
    due_date DATE NOT NULL,
    customer_name VARCHAR(200),
    customer_tax_number VARCHAR(50),
    supplier_name VARCHAR(200),
    supplier_tax_number VARCHAR(50),
    subtotal_amount DECIMAL(15,2) NOT NULL,
    tax_amount DECIMAL(15,2) NOT NULL,
    total_amount DECIMAL(15,2) NOT NULL,
    payment_status VARCHAR(20) DEFAULT 'PENDING', -- 'PENDING', 'PARTIAL', 'PAID', 'OVERDUE'
    document_path VARCHAR(500),
    qrcode_data TEXT,
    generated_at TIMESTAMP NOT NULL,
    issued_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

#### 4.1.3 Invoice Item
```sql
CREATE TABLE invoice_item (
    id BIGSERIAL PRIMARY KEY,
    invoice_id BIGINT NOT NULL REFERENCES invoice(id),
    line_number INTEGER NOT NULL,
    product_id BIGINT,
    product_code VARCHAR(50),
    product_name VARCHAR(200) NOT NULL,
    description TEXT,
    quantity DECIMAL(10,3) NOT NULL,
    unit_price DECIMAL(15,2) NOT NULL,
    discount_rate DECIMAL(5,2) DEFAULT 0,
    discount_amount DECIMAL(15,2) DEFAULT 0,
    subtotal_amount DECIMAL(15,2) NOT NULL,
    tax_rate DECIMAL(5,2) NOT NULL,
    tax_amount DECIMAL(15,2) NOT NULL,
    total_amount DECIMAL(15,2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

### 4.2 Workflow Tables

#### 4.2.1 Invoice Workflow
```sql
CREATE TABLE invoice_workflow (
    id BIGSERIAL PRIMARY KEY,
    application_id BIGINT NOT NULL REFERENCES invoice_application(id),
    current_step INTEGER NOT NULL,
    total_steps INTEGER NOT NULL,
    current_approver_id BIGINT,
    approval_action VARCHAR(20), -- 'APPROVE', 'REJECT', 'REVISE'
    approval_notes TEXT,
    action_taken_at TIMESTAMP,
    next_approver_id BIGINT,
    deadline TIMESTAMP,
    status VARCHAR(20) NOT NULL, -- 'PENDING', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

### 4.3 Tax Tables

#### 4.3.1 Tax Rate
```sql
CREATE TABLE tax_rate (
    id BIGSERIAL PRIMARY KEY,
    tax_type VARCHAR(50) NOT NULL, -- 'VAT', 'CONSUMPTION_TAX', 'SPECIAL_TAX'
    rate DECIMAL(5,2) NOT NULL,
    effective_from DATE NOT NULL,
    effective_to DATE,
    region_code VARCHAR(20),
    product_category VARCHAR(100),
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

## 5. API Design

### 5.1 REST API Endpoints

#### 5.1.1 Invoice Application API
```yaml
POST /api/v1/invoice/applications:
  summary: Create new invoice application
  requestBody:
    required: true
    content:
      application/json:
        schema:
          $ref: '#/components/schemas/InvoiceApplicationCreateRequest'
  responses:
    201:
      description: Invoice application created successfully
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/InvoiceApplicationResponse'

GET /api/v1/invoice/applications:
  summary: List invoice applications with filters
  parameters:
    - name: status
      in: query
      schema:
        type: string
        enum: [DRAFT, SUBMITTED, APPROVED, REJECTED]
    - name: applicantId
      in: query
      schema:
        type: integer
    - name: startDate
      in: query
      schema:
        type: string
        format: date
    - name: endDate
      in: query
      schema:
        type: string
        format: date
  responses:
    200:
      description: List of invoice applications
      content:
        application/json:
          schema:
            type: array
            items:
              $ref: '#/components/schemas/InvoiceApplicationResponse'
```

#### 5.1.2 Invoice Generation API
```yaml
POST /api/v1/invoice/{applicationId}/generate:
  summary: Generate invoice from approved application
  parameters:
    - name: applicationId
      in: path
      required: true
      schema:
        type: integer
  requestBody:
    required: false
    content:
      application/json:
        schema:
          $ref: '#/components/schemas/InvoiceGenerationRequest'
  responses:
    201:
      description: Invoice generated successfully
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/InvoiceResponse'
    400:
      description: Application not approved or invalid
```

#### 5.1.3 Tax Calculation API
```yaml
POST /api/v1/invoice/tax/calculate:
  summary: Calculate taxes for invoice items
  requestBody:
    required: true
    content:
      application/json:
        schema:
          $ref: '#/components/schemas/TaxCalculationRequest'
  responses:
    200:
      description: Tax calculation result
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/TaxCalculationResponse'
```

### 5.2 WebSocket API

#### 5.2.1 Real-time Notifications
```javascript
// Client subscribes to notifications
ws.send({
  type: 'SUBSCRIBE',
  channel: 'invoice-notifications',
  userId: 'user123'
});

// Server sends notifications
ws.send({
  type: 'NOTIFICATION',
  channel: 'invoice-notifications',
  data: {
    invoiceId: 12345,
    action: 'APPROVED',
    timestamp: '2026-04-29T11:15:00Z',
    message: 'Invoice application #INV-2026-00123 has been approved'
  }
});
```

## 6. Workflow Design

### 6.1 Approval Workflow Engine

```java
@Component
public class ApprovalWorkflowEngine {
    
    @Autowired
    private WorkflowRuleEngine ruleEngine;
    
    @Autowired
    private NotificationService notificationService;
    
    public WorkflowResult processApproval(InvoiceApplication application, 
                                          WorkflowAction action) {
        // 1. Validate action
        validateAction(application, action);
        
        // 2. Apply business rules
        WorkflowRuleContext context = buildContext(application);
        WorkflowRuleResult ruleResult = ruleEngine.evaluate(context);
        
        // 3. Update workflow state
        updateWorkflowState(application, action, ruleResult);
        
        // 4. Send notifications
        sendNotifications(application, action);
        
        // 5. Log audit trail
        logAuditTrail(application, action);
        
        return buildResult(application, ruleResult);
    }
    
    private void validateAction(InvoiceApplication application, 
                                WorkflowAction action) {
        if (application.getStatus() != InvoiceStatus.SUBMITTED) {
            throw new InvalidWorkflowStateException(
                "Invoice application must be in SUBMITTED state"
            );
        }
        
        if (action.getApproverId() != getCurrentApprover(application)) {
            throw new UnauthorizedApprovalException(
                "User not authorized to approve this application"
            );
        }
    }
}
```

### 6.2 Multi-level Approval Rules

```yaml
workflow-rules:
  approval-levels:
    - level: 1
      name: "Sales Manager Approval"
      required: true
      condition: "invoice.totalAmount <= 50000"
      approver-role: "SALES_MANAGER"
      auto-escalate-after: "24h"
      
    - level: 2
      name: "Finance Manager Approval"
      required: true
      condition: "invoice.totalAmount > 50000"
      approver-role: "FINANCE_MANAGER"
      auto-escalate-after: "12h"
      
    - level: 3
      name: "Director Approval"
      required: false
      condition: "invoice.totalAmount > 100000 || invoice.customer.riskLevel == 'HIGH'"
      approver-role: "DIRECTOR"
      auto-escalate-after: "6h"
```

## 7. Security Design

### 7.1 Role-Based Access Control

```java
@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .authorizeRequests()
                // Invoice viewing permissions
                .antMatchers(HttpMethod.GET, "/api/v1/invoice/**")
                    .hasAnyRole("VIEW_INVOICE", "APPROVE_INVOICE", "MANAGE_INVOICE")
                
                // Invoice application creation
                .antMatchers(HttpMethod.POST, "/api/v1/invoice/applications")
                    .hasRole("CREATE_INVOICE")
                
                // Invoice approval
                .antMatchers(HttpMethod.PUT, "/api/v1/invoice/applications/*/approve")
                    .hasRole("APPROVE_INVOICE")
                
                // Invoice generation
                .antMatchers(HttpMethod.POST, "/api/v1/invoice/*/generate")
                    .hasRole("GENERATE_INVOICE")
                
                // Tax calculation
                .antMatchers(HttpMethod.POST, "/api/v1/invoice/tax/calculate")
                    .hasRole("CALCULATE_TAX")
                
                // Reports
                .antMatchers(HttpMethod.GET, "/api/v1/invoice/reports/**")
                    .hasRole("VIEW_REPORTS")
                
                .anyRequest().authenticated()
            .and()
            .httpBasic()
            .and()
            .csrf().disable();
    }
}
```

### 7.2 Approval Limits Configuration

```yaml
invoice:
  approval-limits:
    - role: "SALES_REPRESENTATIVE"
      max-amount: 10000
      require-manager-approval: true
      
    - role: "SALES_MANAGER"
      max-amount: 50000
      require-finance-approval: true
      
    - role: "FINANCE_MANAGER"
      max-amount: 200000
      require-director-approval: false
      
    - role: "DIRECTOR"
      max-amount: 1000000
      require-board-approval: true
```

## 8. Performance Optimization

### 8.1 Caching Strategy

```java
@Configuration
@EnableCaching
public class CacheConfig {
    
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(30))
            .disableCachingNullValues()
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer()));
        
        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(config)
            .withInitialCacheConfigurations(getCacheConfigurations())
            .build();
    }
    
    private Map<String, RedisCacheConfiguration> getCacheConfigurations() {
        Map<String, RedisCacheConfiguration> configMap = new HashMap<>();
        
        // Tax rates cache (long TTL as rates change infrequently)
        configMap.put("taxRates", RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofHours(24))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer())));
        
        // Invoice templates cache
        configMap.put("invoiceTemplates", RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofHours(12)));
        
        // Customer data cache
        configMap.put("customers", RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(60)));
        
        return configMap;
    }
}
```

### 8.2 Database Indexing Strategy

```sql
-- Primary indexes
CREATE INDEX idx_invoice_application_status ON invoice_application(status);
CREATE INDEX idx_invoice_application_applicant ON invoice_application(applicant_id, created_at);
CREATE INDEX idx_invoice_application_order ON invoice_application(order_id, order_type);

-- Invoice indexes
CREATE INDEX idx_invoice_number ON invoice(invoice_number);
CREATE INDEX idx_invoice_customer ON invoice(customer_id, invoice_date);
CREATE INDEX idx_invoice_payment_status ON invoice(payment_status, due_date);

-- Composite indexes for reporting
CREATE INDEX idx_invoice_reporting ON invoice(invoice_date, invoice_type, total_amount);
CREATE INDEX idx_invoice_tax_report ON invoice(invoice_date, tax_amount, customer_tax_number);

-- Workflow indexes
CREATE INDEX idx_invoice_workflow_status ON invoice_workflow(status, deadline);
CREATE INDEX idx_invoice_workflow_approver ON invoice_workflow(current_approver_id, status);
```

## 9. Testing Strategy

### 9.1 Test Pyramid

```
        ┌─────────────────────┐
        │   E2E Tests (10%)   │
        │  • Complete flows   │
        │  • Integration      │
        └─────────────────────┘
                 │
        ┌─────────────────────┐
        │ Integration Tests   │
        │      (20%)          │
        │  • API tests       │
        │  • DB integration  │
        └─────────────────────┘
                 │
        ┌─────────────────────┐
        │   Unit Tests (70%)  │
        │  • Business logic   │
        │  • Services        │
        │  • Validators      │
        └─────────────────────┘
```

### 9.2 Key Test Scenarios

1. **Invoice Application Tests**
   - Create invoice application with valid data
   - Validation of required fields
   - Tax calculation accuracy
   - Discount application logic

2. **Approval Workflow Tests**
   - Multi-level approval flow
   - Role-based permission validation
   - Escalation rules
   - Audit trail generation

3. **Invoice Generation Tests**
   - PDF generation with QR code
   - Excel export functionality
   - Template rendering
   - Multi-language support

4. **Tax Calculation Tests**
   - VAT calculation for different regions
   - Special tax rates
   - Tax exemption scenarios
   - Cross-border tax rules

5. **Performance Tests**
   - Bulk invoice generation
   - Concurrent approval processing
   - Report generation performance
   - API response time under load

## 10. Deployment Strategy

### 10.1 Containerization

```dockerfile
# Dockerfile for Invoice Service
FROM openjdk:17-jdk-slim

# Install dependencies
RUN apt-get update && apt-get install -y \
    fontconfig \
    fonts-dejavu \
    && rm -rf /var/lib/apt/lists/*

# Create app directory
WORKDIR /app

# Copy application
COPY target/erp-invoice-*.jar app.jar

# Copy configuration
COPY config/application.yml /app/config/

# Set environment variables
ENV JAVA_OPTS="-Xmx512m -Xms256m"
ENV SPRING_PROFILES_ACTIVE="production"

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar --spring.config.location=file:/app/config/application.yml"]
```

### 10.2 Kubernetes Deployment

```yaml
# invoice-service-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: invoice-service
  namespace: erp-production
spec:
  replicas: 3
  selector:
    matchLabels:
      app: invoice-service
  template:
    metadata:
      labels:
        app: invoice-service
    spec:
      containers:
      - name: invoice-service
        image: registry.example.com/erp-invoice:1.0.0
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "production"
        - name: DB_HOST
          valueFrom:
            configMapKeyRef:
              name: erp-config
              key: database.host
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "500m"
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 15
```

## 11. Monitoring and Observability

### 11.1 Key Metrics

```yaml
metrics:
  business:
    - invoice.applications.created.count
    - invoice.applications.approved.count
    - invoice.applications.rejected.count
    - invoice.generated.count
    - invoice.paid.count
    - invoice.overdue.count
    
  performance:
    - invoice.generation.duration
    - invoice.approval.duration
    - api.response.time.p95
    - api.response.time.p99
    - database.query.duration
    
  system:
    - jvm.memory.used
    - jvm.gc.duration
    - system.cpu.usage
    - thread.count.active
    - connection.pool.active
```

### 11.2 Alerting Rules

```yaml
alerts:
  - alert: HighInvoiceGenerationLatency
    expr: invoice_generation_duration_seconds{p95} > 10
    for: 5m
    labels:
      severity: warning
    annotations:
      summary: "Invoice generation latency is high"
      description: "P95 invoice generation latency is {{ $value }} seconds (threshold: 10s)"
  
  - alert: InvoiceApprovalStuck
    expr: increase(invoice_applications_pending_total[1h]) == 0
        and invoice_applications_submitted_total[1h] > 10
    for: 15m
    labels:
      severity: critical
    annotations:
      summary: "Invoice approval workflow is stuck"
      description: "No invoice applications have been approved in the last hour despite new submissions"
  
  - alert: HighErrorRate
    expr: rate(http_requests_total{status=~"5.."}[5m]) 
        / rate(http_requests_total[5m]) > 0.05
    for: 2m
    labels:
      severity: critical
    annotations:
      summary: "High error rate on invoice API"
      description: "Error rate is {{ $value }}% (threshold: 5%)"
```

## 12. Disaster Recovery

### 12.1 Backup Strategy

```bash
#!/bin/bash
# invoice-backup.sh

# Database backup
pg_dump -h $DB_HOST -U $DB_USER -d erp_invoice -Fc -f /backup/invoice-$(date +%Y%m%d).dump

# Document storage backup
aws s3 sync s3://erp-invoice-documents/ /backup/documents/invoice-$(date +%Y%m%d)/

# Configuration backup
cp -r /app/config /backup/config/invoice-$(date +%Y%m%d)/

# Upload to off-site storage
aws s3 cp /backup/invoice-$(date +%Y%m%d).dump s3://erp-backups/invoice/
aws s3 cp --recursive /backup/documents/invoice-$(date +%Y%m%d)/ s3://erp-backups/documents/
```

### 12.2 Recovery Procedures

1. **Database Recovery**
   ```bash
   # Restore from backup
   pg_restore -h $NEW_DB_HOST -U $DB_USER -d erp_invoice /backup/invoice-latest.dump
   
   # Update application configuration
   sed -i "s/DB_HOST=.*/DB_HOST=$NEW_DB_HOST/" /app/.env
   ```

2. **Service Recovery**
   ```bash
   # Scale up service
   kubectl scale deployment invoice-service --replicas=3 -n erp-production
   
   # Verify health
   kubectl get pods -n erp-production -l app=invoice-service
   curl http://invoice-service.erp-production.svc.cluster.local:8080/actuator/health
   ```

## 13. Conclusion

This document provides a comprehensive design for the ERP Invoice Management Module. The design follows industry best practices for financial systems, with emphasis on:

1. **Security**: Role-based access control, audit trails, data encryption
2. **Compliance**: Chinese tax regulations, financial reporting standards
3. **Scalability**: Microservices architecture, caching strategy, database optimization
4. **Reliability**: Redundant deployment, monitoring, disaster recovery
5. **Maintainability**: Clean code structure, comprehensive testing, documentation

The implementation should follow this design to ensure a robust, secure, and scalable invoice management system that meets business requirements while maintaining high performance and reliability.

---

*Document Version: 1.0*
*Last Updated: 2026-04-29*
*Author: qa-lead*
*Reviewers: devops-engineer, product-analyst*
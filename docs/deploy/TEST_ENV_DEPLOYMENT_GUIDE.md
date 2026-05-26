# AI-Ready Test Environment Deployment Guide

## Overview
This guide describes the optimized deployment pipeline for the AI-Ready test environment, including health checks, rollback mechanisms, and monitoring integration.

## Deployment Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    GitHub Actions Pipeline                   │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │  Pre-Deploy  │  │    Build     │  │     Test     │      │
│  │   Checks     │→ │   Services    │→ │   Integration│      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
│         │                   │                  │            │
│         ▼                   ▼                  ▼            │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │  Deploy to   │  │Verify Deploy │  │ Post-Deploy  │      │
│  │   Test Env   │  │   deployment │  │  Monitoring  │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
│         │                   │                  │            │
│         ▼                   ▼                  ▼            │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │ Rollback on  │  │   Deploy     │  │  Metrics     │      │
│  │   Failure    │  │ Notifications│  │   Report     │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## Deployment Steps

### 1. Pre-Deployment Checks
- Validate configuration files
- Check Docker and Docker Compose availability
- Run security scans
- Validate Docker Compose syntax

### 2. Build Phase
- Build all Docker images
- Verify image builds
- Upload artifacts

### 3. Integration Tests
- Run integration test suite
- Verify database connections
- Test message queue functionality
- Report test results

### 4. Deploy to Test Environment
Execute the optimized deployment script:
```bash
./backend/infrastructure/docker/scripts/deploy-test-optimized.sh
```

The script performs:
- Health checks on infrastructure services
- Health checks on application services
- Automated rollback on failure
- Deployment logging and reporting

### 5. Verify Deployment
- Run smoke tests
- Verify service readiness
- Monitor deployment stability

### 6. Post-Deploy Monitoring
- Collect deployment metrics
- Send health status to monitoring systems
- Update deployment dashboards

## Health Check Configuration

### Container Health Checks
```yaml
healthcheck:
  test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
  interval: 30s
  timeout: 10s
  retries: 3
  start_period: 60s
```

### Service Health Checks
- **PostgreSQL**: Connection timeout test
- **Redis**: PING command response
- **Prometheus**: Health endpoint check
- **Grafana**: API health endpoint
- **API Gateway**: Actuator health endpoint

### Automated Health Check Script
The deployment script includes an automated health check mechanism:
- Checks container status
- Verifies service endpoints
- Retries on failure with exponential backoff
- Triggers rollback on persistent failure

## Rollback Mechanism

### Automatic Rollback
If any step fails during deployment:
1. Stop all services
2. Restore previous version (if available)
3. Restart critical services
4. Send rollback notification

### Manual Rollback
```bash
cd backend/infrastructure/docker
docker-compose -f docker-compose-test.yml down
# Restore previous version
docker-compose -f docker-compose-test.yml up -d
```

## Monitoring Integration

### Prometheus Metrics
```yaml
# Deployment metrics
test_deployment_duration_seconds
test_deployment_status
test_deployment_health_score
```

### Grafana Dashboard
The deployment dashboard shows:
- Deployment history
- Health scores over time
- Service status
- Performance metrics

## Environment Variables

| Variable | Description | Required |
|----------|-------------|----------|
| `VERSION` | Deployment version (commit SHA) | Yes |
| `DEPLOYMENT_TIMESTAMP` | Deployment timestamp | Yes |
| `KUBECONFIG_TEST` | Kubernetes config for test env | Yes |
| `K8S_CONTEXT_TEST` | Kubernetes context for test env | Yes |
| `WECOM_WEBHOOK_URL` | WeCom webhook for notifications | Yes |

## Deployment Customization

### Add Custom Health Checks
Add new health checks in the deployment script:
```bash
check_custom_service() {
    local service="$1"
    local endpoint="$2"
    
    # Your custom health check logic
    # Return 0 on success, 1 on failure
}
```

### Modify Rollback Logic
Customize rollback behavior in the `rollback()` function:
```bash
rollback() {
    # Your custom rollback logic
}
```

## Troubleshooting

### Deployment Timeout
- Increase `HEALTH_CHECK_TIMEOUT` in deployment script
- Check network connectivity
- Verify resource availability

### Health Check Failures
- Check service logs: `docker-compose logs <service>`
- Verify service configuration
- Check resource usage

### Rollback Issues
- Verify `.previous_tag` file exists
- Check service configuration
- Review deployment logs

## Best Practices

1. **Always run pre-deployment checks**
2. **Monitor health during deployment**
3. **Test rollback procedure regularly**
4. **Update health check endpoints**
5. **Review deployment logs regularly**
6. **Keep deployment scripts updated**

## Related Files

- `backend/infrastructure/docker/docker-compose-test.yml` - Test environment configuration
- `backend/infrastructure/docker/scripts/deploy-test-optimized.sh` - Optimized deployment script
- `backend/infrastructure/docker/scripts/start-test-env.sh` - Basic start script
- `backend/infrastructure/docker/scripts/stop-test-env.sh` - Stop script
- `.github/workflows/test-env-ci-cd.yml` - CI/CD pipeline configuration

## Support

For issues or questions:
1. Check deployment logs in `backend/infrastructure/docker/deploy/logs/`
2. Review service logs: `docker-compose logs <service>`
3. Check monitoring dashboard: http://localhost:3000
4. Contact DevOps team

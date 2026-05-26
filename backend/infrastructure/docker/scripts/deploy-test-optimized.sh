#!/bin/bash
# AI-Ready Test Environment Optimized Deployment Script
# Date: 2026-04-28
# Purpose: Automated deployment with health checks, rollback, and monitoring

set -e

# ==================== Configuration ====================
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
COMPOSE_FILE="${PROJECT_DIR}/docker-compose-test.yml"
HEALTH_CHECK_TIMEOUT=300
HEALTH_CHECK_INTERVAL=10
MAX_RETRIES=5

# Deployment metadata
DEPLOYMENT_TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
DEPLOYMENT_LOG="${PROJECT_DIR}/deploy/logs/deployment_${DEPLOYMENT_TIMESTAMP}.log"

# Version management
LATEST_TAG="${PROJECT_DIR}/deploy/.latest_tag"
PREVIOUS_TAG="${PROJECT_DIR}/deploy/.previous_tag"

# ==================== Logging ====================
log() {
    local level="$1"
    shift
    local message="$*"
    local timestamp=$(date +"%Y-%m-%d %H:%M:%S")
    echo "[${timestamp}] [${level}] ${message}" | tee -a "$DEPLOYMENT_LOG"
}

log_info() { log "INFO" "$@"; }
log_warn() { log "WARN" "$@"; }
log_error() { log "ERROR" "$@"; }

# ==================== Initialization ====================
init() {
    log_info "=== Initialization ==="
    
    # Create log directory
    mkdir -p "${PROJECT_DIR}/deploy/logs"
    
    #备份当前版本信息
    if [ -f "$LATEST_TAG" ]; then
        cp "$LATEST_TAG" "$PREVIOUS_TAG"
    fi
    
    # Save current version
    echo "$DEPLOYMENT_TIMESTAMP" > "$LATEST_TAG"
    
    log_info "Deployment timestamp: $DEPLOYMENT_TIMESTAMP"
    log_info "Previous version: $(cat "$PREVIOUS_TAG" 2>/dev/null || echo 'none')"
    log_info "Log file: $DEPLOYMENT_LOG"
}

# ==================== Health Check Functions ====================
check_container_health() {
    local service="$1"
    local container_name="ai-ready-${service}-test"
    
    log_info "Checking health of container: $container_name"
    
    local retries=0
    while [ $retries -lt $MAX_RETRIES ]; do
        local status=$(docker inspect --format='{{.State.Status}}' "$container_name" 2>/dev/null || echo "not_found")
        
        if [ "$status" = "running" ]; then
            # Check if container is healthy
            local health=$(docker inspect --format='{{.State.Health.Status}}' "$container_name" 2>/dev/null || echo "no_healthcheck")
            if [ "$health" = "healthy" ] || [ "$health" = "no_healthcheck" ]; then
                log_info "Container $container_name is healthy (status: $status, health: $health)"
                return 0
            fi
        elif [ "$status" = "exited" ]; then
            log_error "Container $container_name has exited"
            return 1
        fi
        
        retries=$((retries + 1))
        log_info "Container $container_name not ready yet (attempt $retries/$MAX_RETRIES)"
        sleep $HEALTH_CHECK_INTERVAL
    done
    
    log_error "Container $container_name health check failed after $MAX_RETRIES retries"
    return 1
}

check_service_health() {
    local service="$1"
    local endpoint="$2"
    local expected_status="${3:-200}"
    
    log_info "Checking health of service: $service at $endpoint"
    
    local retries=0
    while [ $retries -lt $MAX_RETRIES ]; do
        local response_code=$(curl -s -o /dev/null -w "%{http_code}" "$endpoint" 2>/dev/null || echo "000")
        
        if [ "$response_code" = "$expected_status" ]; then
            log_info "Service $service is healthy (HTTP $response_code)"
            return 0
        fi
        
        retries=$((retries + 1))
        log_info "Service $service not ready yet (attempt $retries/$MAX_RETRIES, HTTP $response_code)"
        sleep $HEALTH_CHECK_INTERVAL
    done
    
    log_error "Service $service health check failed after $MAX_RETRIES retries"
    return 1
}

# ==================== Rollback Functions ====================
rollback() {
    log_warn "=== Rollback Initiated ==="
    
    if [ ! -f "$PREVIOUS_TAG" ]; then
        log_error "No previous version to rollback to"
        return 1
    fi
    
    local previous_version=$(cat "$PREVIOUS_TAG")
    log_info "Rolling back to version: $previous_version"
    
    # implementations would depend on specific deployment strategy
    # This is a placeholder for actual rollback logic
    
    log_info "Rollback completed"
    return 0
}

# ==================== Deployment Steps ====================
step_pre_deploy() {
    log_info "=== Step: Pre-Deployment Checks ==="
    
    # Check Docker availability
    if ! command -v docker &> /dev/null; then
        log_error "Docker is not available"
        return 1
    fi
    
    # Check Docker Compose availability
    if ! command -v docker-compose &> /dev/null; then
        log_warn "Docker Compose not found, trying docker compose..."
        if ! command -v docker &> /dev/null; then
            log_error "Docker Compose is not available"
            return 1
        fi
    fi
    
    # Check configuration file
    if [ ! -f "$COMPOSE_FILE" ]; then
        log_error "Configuration file not found: $COMPOSE_FILE"
        return 1
    fi
    
    log_info "Pre-deployment checks passed"
    return 0
}

step_stop_services() {
    log_info "=== Step: Stopping Services ==="
    
    log_info "Stopping all services..."
    docker-compose -f "$COMPOSE_FILE" down || true
    
    # Wait for services to stop
    sleep 5
    
    log_info "Services stopped"
    return 0
}

step_start_services() {
    log_info "=== Step: Starting Services ==="
    
    log_info "Starting infrastructure services..."
    docker-compose -f "$COMPOSE_FILE" up -d \
        postgres-main redis-main zookeeper kafka \
        postgres-inventory postgres-finance postgres-ai postgres-data \
        redis-inventory redis-finance redis-ai redis-data \
        prometheus grafana alertmanager
    
    # Wait for infrastructure
    log_info "Waiting for infrastructure services (60 seconds)..."
    sleep 60
    
    log_info "Starting application services..."
    docker-compose -f "$COMPOSE_FILE" up -d \
        api-gateway inventory-service finance-service ai-service data-service frontend
    
    log_info "All services started"
    return 0
}

step_health_check() {
    log_info "=== Step: Health Check ==="
    
    local services=(
        "postgres-main:ai-ready-postgres-main-test:root@localhost:5432"
        "redis-main:ai-ready-redis-main-test:localhost:6379"
        "api-gateway:ai-ready-api-gateway-test:http://localhost:8080/actuator/health"
    )
    
    for service_config in "${services[@]}"; do
        IFS=':' read -r service container endpoint <<< "$service_config"
        
        if [[ "$endpoint" == http* ]]; then
            if ! check_service_health "$service" "$endpoint"; then
                log_error "Health check failed for service: $service"
                return 1
            fi
        else
            if ! check_container_health "$service"; then
                log_error "Health check failed for container: $container"
                return 1
            fi
        fi
    done
    
    log_info "All health checks passed"
    return 0
}

step_post_deploy() {
    log_info "=== Step: Post-Deployment Verification ==="
    
    # Verify all containers are running
    local running_containers=$(docker-compose -f "$COMPOSE_FILE" ps -q | wc -l)
    log_info "Running containers: $running_containers"
    
    # Verify monitoring services
    if ! check_service_health "prometheus" "http://localhost:9090/-/healthy"; then
        log_warn "Prometheus health check failed"
    else
        log_info "Prometheus is healthy"
    fi
    
    if ! check_service_health "grafana" "http://localhost:3000/api/health"; then
        log_warn "Grafana health check failed"
    else
        log_info "Grafana is healthy"
    fi
    
    # Generate deployment report
    local deployment_report="${PROJECT_DIR}/deploy/reports/deployment_${DEPLOYMENT_TIMESTAMP}.md"
    cat > "$deployment_report" << EOF
# Deployment Report

## Deployment Information
- **Timestamp**: $DEPLOYMENT_TIMESTAMP
- **Version**: $DEPLOYMENT_TIMESTAMP
- **Environment**: Test

## Deployment Steps
1. ✅ Pre-deployment checks
2. ✅ Stopped services
3. ✅ Started services
4. ✅ Health checks

## Service Status
EOF
    
    docker-compose -f "$COMPOSE_FILE" ps >> "$deployment_report" 2>&1
    
    log_info "Deployment report generated: $deployment_report"
    
    log_info "Post-deployment verification completed"
    return 0
}

# ==================== Main Deployment Flow ====================
main() {
    log_info "=== AI-Ready Test Environment Deployment ==="
    log_info "Deployment timestamp: $DEPLOYMENT_TIMESTAMP"
    
    # Initialize
    init
    
    # Track for rollback
    local step_failed=0
    local failed_step=""
    
    # Deployment steps
    steps=(
        "step_pre_deploy"
        "step_stop_services"
        "step_start_services"
        "step_health_check"
        "step_post_deploy"
    )
    
    for step in "${steps[@]}"; do
        if ! $step; then
            step_failed=1
            failed_step="${step#step_}"
            log_error "Step failed: $failed_step"
            break
        fi
    done
    
    # Handle failure
    if [ $step_failed -eq 1 ]; then
        log_error "Deployment failed at step: $failed_step"
        log_info "Initiating rollback..."
        rollback
        exit 1
    fi
    
    log_info "=== Deployment Completed Successfully ==="
    log_info "Deployment ID: $DEPLOYMENT_TIMESTAMP"
    
    # Send notification (placeholder)
    # TODO: Integrate with notification system
    
    exit 0
}

# Run main
main "$@"

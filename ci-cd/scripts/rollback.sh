#!/bin/bash
# AI-Ready Deployment Rollback Script
# Usage: ./rollback.sh [environment] [revision]
# Example: ./rollback.sh prod 1 (rollback to previous version)
# Example: ./rollback.sh staging 3 (rollback to revision 3)

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default values
ENVIRONMENT="${1:-staging}"
REVISION="${2:-1}"
NAMESPACE="ai-ready-${ENVIRONMENT}"
DEPLOYMENT_NAME="ai-ready-api"

log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

# Validate environment
validate_environment() {
    case "$ENVIRONMENT" in
        dev|staging|prod)
            log_info "Environment validated: $ENVIRONMENT"
            ;;
        *)
            log_error "Invalid environment: $ENVIRONMENT. Valid options: dev, staging, prod"
            exit 1
            ;;
    esac
}

# Check kubectl connection
check_kubectl() {
    log_info "Checking kubectl connection..."
    if ! kubectl cluster-info &>/dev/null; then
        log_error "kubectl cannot connect to cluster. Please check your kubeconfig."
        exit 1
    fi
    log_success "kubectl connection OK"
}

# Get deployment history
get_deployment_history() {
    log_info "Getting deployment history for $DEPLOYMENT_NAME in $NAMESPACE..."
    kubectl rollout history deployment/$DEPLOYMENT_NAME -n $NAMESPACE
}

# Get current deployment status
get_current_status() {
    log_info "Current deployment status:"
    kubectl get deployment $DEPLOYMENT_NAME -n $NAMESPACE -o wide
    kubectl get pods -n $NAMESPACE -l app=$DEPLOYMENT_NAME -o wide
}

# Pre-rollback checks
pre_rollback_checks() {
    log_info "=== Pre-Rollback Checks ==="
    
    # Check deployment exists
    if ! kubectl get deployment $DEPLOYMENT_NAME -n $NAMESPACE &>/dev/null; then
        log_error "Deployment $DEPLOYMENT_NAME does not exist in namespace $NAMESPACE"
        exit 1
    fi
    
    # Check deployment health
    READY_REPLICAS=$(kubectl get deployment $DEPLOYMENT_NAME -n $NAMESPACE -o jsonpath='{.status.readyReplicas}')
    DESIRED_REPLICAS=$(kubectl get deployment $DEPLOYMENT_NAME -n $NAMESPACE -o jsonpath='{.spec.replicas}')
    
    if [ "$READY_REPLICAS" != "$DESIRED_REPLICAS" ]; then
        log_warn "Deployment is not fully healthy: Ready=$READY_REPLICAS, Desired=$DESIRED_REPLICAS"
    fi
    
    # Backup current deployment state
    log_info "Backing up current deployment state..."
    BACKUP_DIR="$PROJECT_ROOT/backups/rollback_$(date +%Y%m%d_%H%M%S)"
    mkdir -p "$BACKUP_DIR"
    
    kubectl get deployment $DEPLOYMENT_NAME -n $NAMESPACE -o yaml > "$BACKUP_DIR/deployment.yaml"
    kubectl get svc -n $NAMESPACE -o yaml > "$BACKUP_DIR/service.yaml"
    kubectl get configmap -n $NAMESPACE -o yaml > "$BACKUP_DIR/configmap.yaml"
    kubectl get pods -n $NAMESPACE -l app=$DEPLOYMENT_NAME -o yaml > "$BACKUP_DIR/pods.yaml"
    
    log_success "Backup saved to: $BACKUP_DIR"
}

# Perform rollback
perform_rollback() {
    log_info "=== Performing Rollback ==="
    
    if [ "$REVISION" == "1" ]; then
        log_info "Rolling back to previous revision..."
        kubectl rollout undo deployment/$DEPLOYMENT_NAME -n $NAMESPACE
    else
        log_info "Rolling back to revision $REVISION..."
        kubectl rollout undo deployment/$DEPLOYMENT_NAME -n $NAMESPACE --to-revision=$REVISION
    fi
    
    # Wait for rollout to complete
    log_info "Waiting for rollout to complete..."
    kubectl rollout status deployment/$DEPLOYMENT_NAME -n $NAMESPACE --timeout=300s
    
    log_success "Rollback completed!"
}

# Post-rollback verification
post_rollback_verification() {
    log_info "=== Post-Rollback Verification ==="
    
    # Check pod status
    kubectl get pods -n $NAMESPACE -l app=$DEPLOYMENT_NAME -o wide
    
    # Wait for pods to be ready
    log_info "Waiting for pods to be ready..."
    kubectl wait --for=condition=ready pods -l app=$DEPLOYMENT_NAME -n $NAMESPACE --timeout=120s
    
    # Health check
    log_info "Performing health check..."
    
    # Get first pod name
    POD_NAME=$(kubectl get pods -n $NAMESPACE -l app=$DEPLOYMENT_NAME -o jsonpath='{.items[0].metadata.name}')
    
    for i in {1..10}; do
        HEALTH_STATUS=$(kubectl exec -n $NAMESPACE $POD_NAME -- curl -s http://localhost:8080/actuator/health 2>/dev/null | grep -o '"status":"[^"]*' | cut -d'"' -f4 || echo "unknown")
        
        if [ "$HEALTH_STATUS" == "UP" ]; then
            log_success "Health check passed: $HEALTH_STATUS"
            break
        else
            log_warn "Health check attempt $i: $HEALTH_STATUS"
            sleep 10
        fi
    done
    
    # Show rollback history
    log_info "Deployment history after rollback:"
    kubectl rollout history deployment/$DEPLOYMENT_NAME -n $NAMESPACE
    
    # Show current revision
    CURRENT_REVISION=$(kubectl get deployment $DEPLOYMENT_NAME -n $NAMESPACE -o jsonpath='{.metadata.annotations.deployment\.kubernetes\.io/revision}')
    log_success "Current revision: $CURRENT_REVISION"
}

# Send notification
send_notification() {
    # Notification webhook (can be customized)
    WEBHOOK_URL="${ROLLBACK_WEBHOOK_URL:-}"
    
    if [ -n "$WEBHOOK_URL" ]; then
        log_info "Sending rollback notification..."
        
        curl -X POST "$WEBHOOK_URL" \
            -H "Content-Type: application/json" \
            -d "{\"text\":\"🔄 AI-Ready Rollback: Environment=$ENVIRONMENT, Revision=$REVISION, Time=$(date -u +"%Y-%m-%dT%H:%M:%SZ\")\"}" \
            || log_warn "Failed to send notification"
    fi
}

# Main execution
main() {
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}  AI-Ready Deployment Rollback Tool${NC}"
    echo -e "${BLUE}========================================${NC}"
    echo ""
    
    validate_environment
    check_kubectl
    
    echo ""
    get_deployment_history
    echo ""
    get_current_status
    echo ""
    
    # Confirmation for production
    if [ "$ENVIRONMENT" == "prod" ]; then
        log_warn "⚠️  PRODUCTION ENVIRONMENT DETECTED!"
        echo ""
        read -p "Are you sure you want to rollback production? (yes/no): " CONFIRM
        
        if [ "$CONFIRM" != "yes" ]; then
            log_info "Rollback cancelled."
            exit 0
        fi
    fi
    
    pre_rollback_checks
    echo ""
    
    perform_rollback
    echo ""
    
    post_rollback_verification
    echo ""
    
    send_notification
    
    echo ""
    echo -e "${GREEN}========================================${NC}"
    echo -e "${GREEN}  Rollback completed successfully!${NC}"
    echo -e "${GREEN}========================================${NC}"
}

main
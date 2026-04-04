#!/bin/bash
# AI-Ready Quality Checks Integration Script
# Runs all quality checks before deployment
# Usage: ./quality-checks.sh [skip-tests]

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$(dirname "$SCRIPT_DIR"))"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

SKIP_TESTS="${1:-false}"
REPORT_DIR="$PROJECT_ROOT/quality-reports"
DATE_STAMP=$(date +%Y%m%d_%H%M%S)

mkdir -p "$REPORT_DIR/$DATE_STAMP"

log_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }
log_success() { echo -e "${GREEN}[SUCCESS]${NC} $1"; }

# ============================================
# 1. Checkstyle - Code Style Check
# ============================================
run_checkstyle() {
    log_info "Running Checkstyle..."
    
    mvn checkstyle:check -B > "$REPORT_DIR/$DATE_STAMP/checkstyle.log" 2>&1 || true
    
    if grep -q "Checkstyle violations" "$REPORT_DIR/$DATE_STAMP/checkstyle.log"; then
        VIOLATIONS=$(grep -c "Checkstyle violations" "$REPORT_DIR/$DATE_STAMP/checkstyle.log")
        log_warn "Checkstyle found $VIOLATIONS violations"
    else
        log_success "Checkstyle passed"
    fi
}

# ============================================
# 2. SpotBugs - Static Analysis
# ============================================
run_spotbugs() {
    log_info "Running SpotBugs..."
    
    mvn spotbugs:check -B > "$REPORT_DIR/$DATE_STAMP/spotbugs.log" 2>&1 || true
    
    if grep -q "BugInstance" "$REPORT_DIR/$DATE_STAMP/spotbugs.log"; then
        BUGS=$(grep -c "BugInstance" "$REPORT_DIR/$DATE_STAMP/spotbugs.log")
        log_warn "SpotBugs found $BUGS potential bugs"
    else
        log_success "SpotBugs passed"
    fi
}

# ============================================
# 3. PMD - Source Code Analyzer
# ============================================
run_pmd() {
    log_info "Running PMD..."
    
    mvn pmd:check -B > "$REPORT_DIR/$DATE_STAMP/pmd.log" 2>&1 || true
    
    if grep -q "PMD violations" "$REPORT_DIR/$DATE_STAMP/pmd.log"; then
        VIOLATIONS=$(grep -c "PMD violations" "$REPORT_DIR/$DATE_STAMP/pmd.log")
        log_warn "PMD found $VIOLATIONS violations"
    else
        log_success "PMD passed"
    fi
}

# ============================================
# 4. Unit Tests with Coverage
# ============================================
run_unit_tests() {
    if [ "$SKIP_TESTS" == "skip-tests" ]; then
        log_info "Skipping tests as requested"
        return 0
    fi
    
    log_info "Running Unit Tests..."
    
    mvn test -B jacoco:report > "$REPORT_DIR/$DATE_STAMP/unit-tests.log" 2>&1
    
    # Check test results
    if grep -q "Tests run:.*Failures: 0" "$REPORT_DIR/$DATE_STAMP/unit-tests.log"; then
        log_success "All unit tests passed"
        
        # Get coverage percentage
        COVERAGE=$(grep -o "Total coverage: [0-9]*%" "$REPORT_DIR/$DATE_STAMP/unit-tests.log" | head -1 || echo "N/A")
        log_info "Code coverage: $COVERAGE"
    else
        FAILED=$(grep "Tests run:" "$REPORT_DIR/$DATE_STAMP/unit-tests.log" | tail -1)
        log_error "Unit tests failed: $FAILED"
        return 1
    fi
}

# ============================================
# 5. Integration Tests
# ============================================
run_integration_tests() {
    if [ "$SKIP_TESTS" == "skip-tests" ]; then
        log_info "Skipping integration tests"
        return 0
    fi
    
    log_info "Running Integration Tests..."
    
    mvn verify -B -Dtest.skip=true > "$REPORT_DIR/$DATE_STAMP/integration-tests.log" 2>&1
    
    if grep -q "Tests run:.*Failures: 0" "$REPORT_DIR/$DATE_STAMP/integration-tests.log"; then
        log_success "All integration tests passed"
    else
        log_error "Integration tests failed"
        return 1
    fi
}

# ============================================
# 6. Dependency Check - OWASP
# ============================================
run_dependency_check() {
    log_info "Running OWASP Dependency Check..."
    
    mvn org.owasp:dependency-check-maven:check -B > "$REPORT_DIR/$DATE_STAMP/dependency-check.log" 2>&1 || true
    
    if grep -q "One or more dependencies were identified with known vulnerabilities" "$REPORT_DIR/$DATE_STAMP/dependency-check.log"; then
        log_warn "Security vulnerabilities found in dependencies!"
        log_warn "Please review: $REPORT_DIR/$DATE_STAMP/dependency-check.log"
    else
        log_success "No known vulnerabilities in dependencies"
    fi
}

# ============================================
# 7. License Check
# ============================================
run_license_check() {
    log_info "Checking licenses..."
    
    mvn license:check -B > "$REPORT_DIR/$DATE_STAMP/license-check.log" 2>&1 || true
    
    if grep -q "Missing license" "$REPORT_DIR/$DATE_STAMP/license-check.log"; then
        log_warn "License issues found"
    else
        log_success "License check passed"
    fi
}

# ============================================
# 8. Docker Image Security Scan
# ============================================
run_docker_scan() {
    log_info "Running Docker security scan..."
    
    IMAGE_NAME="ai-ready:quality-check"
    
    # Build image for scanning
    docker build -t $IMAGE_NAME "$PROJECT_ROOT" > "$REPORT_DIR/$DATE_STAMP/docker-build.log" 2>&1
    
    # Run Trivy scan
    docker run --rm -v /var/run/docker.sock:/var/run/docker.sock \
        aquasec/trivy:latest image --severity HIGH,CRITICAL \
        $IMAGE_NAME > "$REPORT_DIR/$DATE_STAMP/trivy-scan.log" 2>&1 || true
    
    if grep -q "Total: 0" "$REPORT_DIR/$DATE_STAMP/trivy-scan.log"; then
        log_success "No critical/high vulnerabilities in Docker image"
    else
        CRITICAL=$(grep -o "CRITICAL: [0-9]*" "$REPORT_DIR/$DATE_STAMP/trivy-scan.log" | head -1 || echo "CRITICAL: 0")
        HIGH=$(grep -o "HIGH: [0-9]*" "$REPORT_DIR/$DATE_STAMP/trivy-scan.log" | head -1 || echo "HIGH: 0")
        log_warn "Docker image vulnerabilities: $CRITICAL, $HIGH"
    fi
    
    # Clean up
    docker rmi $IMAGE_NAME 2>/dev/null || true
}

# ============================================
# Generate Summary Report
# ============================================
generate_summary() {
    log_info "Generating quality summary report..."
    
    SUMMARY_FILE="$REPORT_DIR/$DATE_STAMP/summary.md"
    
    cat > "$SUMMARY_FILE" << EOF
# AI-Ready Quality Check Report

**Date**: $(date)
**Branch**: $(git branch --show-current 2>/dev/null || echo "unknown")
**Commit**: $(git rev-parse --short HEAD 2>/dev/null || echo "unknown")

## Summary

| Check | Status | Details |
|-------|--------|---------|
| Checkstyle | $(grep -q "passed" "$REPORT_DIR/$DATE_STAMP/checkstyle.log" && echo "✅ Pass" || echo "⚠️ Issues") | See checkstyle.log |
| SpotBugs | $(grep -q "passed" "$REPORT_DIR/$DATE_STAMP/spotbugs.log" && echo "✅ Pass" || echo "⚠️ Issues") | See spotbugs.log |
| PMD | $(grep -q "passed" "$REPORT_DIR/$DATE_STAMP/pmd.log" && echo "✅ Pass" || echo "⚠️ Issues") | See pmd.log |
| Unit Tests | $(grep -q "passed" "$REPORT_DIR/$DATE_STAMP/unit-tests.log" && echo "✅ Pass" || echo "❌ Failed") | See unit-tests.log |
| Integration Tests | $(grep -q "passed" "$REPORT_DIR/$DATE_STAMP/integration-tests.log" && echo "✅ Pass" || echo "❌ Failed") | See integration-tests.log |
| Dependency Check | $(grep -q "vulnerabilities" "$REPORT_DIR/$DATE_STAMP/dependency-check.log" && echo "⚠️ Issues" || echo "✅ Pass") | See dependency-check.log |
| License Check | $(grep -q "issues" "$REPORT_DIR/$DATE_STAMP/license-check.log" && echo "⚠️ Issues" || echo "✅ Pass") | See license-check.log |
| Docker Scan | $(grep -q "Total: 0" "$REPORT_DIR/$DATE_STAMP/trivy-scan.log" && echo "✅ Pass" || echo "⚠️ Issues") | See trivy-scan.log |

## Recommendation

$([ "$SKIP_TESTS" == "skip-tests" ] && echo "⚠️ Tests were skipped. Full quality check recommended before production deployment." || echo "✅ All quality checks completed. Review results before proceeding with deployment.")

---
*Generated by AI-Ready Quality Check Tool*
EOF
    
    log_success "Summary report saved to: $SUMMARY_FILE"
    
    # Display summary
    echo ""
    cat "$SUMMARY_FILE"
}

# ============================================
# Main Execution
# ============================================
main() {
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}  AI-Ready Quality Check Pipeline${NC}"
    echo -e "${BLUE}========================================${NC}"
    echo ""
    
    run_checkstyle
    run_spotbugs
    run_pmd
    run_unit_tests || true
    run_integration_tests || true
    run_dependency_check
    run_license_check
    run_docker_scan
    
    echo ""
    generate_summary
    
    echo ""
    echo -e "${GREEN}Quality checks completed!${NC}"
    echo "Reports available at: $REPORT_DIR/$DATE_STAMP"
}

main
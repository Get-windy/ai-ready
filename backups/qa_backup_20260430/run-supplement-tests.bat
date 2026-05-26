@echo off
cd /d I:\AI-Ready\backend\erp\erp-purchase
echo Compiling and running new test files...
mvn test-compile -q
if %ERRORLEVEL% EQU 0 (
    echo Test compilation successful.
    echo Running PurchaseQuoteComparisonTest...
    mvn test -Dtest=PurchaseQuoteComparisonTest -q
    echo Running PurchaseContractServiceTest...
    mvn test -Dtest=PurchaseContractServiceTest -q
    echo Running PurchaseIntegrationTest...
    mvn test -Dtest=PurchaseIntegrationTest -q
    echo.
    echo Generating summary report...
    mvn test -q
) else (
    echo Test compilation failed.
)
echo Done.
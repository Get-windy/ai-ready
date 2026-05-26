@echo off
echo AI-Ready Performance Test Execution
echo ====================================
echo.
echo 1. Start Mock Service (in new terminal):
echo    python tests\performance\mock_services\mock_api_gateway.py
echo.
echo 2. Run Performance Tests (in another terminal):
echo    python tests\performance\api\performance_benchmark_test.py
echo.
echo 3. Or run full test suite:
echo    python tests\performance\run_performance_tests.py
echo.
echo 4. Reports will be generated in:
echo    tests\performance\reports\
echo.
pause

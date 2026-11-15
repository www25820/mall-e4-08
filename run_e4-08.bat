@echo off
setlocal

REM ============================================================
REM  E4-08 cyclic dependency - one-click runner
REM  Usage: double-click, or run in cmd
REM  Output: after-construction-injection test + coverage
REM ============================================================

set "MVN=C:\Program Files\JetBrains\IntelliJ IDEA 2024.2.2\plugins\maven\lib\maven3\bin\mvn.cmd"
set "PROJECT=C:\Users\root\Downloads\mall-master\mall-master"

if not exist "%MVN%" set "MVN=mvn"

cd /d "%PROJECT%"
if errorlevel 1 (
    echo [ERROR] project dir not found: %PROJECT%
    pause
    exit /b 1
)

set "EV=%PROJECT%\evidence-e4"
if exist "%EV%" rmdir /s /q "%EV%"
mkdir "%EV%"

echo ============================================================
echo  [1/2] Run E4-08 after test (expect 4 passed)
echo ============================================================
call "%MVN%" -B -pl mall-admin -am -DskipTests=false -Dtest=UmsAdminCacheServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test > "%EV%\1_test_after.log" 2>&1
set "TEST_RC=%errorlevel%"
echo test exit code: %TEST_RC%
echo test log saved: evidence-e4\1_test_after.log

echo.
echo ============================================================
echo  [2/2] Generate JaCoCo coverage report
echo ============================================================
call "%MVN%" -B -pl mall-admin -am -DskipTests=false -Dtest=UmsAdminCacheServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false org.jacoco:jacoco-maven-plugin:0.8.13:prepare-agent test org.jacoco:jacoco-maven-plugin:0.8.13:report > "%EV%\2_coverage.log" 2>&1
set "COV_RC=%errorlevel%"
echo coverage exit code: %COV_RC%
echo coverage log saved: evidence-e4\2_coverage.log

echo.
echo ============================================================
echo  Summary
echo ============================================================
echo  test exit code:     %TEST_RC%   (0 = all passed)
echo  coverage exit code: %COV_RC%
echo.
echo  Evidence files:
echo    %EV%\1_test_after.log
echo    %EV%\2_coverage.log
echo.
echo  Coverage report (open in browser):
echo    %PROJECT%\mall-admin\target\site\jacoco\index.html
echo ============================================================

pause
endlocal

@echo off
echo ==========================================================
echo Starting Smart Traffic Police Management System...
echo ==========================================================
mvn javafx:run
if %ERRORLEVEL% NEQ 0 (
    echo Fallback to exec:java launcher...
    mvn compile exec:java -Dexec.mainClass="com.smarttraffic.SmartTrafficAppLauncher"
)
pause

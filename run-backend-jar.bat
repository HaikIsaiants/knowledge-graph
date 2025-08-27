@echo off
setlocal

set JAVA_HOME=%~dp0tools\jdk-17
set PATH=%JAVA_HOME%\bin;%PATH%

echo Building backend JAR...
cd backend
call ..\tools\maven\bin\mvn.cmd package -Dmaven.test.skip=true

if %ERRORLEVEL% NEQ 0 (
    echo Build failed!
    exit /b 1
)

echo Starting Spring Boot backend...
"%JAVA_HOME%\bin\java" -Dspring.profiles.active=local -jar target\knowledge-graph-backend-0.0.1-SNAPSHOT.jar

endlocal
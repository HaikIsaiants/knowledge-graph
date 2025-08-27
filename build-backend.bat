@echo off
set JAVA_HOME=%~dp0tools\jdk-17
set PATH=%JAVA_HOME%\bin;%PATH%
cd backend
..\tools\maven\bin\mvn package -Dmaven.test.skip=true
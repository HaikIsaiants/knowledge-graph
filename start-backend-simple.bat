@echo off
set JAVA_HOME=%~dp0tools\jdk-17
set PATH=%JAVA_HOME%\bin;%PATH%
cd backend
..\tools\maven\bin\mvn.cmd spring-boot:run -Dspring-boot.run.profiles=local -Dmaven.test.skip=true
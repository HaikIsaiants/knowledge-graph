@echo off
echo ==========================================
echo Knowledge Graph Backend Setup (Portable)
echo ==========================================
echo.

set TOOLS_DIR=%~dp0tools
set JAVA_DIR=%TOOLS_DIR%\jdk-17
set MAVEN_DIR=%TOOLS_DIR%\maven

echo Creating tools directory...
if not exist "%TOOLS_DIR%" mkdir "%TOOLS_DIR%"

echo.
echo Downloading OpenJDK 17...
if not exist "%JAVA_DIR%" (
    echo Downloading JDK from Microsoft Build of OpenJDK...
    curl -L -o "%TOOLS_DIR%\jdk.zip" "https://aka.ms/download-jdk/microsoft-jdk-17-windows-x64.zip"
    
    if exist "%TOOLS_DIR%\jdk.zip" (
        echo Extracting JDK...
        powershell -Command "Expand-Archive -Path '%TOOLS_DIR%\jdk.zip' -DestinationPath '%TOOLS_DIR%' -Force"
        
        REM Find the extracted JDK directory
        for /d %%d in ("%TOOLS_DIR%\jdk-17*") do (
            move "%%d" "%JAVA_DIR%" 2>nul
        )
        del "%TOOLS_DIR%\jdk.zip"
    ) else (
        echo Failed to download JDK!
        pause
        exit /b 1
    )
) else (
    echo JDK already downloaded.
)

echo.
echo Downloading Apache Maven...
if not exist "%MAVEN_DIR%" (
    echo Downloading Maven 3.9.9...
    curl -L -o "%TOOLS_DIR%\maven.zip" "https://dlcdn.apache.org/maven/maven-3/3.9.9/binaries/apache-maven-3.9.9-bin.zip"
    
    if exist "%TOOLS_DIR%\maven.zip" (
        echo Extracting Maven...
        powershell -Command "Expand-Archive -Path '%TOOLS_DIR%\maven.zip' -DestinationPath '%TOOLS_DIR%' -Force"
        move "%TOOLS_DIR%\apache-maven-3.9.9" "%MAVEN_DIR%"
        del "%TOOLS_DIR%\maven.zip"
    ) else (
        echo Failed to download Maven!
        pause
        exit /b 1
    )
) else (
    echo Maven already downloaded.
)

echo.
echo Setting up environment variables for this session...
set JAVA_HOME=%JAVA_DIR%
set PATH=%JAVA_DIR%\bin;%MAVEN_DIR%\bin;%PATH%

echo.
echo Verifying installations...
echo.
"%JAVA_DIR%\bin\java" -version 2>&1
echo.
call "%MAVEN_DIR%\bin\mvn" -version

echo.
echo ==========================================
echo Setup complete! 
echo ==========================================
echo.
echo To run the backend, use: run-backend.bat
echo.
# Install script for Knowledge Graph Backend Dependencies

Write-Host "Knowledge Graph Backend Installation Script" -ForegroundColor Green
Write-Host "============================================" -ForegroundColor Green

# Check if running as Administrator
if (-NOT ([Security.Principal.WindowsPrincipal] [Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole] "Administrator")) {
    Write-Host "This script requires Administrator privileges for Chocolatey installation." -ForegroundColor Yellow
    Write-Host "Please run PowerShell as Administrator and try again." -ForegroundColor Yellow
    exit 1
}

# Install Chocolatey if not present
if (!(Get-Command choco -ErrorAction SilentlyContinue)) {
    Write-Host "Installing Chocolatey package manager..." -ForegroundColor Yellow
    Set-ExecutionPolicy Bypass -Scope Process -Force
    [System.Net.ServicePointManager]::SecurityProtocol = [System.Net.ServicePointManager]::SecurityProtocol -bor 3072
    iex ((New-Object System.Net.WebClient).DownloadString('https://community.chocolatey.org/install.ps1'))
    
    # Refresh environment
    $env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User")
}

# Install OpenJDK 17
Write-Host "`nInstalling OpenJDK 17..." -ForegroundColor Yellow
choco install openjdk17 -y

# Install Maven
Write-Host "`nInstalling Apache Maven..." -ForegroundColor Yellow
choco install maven -y

# Refresh environment variables
Write-Host "`nRefreshing environment variables..." -ForegroundColor Yellow
$env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User")
refreshenv

# Verify installations
Write-Host "`nVerifying installations..." -ForegroundColor Yellow

$javaVersion = java -version 2>&1 | Select-String "version"
if ($javaVersion) {
    Write-Host "✓ Java installed: $javaVersion" -ForegroundColor Green
} else {
    Write-Host "✗ Java installation failed" -ForegroundColor Red
}

$mvnVersion = mvn -version 2>&1 | Select-String "Apache Maven"
if ($mvnVersion) {
    Write-Host "✓ Maven installed: $mvnVersion" -ForegroundColor Green
} else {
    Write-Host "✗ Maven installation failed" -ForegroundColor Red
}

Write-Host "`nInstallation complete!" -ForegroundColor Green
Write-Host "Please restart your terminal/IDE to ensure all environment variables are loaded." -ForegroundColor Yellow
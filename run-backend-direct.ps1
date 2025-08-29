$env:JAVA_HOME = "$PSScriptRoot\tools\jdk-17"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
Set-Location backend
& "$PSScriptRoot\tools\maven\bin\mvn.cmd" spring-boot:run "-Dspring-boot.run.profiles=local" "-Dmaven.test.skip=true"
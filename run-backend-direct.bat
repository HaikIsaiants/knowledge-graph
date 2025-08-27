@echo off
cd backend
set JAVA_HOME=C:\Users\haiki\OneDrive\Documents\GitHub\knowledge-graph\tools\jdk-17
"%JAVA_HOME%\bin\java" -cp "target\classes;..\tools\maven\repository\org\springframework\boot\spring-boot-starter-web\3.2.0\*;..\tools\maven\repository\org\springframework\boot\spring-boot-starter-data-jpa\3.2.0\*;..\tools\maven\repository\org\postgresql\postgresql\42.7.1\*;..\tools\maven\repository\*\*\*.jar" com.knowledgegraph.KnowledgeGraphApplication
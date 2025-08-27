@echo off
echo Running Search and Query API tests...
cd backend

echo.
echo Compiling code and tests...
call mvnw.cmd clean compile test-compile

if %errorlevel% neq 0 (
    echo Compilation failed!
    exit /b %errorlevel%
)

echo.
echo ====================================
echo Running SearchService Tests...
echo ====================================
call mvnw.cmd test -Dtest=SearchServiceTest

echo.
echo ====================================
echo Running VectorSearchService Tests...
echo ====================================
call mvnw.cmd test -Dtest=VectorSearchServiceTest

echo.
echo ====================================
echo Running HybridSearchService Tests...
echo ====================================
call mvnw.cmd test -Dtest=HybridSearchServiceTest

echo.
echo ====================================
echo Running GraphTraversalService Tests...
echo ====================================
call mvnw.cmd test -Dtest=GraphTraversalServiceTest

echo.
echo ====================================
echo Running SearchController Tests...
echo ====================================
call mvnw.cmd test -Dtest=SearchControllerTest

echo.
echo ====================================
echo Running NodeController Tests...
echo ====================================
call mvnw.cmd test -Dtest=NodeControllerTest

echo.
echo ====================================
echo Running GraphController Tests...
echo ====================================
call mvnw.cmd test -Dtest=GraphControllerTest

echo.
echo ====================================
echo Running SearchUtils Tests...
echo ====================================
call mvnw.cmd test -Dtest=SearchUtilsTest

echo.
echo ====================================
echo Generating Test Coverage Report...
echo ====================================
call mvnw.cmd jacoco:report

echo.
echo Tests completed! Check target/site/jacoco for coverage report.
pause
# Testing Guide for Knowledge Graph Search & Query API

## Overview

This guide provides instructions for running and understanding the comprehensive test suite for the Search & Query API implementation.

## Test Structure

```
backend/src/test/java/com/knowledgegraph/
├── controller/
│   ├── SearchControllerTest.java
│   ├── NodeControllerTest.java
│   ├── GraphControllerTest.java
│   └── GlobalExceptionHandlerTest.java
├── service/
│   ├── SearchServiceTest.java
│   ├── VectorSearchServiceTest.java
│   ├── HybridSearchServiceTest.java
│   └── GraphTraversalServiceTest.java
└── util/
    └── SearchUtilsTest.java
```

## Running Tests

### Run All Tests
```bash
# Windows
run-tests.bat

# Linux/Mac
cd backend && ./mvnw test
```

### Run Search API Tests Only
```bash
# Windows
run-search-tests.bat

# Linux/Mac
cd backend && ./mvnw test -Dtest=*Search*,*Graph*,*Node*
```

### Run Individual Test Classes
```bash
cd backend

# Service tests
./mvnw test -Dtest=SearchServiceTest
./mvnw test -Dtest=VectorSearchServiceTest
./mvnw test -Dtest=HybridSearchServiceTest
./mvnw test -Dtest=GraphTraversalServiceTest

# Controller tests
./mvnw test -Dtest=SearchControllerTest
./mvnw test -Dtest=NodeControllerTest
./mvnw test -Dtest=GraphControllerTest
./mvnw test -Dtest=GlobalExceptionHandlerTest

# Utility tests
./mvnw test -Dtest=SearchUtilsTest
```

### Run Tests with Coverage
```bash
cd backend
./mvnw clean test jacoco:report
```

Coverage report will be available at:
- HTML: `backend/target/site/jacoco/index.html`
- XML: `backend/target/site/jacoco/jacoco.xml`

## Test Categories

### 1. Unit Tests

#### Service Layer
- **SearchServiceTest**: Full-text search, highlighting, faceting
- **VectorSearchServiceTest**: Similarity search, k-NN, embeddings
- **HybridSearchServiceTest**: Combined search, result merging, adaptive weights
- **GraphTraversalServiceTest**: Graph operations, paths, neighborhoods

#### Utility Classes
- **SearchUtilsTest**: Score extraction, normalization, timing, transformations

### 2. Integration Tests

#### Controller Layer
- **SearchControllerTest**: REST endpoints, request/response validation
- **NodeControllerTest**: CRUD operations, relationships
- **GraphControllerTest**: Graph queries, traversals
- **GlobalExceptionHandlerTest**: Error handling, response formatting

## Test Patterns and Best Practices

### 1. Mocking
```java
@Mock
private NodeRepository nodeRepository;

@InjectMocks
private SearchService searchService;
```

### 2. Test Data Builders
```java
Node testNode = Node.builder()
    .id(UUID.randomUUID())
    .name("Test Node")
    .type(NodeType.PERSON)
    .build();
```

### 3. Descriptive Test Names
```java
@Test
@DisplayName("searchNodes - Full-text search with type filter")
void testSearchNodes_WithTypeFilter() { }
```

### 4. Console Logging
```java
System.out.println("Testing full-text search with type filter...");
// ... test logic ...
System.out.println("✓ Full-text search with type filter works correctly");
```

## Test Coverage Goals

- **Line Coverage**: > 80%
- **Branch Coverage**: > 75%
- **Method Coverage**: > 90%

## Test Scenarios Covered

### Happy Path
- ✅ Standard CRUD operations
- ✅ Basic search queries
- ✅ Normal graph traversals
- ✅ Default parameter handling

### Edge Cases
- ✅ Empty results
- ✅ Null values
- ✅ Missing parameters
- ✅ Invalid inputs
- ✅ Boundary conditions

### Error Conditions
- ✅ Database errors
- ✅ Service exceptions
- ✅ Validation failures
- ✅ Resource not found

### Performance
- ✅ Large datasets
- ✅ Concurrent requests
- ✅ Response times
- ✅ Memory usage

## Continuous Integration

### GitHub Actions Workflow
```yaml
name: Tests
on: [push, pull_request]
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - uses: actions/setup-java@v2
        with:
          java-version: '17'
      - run: cd backend && ./mvnw test
      - run: cd backend && ./mvnw jacoco:report
      - uses: codecov/codecov-action@v2
```

## Debugging Failed Tests

### 1. Enable Debug Logging
```properties
# application-test.properties
logging.level.com.knowledgegraph=DEBUG
logging.level.org.springframework.web=DEBUG
```

### 2. Run Single Test Method
```bash
./mvnw test -Dtest=SearchServiceTest#testSearchNodes_WithTypeFilter
```

### 3. Skip Tests During Build
```bash
./mvnw clean install -DskipTests
```

## Test Database Setup

Tests use an in-memory H2 database by default. Configuration in `application-test.properties`:

```properties
spring.datasource.url=jdbc:h2:mem:testdb
spring.jpa.hibernate.ddl-auto=create-drop
```

## Mock Data Generation

### Using Test Fixtures
```java
@TestFixture
public class TestDataFactory {
    public static Node createTestNode() { }
    public static Edge createTestEdge() { }
    public static Document createTestDocument() { }
}
```

## Performance Testing

### JMeter Tests
Load test configuration files in `backend/src/test/jmeter/`:
- `search-api-load-test.jmx`
- `graph-traversal-load-test.jmx`

### Running Load Tests
```bash
jmeter -n -t search-api-load-test.jmx -l results.jtl
```

## Test Reports

### Surefire Reports
```bash
cd backend/target/surefire-reports
```

### Test Summary
After running tests, check:
- `backend/target/surefire-reports/TEST-*.xml`
- Console output for detailed test execution logs

## Troubleshooting

### Common Issues

1. **Maven wrapper not found**
   - Run: `mvn -N io.takari:maven:wrapper`

2. **Out of memory during tests**
   - Set: `export MAVEN_OPTS="-Xmx2048m"`

3. **Port already in use**
   - Tests use random ports, but check: `netstat -an | grep 8080`

4. **Database connection issues**
   - Ensure H2 dependency is included
   - Check test profile configuration

## Contributing

When adding new features:
1. Write tests first (TDD approach)
2. Ensure > 80% coverage for new code
3. Run full test suite before committing
4. Update this guide if adding new test patterns

## Contact

For questions about testing:
- Review test examples in codebase
- Check Spring Boot testing documentation
- Consult team lead for complex scenarios
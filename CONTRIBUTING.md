# Contributing to Knowledge Graph System

Thank you for your interest in contributing to the Knowledge Graph System!

## Development Setup

1. Fork the repository
2. Clone your fork locally
3. Install prerequisites:
   - Java 17+
   - Node.js 18+
   - PostgreSQL 15+
4. Follow setup instructions in README.md

## Development Workflow

1. Create a feature branch from `main`
2. Make your changes
3. Write/update tests
4. Ensure all tests pass
5. Update documentation if needed
6. Submit a pull request

## Code Standards

### Backend (Java/Spring Boot)
- Follow standard Java naming conventions
- Use meaningful variable and method names
- Add JavaDoc comments for public methods
- Write unit tests for new functionality
- Maintain test coverage above 80%

### Frontend (Vue 3/TypeScript)
- Use Vue 3 Composition API with `<script setup>`
- Follow TypeScript best practices
- Use Tailwind CSS for styling
- Write component tests with Vitest
- Keep components focused and reusable

## Testing

```bash
# Run backend tests
cd backend
mvn clean test

# Run frontend tests
cd frontend
npm test
```

## Commit Messages

Use clear, descriptive commit messages:
- `feat:` - New feature
- `fix:` - Bug fix
- `docs:` - Documentation changes
- `style:` - Code style changes
- `refactor:` - Code refactoring
- `test:` - Test additions or changes
- `chore:` - Build process or auxiliary tool changes

## Pull Request Process

1. Update README.md with details of changes if applicable
2. Ensure all tests pass
3. Update the plan.md if you've completed new features
4. Request review from maintainers

## Questions?

Feel free to open an issue for any questions or suggestions!
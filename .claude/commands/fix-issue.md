# Fix issue #$ARGUMENTS following our coding standards

Locate and fix the specified issue number, adhering to the project's coding standards and architecture.

## Before fixing
- Read issue #$ARGUMENTS from Linear/GitHub to understand the problem
- Check CLAUDE.md for relevant architecture/layering patterns
- Identify affected components and their responsibilities
- Review related tests to understand expected behavior

## Coding standards applied
- **Layering**: Controller → Service → Repository (no business logic in controllers)
- **Type safety**: TypeScript strict mode (frontend), Java type contracts (backend)
- **Naming**: Clear, descriptive names matching domain language
- **Errors**: Use domain-specific exceptions (BadRequestException, PolicyViolationException, etc.)
- **Testing**: Unit tests for logic, integration tests for flows
- **Migrations**: Flyway versioned migrations for schema changes (never auto-DDL)
- **Permissions**: Hand-rolled checks in services (assertOwner, assertCanDecide, etc.)
- **Audit trail**: Record state transitions via AuditService

## Fix workflow
1. Locate the code responsible for the issue
2. Add or update tests first (red → green)
3. Implement the fix
4. Verify against existing tests
5. Create a new commit with issue number in message

## Commit message format
`Fix #$ARGUMENTS: [short description of what was fixed]`
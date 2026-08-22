# Pockets

## Configuration

Runtime configuration is loaded from the ignored root `.env` file. Do not
commit database credentials, JWT secrets, access tokens, or local `.env` files.

Create `.env` in the project root:

```properties
DB_URL=jdbc:postgresql://localhost:5432/pockets_db
DB_USERNAME=pockets_user
DB_PASSWORD=your-local-database-password
JWT_SECRET=generate-a-unique-secret-with-openssl-rand-hex-32
TEST_DB_URL=jdbc:postgresql://localhost:5432/pockets_test_db
TEST_DB_USERNAME=pockets_user
TEST_DB_PASSWORD=your-local-test-password
```

Generate a JWT secret with `openssl rand -hex 32`, then start the application:

```bash
./mvnw spring-boot:run
```

The committed [application.properties.example](src/main/resources/application.properties.example)
documents the available non-secret settings. Production deployments should
provide `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, and `JWT_SECRET` through the
deployment platform's secret manager or environment configuration.

Tests use the corresponding `TEST_DB_URL`, `TEST_DB_USERNAME`, and
`TEST_DB_PASSWORD` values from the same `.env` file:

```bash
./mvnw test
```

# API contract

This directory holds the **canonical OpenAPI 3.0 spec** for the Owner's Manual RAG API.

- **[openapi.yaml](./openapi.yaml)** — Paths, request/response schemas, and validation rules aligned with the Java DTOs in `com.rag.ownermanual.dto.query` and `com.rag.ownermanual.dto.ingest`.

**Use this spec to:**

- Implement controllers without inferring the contract from DTOs alone.
- Generate client SDKs or types (e.g. OpenAPI Generator).
- Validate request/response examples or run contract tests.


**When the application runs**, Swagger UI is available at `/swagger-ui.html` and the live OpenAPI JSON at `/v3/api-docs` (springdoc discovers controllers and DTOs annotated with `@Schema`).

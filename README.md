# Owner Manual RAG System

A Spring Boot application for RAG (Retrieval-Augmented Generation) on owner's manual documents, with multimodal support for dashboard light identification.

## Tech Stack
- **Backend**: Spring Boot 3.5.10 + Spring AI 1.1.2
- **Vector Store**: Qdrant (Cloud or Docker)
- **Embeddings**: OpenAI (configurable)
- **LLM**: Groq (configurable via Spring AI OpenAI-compatible API)
- **Relational DB**: Supabase PostgreSQL (managed)
- **Observability**: Spring Boot Actuator

## Prerequisites
- Java 21+
- Maven 3.8+
- Docker & Docker Compose (for local development)
- Environment variables configured (see below)

## Environment Variables
The following environment variables must be set before running the application:

### OpenAI (for embeddings)
- `OPENAI_API_KEY` - Used for text embeddings (`text-embedding-3-small`)

### Groq (for LLM chat)
- `GROQ_API_KEY` - Used for Chat
- `GROQ_MODEL` - Model name (e.g., `llama-3.1-8b-instant`)

### Qdrant (vector store)
- `QDRANT_URL` - Qdrant hostname (e.g., `53453rfe35452.us-east-1-1.aws.cloud.qdrant.io` for Qdrant Cloud, or `localhost` for local Docker)
- `QDRANT_API_KEY` - Qdrant API key (required for Qdrant Cloud; can be empty for local)

### Supabase PostgreSQL (relational database)
- `SUPABASE_DB_URL` - JDBC connection URL (e.g., `jdbc:postgresql://aws-1-us-east-1.pooler.supabase.com:6543/postgres?sslmode=require`)
- `SUPABASE_DB_USERNAME` - Database username
- `SUPABASE_DB_PASSWORD` - Database password

## Quick Start

### Local Development (with Docker Compose)
One command starts the **app**, **Postgres**, and **Qdrant** together. No need to run databases separately.

1. **Environment variables**: The app reads from a `.env` file in the project root. If you already have `.env` with `OPENAI_API_KEY`, `GROQ_API_KEY`, and `GROQ_MODEL` set, you’re good to go. If not, set the environment variables or export in your shell. The compose file sets `QDRANT_URL`, `SUPABASE_DB_*`, and `SPRING_AI_VECTORSTORE_QDRANT_USE_TLS` for the local stack (no need to set those in `.env` when using docker-compose).

2. **Start the full stack**:
   ```bash
   docker-compose up
   ```

3. **Verify health** (after the app is up):
   ```bash
   curl http://localhost:8080/actuator/health
   ```
   All components (qdrant, embedding, chat, db) should be UP when the stack is running.

### Running Without Docker (for testing)
If you have Qdrant and Postgres running locally or via cloud services:

```bash
mvn spring-boot:run
```

The application will fail fast at startup if any required environment variables are missing.

### Using Qdrant Cloud + Supabase (no local Qdrant or Postgres)
If your `.env` already points to **Qdrant Cloud** and **Supabase** (cloud Postgres), you **do not run Qdrant or Postgres locally**. The app talks to the cloud services.

1. Put your cloud values in `.env` (Qdrant Cloud host + API key, Supabase URL + username + password, plus OpenAI and Groq keys).
2. From the project root run:
   ```bash
   mvn spring-boot:run
   ```
3. Check health: `curl -s http://localhost:8080/actuator/health | jq`

No Docker commands for Qdrant or Postgres are needed in this setup.

---

## Running Qdrant and Postgres locally (optional)
Use this **only** when you want Qdrant and Postgres in Docker on your machine (e.g. to avoid hitting cloud during dev, or to test with a clean DB). If you use Qdrant Cloud + Supabase, skip this section.

### 1. Start Qdrant (Docker)
```bash
docker run -d --name qdrant -p 6333:6333 -p 6334:6334 qdrant/qdrant:latest
```

- **6333** = REST API  
- **6334** = gRPC (Spring AI Qdrant client uses this by default)

### 2. Start Postgres (Docker) — optional if you use Supabase Cloud
For a **local** Postgres (no Supabase):

```bash
docker run -d --name postgres-ownermanual \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=ownermanual \
  -p 5432:5432 \
  postgres:16
```

### 3. Run the app and check health

From the project root (with Qdrant and, if local, Postgres already running):
```bash
mvn spring-boot:run
```

In another terminal:
```bash
curl -s http://localhost:8080/actuator/health | jq
```

You should see `"status": "UP"` and components (qdrant, embedding, chat, db) UP.


## Health Checks
The application exposes health endpoints via Spring Boot Actuator:
- `/actuator/health` - Overall application health (includes Qdrant, embedding, LLM, and Postgres connectivity)



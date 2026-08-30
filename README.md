# 🤖 RAG Generator

A full-stack Retrieval-Augmented Generation (RAG) application for creating knowledge bases from documents and asking natural-language questions against their content.

The application supports both **OpenAI** and **Ollama** as AI providers, allowing it to run either with cloud-based AI services or completely locally.

## Functionality

The application provides the following core functionality:

* Create and manage knowledge bases.
* Upload PDF documents to a knowledge base.
* Extract text from uploaded documents.
* Split documents into chunks.
* Generate embeddings for document chunks.
* Store document metadata, chunks, and embeddings.
* Perform semantic similarity search.
* Retrieve relevant document chunks for a question.
* Generate answers using retrieved context.
* Support OpenAI and Ollama as interchangeable AI providers.
* Provide Swagger/OpenAPI documentation for the backend API.

## 🚀 Quick Start

No additional setup is required for the default local configuration.

From the project root, simply run:

```bash
docker compose up -d --build
```

That's it! 🎉
> 💡 **Note:** The first startup may take longer while Ollama downloads the AI models. Subsequent startups will reuse the downloaded models.

## The RAG pipeline is:

```text
Document
   ↓
Text Extraction
   ↓
Chunking
   ↓
Embedding Generation
   ↓
PostgreSQL + pgvector
   ↓
Semantic Retrieval
   ↓
Relevant Context
   ↓
LLM
   ↓
Generated Answer
```

## Tech Stack

### Backend

* Java
* Spring Boot
* Spring Data JPA
* Spring AI
* Hibernate
* Gradle
* Flyway

### Frontend

* Next.js
* React
* TypeScript
* TanStack React Query
* Axios
* React Hook Form
* Zod
* Tailwind CSS
* pnpm

### Data & Infrastructure

* PostgreSQL
* pgvector
* MinIO
* Docker
* Docker Compose

### AI Providers

* OpenAI
* Ollama

## Architecture

The backend follows a **modular monolith** architecture and is organized using **Clean Architecture** principles.

The overall repository uses a **monorepo** structure.

### Monorepo

The frontend and backend are maintained in a single Git repository:

```text
rag-generator/
├── backend/
├── frontend/
├── docker-compose.yml
├── .env.example
├── .gitignore
└── README.md
```

The monorepo approach is used because the frontend and backend are part of the same product and are developed, tested, and deployed together.

It provides several benefits:

* One repository for the complete application.
* Easier local development and onboarding.
* Shared project documentation and configuration.
* A single Docker Compose configuration for the complete stack.

The frontend and backend are still kept as separate applications within the repository. Each application has its own dependencies, build process, Dockerfile, and development workflow.

The monorepo therefore provides **repository-level simplicity without tightly coupling the application implementations**.

### Modular Monolith with Layered Architecture

The backend is implemented as a **modular monolith**. Business capabilities are separated into modules within a single Spring Boot application.

Each module follows a **layered architecture**, separating responsibilities such as controllers, services and repositories.

```text
Spring Boot Application
│
├── Knowledge Base
│   ├── Controller
│   ├── Service
│   └── Repository
│
├── Document
│   ├── Controller
│   ├── Service
│   └── Repository
│
└── RAG
    ├── Controller
    └── Service
```

This approach keeps related functionality together while maintaining clear separation of responsibilities within each module. It provides the simplicity of a single deployable application without sacrificing modularity and maintainability.

## Data Architecture

### PostgreSQL + pgvector

PostgreSQL is used as the primary relational database.

The application uses pgvector to store and search document embeddings directly in PostgreSQL.

This allows the application to store:

* Knowledge base information.
* Document metadata.
* Document chunks.
* Vector embeddings.
* Relationships between these entities.

Using PostgreSQL with pgvector avoids introducing a separate vector database while providing both relational and semantic search capabilities.

### MinIO

MinIO is used as the application's object storage for uploaded documents.

It provides functionality similar to AWS S3, but runs locally through Docker. This allows the application to store and manage uploaded files locally without requiring cloud infrastructure.

Binary document files are stored in MinIO, while PostgreSQL stores the document metadata and a reference to the corresponding object.

```text
Uploaded PDF
     │
     ├──────────────► MinIO
     │                 └── Original document
     │
     └──────────────► PostgreSQL
                       ├── Document metadata
                       ├── Chunks
                       └── Embeddings
```

MinIO provides an S3-compatible API, keeping the application storage abstraction compatible with S3-style object storage solutions.

## Project Setup

### Prerequisites

For the recommended setup, install:

* Docker
* Docker Compose

PostgreSQL, MinIO, and Ollama do not need to be installed separately when using Docker Compose.

### Environment Configuration

Environment configuration is **optional** for the default local setup.

```👉 The application provides sensible default values for local development, so the complete system can be started directly with:```

```bash
docker compose up -d --build
```

If you want to customize the default configuration, create a `.env` file from the provided example:

```bash
cp .env.example .env
```

The `.env.example` file documents the available configuration options without requiring developers to configure every value manually.

You can use the `.env` file when you need to customize settings such as:

* AI provider
* AI models
* Database configuration
* Application ports
* MinIO configuration
* RAG configuration
* Document processing configuration

The `.env` file is optional and should not be committed to Git.

The `.env.example` file is committed to the repository as a reference for available configuration options.

## Run Locally with Docker Compose

No additional configuration is required for the default local setup.

Simply run:

```bash
docker compose up -d --build
```

Docker Compose will start the complete application stack, including:

* Next.js frontend
* Spring Boot backend
* PostgreSQL with pgvector
* MinIO
* Ollama
* Required Ollama chat and embedding models

```👉 The default configuration is designed to work locally without requiring external services or API keys.```

On the first startup, Ollama may take some time to download the configured models. The downloaded models are persisted in a Docker volume and are reused on subsequent starts.

### Custom Configuration

If you need to change the default configuration:

```bash
cp .env.example .env
```

Modify the required values in `.env`, then start the application:

```bash
docker compose up -d --build
```

You only need to configure the values you want to change. All other settings continue to use their default values.

For example, the AI provider can be changed from the default local Ollama setup to OpenAI. When OpenAI is selected, an OpenAI API key must also be provided.

## AI Provider

The application supports both **Ollama** and **OpenAI**.

### Ollama

Ollama is the default provider for local development.

It allows the RAG application to run locally without an external AI API.

The default chat model is a lightweight model suitable for local environments. Larger models such as `llama3.1` can also be configured when the machine has sufficient resources.

### OpenAI

OpenAI can be selected when cloud-based AI inference is preferred.

When OpenAI is selected, an OpenAI API key must be configured.

After changing the provider or other AI configuration, restart the stack:

```bash
docker compose up -d --build
```

The host ports can be customized through the environment configuration if the default ports are already in use.

## Switching AI Providers

The application supports both **Ollama** and **OpenAI**.

The AI provider can be selected by changing the provider configuration in the `.env` file.

### Ollama

To use Ollama locally:

```env
AI_PROVIDER=ollama
```

No external API key is required.

The default local chat model is a lightweight Qwen model. If your machine has sufficient resources, you can change the chat model to a larger model such as `llama3.1`.

### OpenAI

To use OpenAI:

```env
AI_PROVIDER=openai
```

When using OpenAI, an OpenAI API key is required. Configure the key in the `.env` file.

After changing the provider or model configuration, restart the application:

```bash
docker compose up -d --build
```


For the simplest and most reproducible setup, Docker Compose is recommended because it provides all required infrastructure services with a single command.


## Docker Architecture

The complete application runs as separate containers:

```text
                    Browser
                       │
                       ▼
                 ┌───────────┐
                 │  Next.js  │
                 │ Frontend  │
                 └─────┬─────┘
                       │
                       ▼
                 ┌───────────┐
                 │  Spring   │
                 │   Boot    │
                 │  Backend  │
                 └─────┬─────┘
                       │
          ┌────────────┼────────────┐
          │            │            │
          ▼            ▼            ▼
    PostgreSQL       MinIO        Ollama
    + pgvector                     │
                                   ├── Chat Model
                                   └── Embedding Model
```

The services communicate through the Docker Compose network using service names.

For example, the backend communicates internally with:

```text
postgres:5432
minio:9000
ollama:11434
```

## Application URLs

After starting the application:

| Service       | URL                        |
| ------------- | -------------------------- |
| Frontend      | http://localhost:3000      |
| Backend       | http://localhost:8080      |
| Swagger       | http://localhost:8080/docs |
| MinIO API     | http://localhost:9000      |
| MinIO Console | http://localhost:9001      |
| Ollama        | http://localhost:11434     |


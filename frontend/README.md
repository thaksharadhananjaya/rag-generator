# RAG Generator — Frontend

Next.js 16 (App Router) frontend for the RAG Generator backend. Create a
knowledge base, upload PDFs, watch ingestion progress, then ask questions and
read grounded answers with their supporting sources.

## Stack

- **Next.js 16** + **TypeScript** (App Router)
- **Tailwind CSS v4**
- **Axios** — the only HTTP client; centralized in `lib/api/client.ts`
- **TanStack React Query** — server-state: caching, loading/error, polling,
  mutations, cache invalidation
- **React Hook Form** + **Zod** — forms and validation

No Redux, no auth middleware, no extra state libraries.

## Getting started

```bash
cp .env.example .env   # then edit if your backend is elsewhere
npm install
npm run dev                  # http://localhost:3000
```

### Environment

| Variable | Purpose | Default |
| --- | --- | --- |
| `NEXT_PUBLIC_API_URL` | Base URL of the Spring Boot backend (no trailing slash) | `http://localhost:8080` |
| `NEXT_PUBLIC_MAX_UPLOAD_MB` | Frontend PDF size guard (backend stays authoritative) | `25` |

## Routes

| Path | Purpose |
| --- | --- |
| `/` | **Home / primary ask experience** — pick a knowledge base (loaded from `GET /api/v1/knowledge-bases`), create one via the existing modal, ask a question (Advanced Options: `topK`, `minScore`), read the grounded answer + sources. Links out to the management UI. |
| `/knowledge-bases` | Paginated list; create / open / delete |
| `/knowledge-bases/[id]` | Two tabs: **Documents** (upload, status polling, re-ingest, delete) and **Ask questions** (query + grounded answer + sources) |

## Architecture

```
lib/
  config.ts                 env + constants
  types/api.ts              all backend models (aligned with the OpenAPI schemas)
  api/
    client.ts               Axios instance + response interceptor
    errors.ts               normalize any failure -> { status, code, message, fieldErrors }
    pagination.ts           normalize the page envelope
    knowledge-base-api.ts   POST/GET/GET{id}/DELETE  /api/v1/knowledge-bases
    document-api.ts          upload/list/get/delete/re-ingest documents
    rag-api.ts               POST /api/v1/knowledge-bases/{id}/query
  validation/schemas.ts     Zod schemas + PDF client-side validation
  hooks/                     React Query hooks (one file per domain)
components/
  ui/                        Button, Card, Modal, ConfirmDialog, Toast, Skeleton, Pagination, EmptyState, …
  errors/                    ApiErrorMessage, FieldError, InlineError
  knowledge-bases/  documents/  rag/     feature components
```

### Backend endpoints used (exhaustive — nothing else is called)

| Method | Path |
| --- | --- |
| POST | `/api/v1/knowledge-bases` |
| GET | `/api/v1/knowledge-bases?page&size&sort` |
| GET | `/api/v1/knowledge-bases/{id}` |
| DELETE | `/api/v1/knowledge-bases/{id}` |
| POST | `/api/v1/knowledge-bases/{knowledgeBaseId}/documents` (multipart, field `file`) |
| GET | `/api/v1/knowledge-bases/{knowledgeBaseId}/documents?page&size&sort` |
| GET | `/api/v1/documents/{documentId}` |
| DELETE | `/api/v1/documents/{documentId}` |
| POST | `/api/v1/documents/{documentId}/ingest` (retry failed ingestion) |
| POST | `/api/v1/knowledge-bases/{knowledgeBaseId}/query` |

### Ingestion polling

While any document is `PENDING`, `PROCESSING`, or `EMBEDDING`, the existing
document-list query is refetched every 3s (`refetchInterval`). Polling stops
automatically once everything is `COMPLETED` or `FAILED`. No dedicated status
endpoint is used.

### Error handling

Every request passes through one Axios interceptor that converts failures into a
consistent `ApiError` (`status`, `code`, `message`, `fieldErrors`). Java/Spring
stack traces are stripped. `400/404/409/413/415/422/429/500/502/503/504` and
network errors each get a clear, user-facing message and a retry affordance
where it makes sense. Failed deletes keep the item visible; failed uploads keep
the upload UI; failed queries keep the question and advanced options intact.

## Scripts

```bash
npm run dev      # dev server
npm run build    # production build (also type-checks)
npm run lint     # eslint
```

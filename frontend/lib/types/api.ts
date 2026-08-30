/**
 * TypeScript models for the RAG Generator backend.
 * Kept centralized and aligned with the backend Swagger/OpenAPI schemas.
 */

/* ------------------------------------------------------------------ */
/* Pagination                                                          */
/* ------------------------------------------------------------------ */

/** Query params accepted by every paginated backend endpoint. */
export interface Pageable {
  page: number;
  size: number;
  /** e.g. "createdAt,desc" */
  sort?: string;
}

/**
 * Normalized page envelope used across the frontend.
 * The backend returns `content`, `page`, `size`, `totalElements`, `totalPages`.
 */
export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

/* ------------------------------------------------------------------ */
/* Knowledge bases                                                     */
/* ------------------------------------------------------------------ */

export interface KnowledgeBase {
  id: string;
  name: string;
  description?: string | null;
  createdAt: string;
  updatedAt?: string | null;
  /** Present on some backend versions; rendered only when available. */
  documentCount?: number | null;
}

export interface CreateKnowledgeBaseRequest {
  name: string;
  description?: string;
}

/* ------------------------------------------------------------------ */
/* Documents                                                           */
/* ------------------------------------------------------------------ */

export type DocumentStatus =
  | "PENDING"
  | "PROCESSING"
  | "EMBEDDING"
  | "COMPLETED"
  | "FAILED";

/** Statuses for which the frontend should keep polling. */
export const IN_PROGRESS_STATUSES: DocumentStatus[] = [
  "PENDING",
  "PROCESSING",
  "EMBEDDING",
];

export interface DocumentDto {
  id: string;
  knowledgeBaseId?: string;
  filename: string;
  sizeBytes: number;
  status: DocumentStatus;
  chunkCount?: number | null;
  createdAt: string;
  updatedAt?: string | null;
  ingestedAt?: string | null;
  failureReason?: string | null;
  retryCount?: number | null;
}

/* ------------------------------------------------------------------ */
/* RAG query                                                           */
/* ------------------------------------------------------------------ */

export interface RagQueryRequest {
  question: string;
  topK?: number;
  minScore?: number;
}

export interface RagSource {
  documentId: string;
  chunkId: string;
  ordinal: number;
  page?: number | null;
  score: number;
  excerpt: string;
}

export interface RagQueryResponse {
  answer: string;
  sources: RagSource[];
  model: string;
  tokensUsed: number;
  retrievedChunks: number;
}

/* ------------------------------------------------------------------ */
/* Errors                                                              */
/* ------------------------------------------------------------------ */

/** Consistent, normalized error shape used everywhere in the UI. */
export interface ApiError {
  /** HTTP status code, or 0 for a network / unreachable-backend error. */
  status: number;
  /** Stable machine-readable code, e.g. "NOT_FOUND", "NETWORK_ERROR". */
  code: string;
  /** User-facing message. Never a raw stack trace. */
  message: string;
  /** Field-level validation messages keyed by field name. */
  fieldErrors?: Record<string, string>;
}

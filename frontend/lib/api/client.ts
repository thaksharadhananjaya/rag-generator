import axios, { AxiosError } from "axios";
import { config } from "@/lib/config";
import { normalizeError } from "@/lib/api/errors";

/**
 * Centralized Axios client for all communication with the Spring Boot backend.
 *
 * Responsibilities split:
 *  - Axios (this file) performs HTTP requests only.
 *  - TanStack React Query owns caching, loading/error state, polling, retries,
 *    mutations and cache invalidation.
 *
 * Components must never call `fetch()` or `axios` directly — they go through
 * the typed API modules (`knowledge-base-api.ts`, `document-api.ts`,
 * `rag-api.ts`) which use this instance.
 */
export const apiClient = axios.create({
  baseURL: config.apiBaseUrl ? `${config.apiBaseUrl}` : undefined,
  timeout: 30_000,
  headers: { Accept: "application/json" },
});

// Single response interceptor: normalize every error into our ApiError shape
// so callers and React Query never see a raw AxiosError or stack trace.
apiClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError) => Promise.reject(normalizeError(error)),
);

/** Build a Spring `pageable` query string. */
export function pageableParams(params: {
  page: number;
  size: number;
  sort?: string;
}): Record<string, string | number> {
  const out: Record<string, string | number> = {
    page: params.page,
    size: params.size,
  };
  if (params.sort) out.sort = params.sort;
  return out;
}

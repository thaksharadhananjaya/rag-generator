/**
 * Centralized frontend configuration sourced from environment variables.
 * All values are read once at module load.
 */

function readApiBaseUrl(): string {
  const raw = process.env.NEXT_PUBLIC_API_URL?.trim();
  if (!raw) {
    // Fall back to a same-origin relative base so the app still renders in
    // environments where the variable was not provided.
    return "";
  }
  return raw.replace(/\/+$/, "");
}

function readMaxUploadBytes(): number {
  const raw = Number(process.env.NEXT_PUBLIC_MAX_UPLOAD_MB);
  const mb = Number.isFinite(raw) && raw > 0 ? raw : 25;
  return Math.round(mb * 1024 * 1024);
}

export const config = {
  /** Backend base URL, without a trailing slash. May be "" for same-origin. */
  apiBaseUrl: readApiBaseUrl(),
  /** Maximum accepted PDF upload size in bytes (frontend guard only). */
  maxUploadBytes: readMaxUploadBytes(),
  /** Human-readable form of the upload limit, e.g. "25 MB". */
  get maxUploadLabel(): string {
    return `${Math.round(this.maxUploadBytes / (1024 * 1024))} MB`;
  },
} as const;

/** Default page size for paginated lists. */
export const DEFAULT_PAGE_SIZE = 20;

/** Interval (ms) for polling documents that are still being ingested. */
export const INGESTION_POLL_INTERVAL_MS = 3000;

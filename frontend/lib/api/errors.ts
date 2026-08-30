import { AxiosError } from "axios";
import type { ApiError } from "@/lib/types/api";

/**
 * Centralized backend error handling.
 *
 * Every failed request is funnelled through {@link normalizeError} which turns
 * an unknown thrown value into a consistent {@link ApiError}. Raw Java/Spring
 * stack traces are never surfaced to the user.
 */

/** Typed guard so callers can `catch (e) { if (isApiError(e)) ... }`. */
export function isApiError(value: unknown): value is ApiError {
  return (
    typeof value === "object" &&
    value !== null &&
    "status" in value &&
    "code" in value &&
    "message" in value
  );
}

const STATUS_MESSAGE: Record<number, { code: string; message: string }> = {
  400: { code: "BAD_REQUEST", message: "The request was invalid. Please check the highlighted fields and try again." },
  401: { code: "UNAUTHORIZED", message: "You are not authorized to perform this action." },
  403: { code: "FORBIDDEN", message: "You do not have permission to perform this action." },
  404: { code: "NOT_FOUND", message: "The requested resource was not found." },
  409: { code: "CONFLICT", message: "This action conflicts with the current state of the resource." },
  413: { code: "PAYLOAD_TOO_LARGE", message: "The uploaded file is too large." },
  415: { code: "UNSUPPORTED_MEDIA_TYPE", message: "Only PDF files are supported." },
  422: { code: "UNPROCESSABLE_ENTITY", message: "The request could not be processed. Please review your input and try again." },
  429: { code: "RATE_LIMITED", message: "Too many requests. Please wait a moment and try again." },
  500: { code: "SERVER_ERROR", message: "Something went wrong on the server. Please try again." },
  502: { code: "BAD_GATEWAY", message: "The backend or an external service is temporarily unavailable. Please try again shortly." },
  503: { code: "SERVICE_UNAVAILABLE", message: "The service is temporarily unavailable. Please try again shortly." },
  504: { code: "GATEWAY_TIMEOUT", message: "The server took too long to respond. Please try again shortly." },
};

const RETRYABLE_STATUSES = new Set([0, 429, 500, 502, 503, 504]);

/** Whether a "Try again" affordance makes sense for this error. */
export function isRetryable(error: ApiError): boolean {
  return RETRYABLE_STATUSES.has(error.status);
}

/**
 * Extract a safe, user-facing message from a backend error body without
 * leaking implementation details. Spring's default error body looks like
 * `{ timestamp, status, error, message, path, trace }`; some services use
 * `{ code, message, errors: [...] }` or ProblemDetail `{ title, detail }`.
 */
function extractFromBody(
  body: unknown,
  fallback: { code: string; message: string },
): { code: string; message: string; fieldErrors?: Record<string, string> } {
  if (typeof body === "string" && body.trim() && !looksLikeStackTrace(body)) {
    return { code: fallback.code, message: body.trim() };
  }
  if (typeof body !== "object" || body === null) {
    return fallback;
  }

  const b = body as Record<string, unknown>;
  const fieldErrors = collectFieldErrors(b);

  const rawMessage =
    firstString(b.message) ??
    firstString(b.detail) ??
    firstString(b.title) ??
    firstString(b.error);

  const code =
    firstString(b.code) ??
    firstString(b.errorCode) ??
    fallback.code;

  const message =
    rawMessage && !looksLikeStackTrace(rawMessage) ? rawMessage : fallback.message;

  return {
    code,
    message,
    fieldErrors: fieldErrors && Object.keys(fieldErrors).length ? fieldErrors : undefined,
  };
}

/** Pull Spring `{ errors: [{ field, defaultMessage }] }` and similar shapes. */
function collectFieldErrors(
  b: Record<string, unknown>,
): Record<string, string> | undefined {
  const out: Record<string, string> = {};

  const candidates = [b.errors, b.fieldErrors, b.violations];
  for (const c of candidates) {
    if (Array.isArray(c)) {
      for (const item of c) {
        if (item && typeof item === "object") {
          const it = item as Record<string, unknown>;
          const field =
            firstString(it.field) ??
            firstString(it.property) ??
            firstString(it.name);
          const msg =
            firstString(it.defaultMessage) ??
            firstString(it.message) ??
            firstString(it.reason);
          if (field && msg) out[field] = msg;
        }
      }
    } else if (c && typeof c === "object") {
      for (const [k, v] of Object.entries(c as Record<string, unknown>)) {
        const msg = firstString(v);
        if (msg) out[k] = msg;
      }
    }
  }

  return Object.keys(out).length ? out : undefined;
}

function firstString(value: unknown): string | undefined {
  if (typeof value === "string" && value.trim()) return value.trim();
  return undefined;
}

function looksLikeStackTrace(text: string): boolean {
  return (
    /\bat [\w$.]+\([\w$.]+\.java:\d+\)/.test(text) ||
    text.includes("org.springframework.") ||
    text.includes("java.lang.") ||
    text.split("\n").length > 6
  );
}

/**
 * Convert any thrown value into a normalized {@link ApiError}.
 * This is the single choke-point for backend error handling.
 */
export function normalizeError(error: unknown): ApiError {
  if (isApiError(error)) return error;

  if (error instanceof AxiosError) {
    // No response => network error / backend unreachable / CORS / timeout.
    if (!error.response) {
      const timedOut = error.code === "ECONNABORTED" || error.code === "ETIMEDOUT";
      return {
        status: 0,
        code: timedOut ? "TIMEOUT" : "NETWORK_ERROR",
        message: timedOut
          ? "The request timed out. Please check your connection and try again."
          : "Unable to reach the server. Please check your connection and try again.",
      };
    }

    const status = error.response.status;
    const fallback =
      STATUS_MESSAGE[status] ??
      (status >= 500
        ? { code: "SERVER_ERROR", message: "Something went wrong on the server. Please try again." }
        : { code: "REQUEST_FAILED", message: "The request could not be completed. Please try again." });

    const extracted = extractFromBody(error.response.data, fallback);
    return { status, ...extracted };
  }

  if (error instanceof Error) {
    return { status: 0, code: "UNKNOWN", message: error.message || "An unexpected error occurred." };
  }

  return { status: 0, code: "UNKNOWN", message: "An unexpected error occurred." };
}

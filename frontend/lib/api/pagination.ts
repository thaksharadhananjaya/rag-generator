import type { Page } from "@/lib/types/api";

/**
 * Normalize whatever paginated envelope the backend returns into our flat
 * {@link Page} shape.
 *
 * The documented contract is flat (`content`, `page`, `size`, `totalElements`,
 * `totalPages`). Spring can also emit a nested `page: { number, size,
 * totalElements, totalPages }` object, so we handle both defensively without
 * ever downloading extra records.
 */
export function normalizePage<T>(raw: unknown, requestedSize: number): Page<T> {
  const r = (raw ?? {}) as Record<string, unknown>;
  const content = Array.isArray(r.content) ? (r.content as T[]) : [];

  // Nested Spring form.
  if (r.page && typeof r.page === "object") {
    const p = r.page as Record<string, unknown>;
    return {
      content,
      page: num(p.number, 0),
      size: num(p.size, requestedSize),
      totalElements: num(p.totalElements, content.length),
      totalPages: num(p.totalPages, 1),
    };
  }

  // Flat documented form.
  return {
    content,
    page: num(r.page ?? r.number, 0),
    size: num(r.size, requestedSize),
    totalElements: num(r.totalElements, content.length),
    totalPages: num(r.totalPages, 1),
  };
}

function num(value: unknown, fallback: number): number {
  const n = Number(value);
  return Number.isFinite(n) ? n : fallback;
}

"use client";

import { Button } from "@/components/ui/button";

interface PaginationProps {
  /** Zero-based current page. */
  page: number;
  totalPages: number;
  totalElements: number;
  pageSize: number;
  onPageChange: (page: number) => void;
  disabled?: boolean;
}

/**
 * Server-driven pagination control. Uses the backend's `page` / `totalPages` /
 * `totalElements` values — never downloads all records to paginate client-side.
 */
export function Pagination({
  page,
  totalPages,
  totalElements,
  pageSize,
  onPageChange,
  disabled,
}: PaginationProps) {
  if (totalElements === 0) return null;

  const first = page * pageSize + 1;
  const last = Math.min((page + 1) * pageSize, totalElements);
  const canPrev = page > 0;
  const canNext = page + 1 < totalPages;

  return (
    <div className="flex flex-col items-center justify-between gap-3 pt-2 sm:flex-row">
      <p className="text-sm text-slate-600" aria-live="polite">
        Showing <span className="font-medium">{first}</span>–
        <span className="font-medium">{last}</span> of{" "}
        <span className="font-medium">{totalElements}</span>
      </p>
      <div className="flex items-center gap-2">
        <Button
          size="sm"
          variant="secondary"
          onClick={() => onPageChange(page - 1)}
          disabled={disabled || !canPrev}
        >
          Previous
        </Button>
        <span className="text-sm text-slate-600">
          Page {page + 1} of {Math.max(totalPages, 1)}
        </span>
        <Button
          size="sm"
          variant="secondary"
          onClick={() => onPageChange(page + 1)}
          disabled={disabled || !canNext}
        >
          Next
        </Button>
      </div>
    </div>
  );
}

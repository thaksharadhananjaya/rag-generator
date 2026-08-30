import type { ApiError } from "@/lib/types/api";
import { isRetryable } from "@/lib/api/errors";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils/cn";

interface Props {
  error: ApiError;
  /** Optional short heading, e.g. "Upload failed". */
  title?: string;
  onRetry?: () => void;
  retrying?: boolean;
  className?: string;
  /** Compact single-line style for use inside list rows. */
  compact?: boolean;
}

/**
 * Reusable, user-safe rendering of a normalized {@link ApiError}.
 * Never shows a stack trace. Shows a "Try again" action when it makes sense.
 */
export function ApiErrorMessage({
  error,
  title,
  onRetry,
  retrying,
  className,
  compact,
}: Props) {
  const heading = title ?? defaultTitle(error);
  const showRetry = Boolean(onRetry) && (isRetryable(error) || Boolean(onRetry));

  return (
    <div
      role="alert"
      className={cn(
        "rounded-lg border border-red-200 bg-red-50 text-red-900",
        compact ? "px-3 py-2" : "p-4",
        className,
      )}
    >
      <div className="flex items-start gap-3">
        <span
          aria-hidden
          className="mt-0.5 flex h-5 w-5 shrink-0 items-center justify-center rounded-full bg-red-100 text-xs font-bold"
        >
          !
        </span>
        <div className="min-w-0 flex-1">
          <p className={cn("font-semibold", compact ? "text-sm" : "text-sm")}>{heading}</p>
          <p className="mt-0.5 text-sm text-red-800 break-words">{error.message}</p>

          {error.fieldErrors && Object.keys(error.fieldErrors).length > 0 && (
            <ul className="mt-2 list-inside list-disc space-y-0.5 text-sm text-red-800">
              {Object.entries(error.fieldErrors).map(([field, msg]) => (
                <li key={field}>
                  <span className="font-medium">{field}:</span> {msg}
                </li>
              ))}
            </ul>
          )}

          {showRetry && (
            <div className="mt-3">
              <Button size="sm" variant="secondary" onClick={onRetry} loading={retrying}>
                Try again
              </Button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

function defaultTitle(error: ApiError): string {
  switch (error.status) {
    case 0:
      return error.code === "TIMEOUT" ? "Request timed out" : "Cannot reach the server";
    case 404:
      return "Not found";
    case 409:
      return "Conflict";
    case 413:
      return "File too large";
    case 415:
      return "Unsupported file type";
    case 429:
      return "Too many requests";
    default:
      return error.status >= 500 ? "Server error" : "Something went wrong";
  }
}

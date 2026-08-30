import { cn } from "@/lib/utils/cn";

/** Lightweight inline error text with an optional retry link. */
export function InlineError({
  message,
  onRetry,
  className,
}: {
  message: string;
  onRetry?: () => void;
  className?: string;
}) {
  return (
    <p role="alert" className={cn("text-sm text-red-700", className)}>
      {message}
      {onRetry && (
        <button
          type="button"
          onClick={onRetry}
          className="ml-2 font-semibold underline underline-offset-2 hover:opacity-80"
        >
          Try again
        </button>
      )}
    </p>
  );
}

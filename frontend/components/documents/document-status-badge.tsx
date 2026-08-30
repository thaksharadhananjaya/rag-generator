import type { DocumentStatus } from "@/lib/types/api";
import { cn } from "@/lib/utils/cn";

interface StatusMeta {
  label: string;
  /** Text glyph so status is never communicated by color alone. */
  glyph: string;
  className: string;
  animate?: boolean;
}

const STATUS_META: Record<DocumentStatus, StatusMeta> = {
  PENDING: {
    label: "Waiting",
    glyph: "◌",
    className: "border-slate-300 bg-slate-100 text-slate-700",
  },
  PROCESSING: {
    label: "Processing",
    glyph: "◌",
    className: "border-amber-300 bg-amber-50 text-amber-800",
    animate: true,
  },
  EMBEDDING: {
    label: "Creating embeddings",
    glyph: "↻",
    className: "border-indigo-300 bg-indigo-50 text-indigo-800",
    animate: true,
  },
  COMPLETED: {
    label: "Ready",
    glyph: "✓",
    className: "border-emerald-300 bg-emerald-50 text-emerald-800",
  },
  FAILED: {
    label: "Failed",
    glyph: "!",
    className: "border-red-300 bg-red-50 text-red-800",
  },
};

export function DocumentStatusBadge({ status }: { status: DocumentStatus }) {
  const meta = STATUS_META[status] ?? {
    label: status,
    glyph: "•",
    className: "border-slate-300 bg-slate-100 text-slate-700",
  };

  return (
    <span
      className={cn(
        "inline-flex items-center gap-1.5 rounded-full border px-2.5 py-0.5 text-xs font-medium",
        meta.className,
      )}
    >
      <span aria-hidden className={cn("font-bold", meta.animate && "animate-pulse")}>
        {meta.glyph}
      </span>
      {meta.label}
    </span>
  );
}

export function statusLabel(status: DocumentStatus): string {
  return STATUS_META[status]?.label ?? status;
}

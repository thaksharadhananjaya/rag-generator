"use client";

import { useRef, useState, type DragEvent } from "react";
import { Button } from "@/components/ui/button";
import { InlineError } from "@/components/errors/inline-error";
import { ApiErrorMessage } from "@/components/errors/api-error-message";
import { useToast } from "@/components/ui/toast";
import { useUploadDocument } from "@/lib/hooks/use-documents";
import { validatePdfFile } from "@/lib/validation/schemas";
import { config } from "@/lib/config";
import { formatBytes } from "@/lib/utils/format";
import { cn } from "@/lib/utils/cn";

export function DocumentUpload({ knowledgeBaseId }: { knowledgeBaseId: string }) {
  const toast = useToast();
  const inputRef = useRef<HTMLInputElement>(null);

  const [file, setFile] = useState<File | null>(null);
  const [clientError, setClientError] = useState<string | null>(null);
  const [dragActive, setDragActive] = useState(false);
  const [progress, setProgress] = useState(0);

  const upload = useUploadDocument(knowledgeBaseId);

  function selectFile(next: File | null) {
    upload.reset();
    setProgress(0);
    const result = validatePdfFile(next);
    if (!result.ok) {
      setFile(null);
      setClientError(result.error ?? "Invalid file.");
      return;
    }
    setClientError(null);
    setFile(next);
  }

  function onDrop(e: DragEvent<HTMLDivElement>) {
    e.preventDefault();
    setDragActive(false);
    const dropped = e.dataTransfer.files?.[0] ?? null;
    if (dropped) selectFile(dropped);
  }

  function startUpload() {
    if (!file) {
      setClientError("Select a file to upload.");
      return;
    }
    // Re-validate right before sending; backend remains authoritative.
    const result = validatePdfFile(file);
    if (!result.ok) {
      setClientError(result.error ?? "Invalid file.");
      return;
    }
    upload.mutate(
      { file, onProgress: setProgress },
      {
        onSuccess: (doc) => {
          toast.success("Upload complete", `${doc.filename} is queued for ingestion.`);
          setFile(null);
          setProgress(0);
          if (inputRef.current) inputRef.current.value = "";
        },
      },
    );
  }

  const busy = upload.isPending;

  return (
    <div className="space-y-3">
      <div
        role="button"
        tabIndex={0}
        aria-label="Upload a PDF. Drag and drop a file here or activate to browse."
        onClick={() => !busy && inputRef.current?.click()}
        onKeyDown={(e) => {
          if ((e.key === "Enter" || e.key === " ") && !busy) {
            e.preventDefault();
            inputRef.current?.click();
          }
        }}
        onDragOver={(e) => {
          e.preventDefault();
          if (!busy) setDragActive(true);
        }}
        onDragLeave={() => setDragActive(false)}
        onDrop={(e) => !busy && onDrop(e)}
        className={cn(
          "flex flex-col items-center justify-center rounded-xl border-2 border-dashed px-6 py-8 text-center transition-colors focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-slate-900",
          dragActive
            ? "border-slate-900 bg-slate-50"
            : "border-slate-300 bg-white hover:bg-slate-50",
          busy && "pointer-events-none opacity-60",
        )}
      >
        <svg
          aria-hidden
          viewBox="0 0 24 24"
          className="mb-2 h-8 w-8 text-slate-400"
          fill="none"
          stroke="currentColor"
          strokeWidth="1.6"
        >
          <path d="M12 16V4m0 0L8 8m4-4 4 4" strokeLinecap="round" strokeLinejoin="round" />
          <path d="M4 16v2a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-2" strokeLinecap="round" />
        </svg>
        <p className="text-sm font-medium text-slate-800">
          Drag &amp; drop a PDF here, or{" "}
          <span className="text-slate-900 underline">Browse Files</span>
        </p>
        <p className="mt-1 text-xs text-slate-500">
          PDF only · up to {config.maxUploadLabel}
        </p>
        <input
          ref={inputRef}
          type="file"
          accept="application/pdf,.pdf"
          className="sr-only"
          onChange={(e) => selectFile(e.target.files?.[0] ?? null)}
        />
      </div>

      {clientError && (
        <InlineError message={clientError} />
      )}

      {file && (
        <div className="flex flex-col gap-3 rounded-lg border border-slate-200 bg-white p-3 sm:flex-row sm:items-center sm:justify-between">
          <div className="min-w-0">
            <p className="truncate text-sm font-medium text-slate-800">{file.name}</p>
            <p className="text-xs text-slate-500">{formatBytes(file.size)}</p>
            {busy && (
              <div className="mt-2 h-1.5 w-40 overflow-hidden rounded-full bg-slate-100">
                <div
                  className="h-full rounded-full bg-slate-900 transition-all"
                  style={{ width: `${progress}%` }}
                />
              </div>
            )}
          </div>
          <div className="flex items-center gap-2">
            <Button
              variant="ghost"
              size="sm"
              onClick={() => {
                setFile(null);
                upload.reset();
                if (inputRef.current) inputRef.current.value = "";
              }}
              disabled={busy}
            >
              Remove
            </Button>
            <Button size="sm" onClick={startUpload} loading={busy}>
              Upload PDF
            </Button>
          </div>
        </div>
      )}

      {upload.isError && (
        <ApiErrorMessage
          error={upload.error}
          title="Upload failed"
          onRetry={startUpload}
          retrying={busy}
        />
      )}
    </div>
  );
}

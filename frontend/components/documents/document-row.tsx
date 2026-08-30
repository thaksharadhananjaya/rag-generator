"use client";

import { Card } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { DocumentStatusBadge } from "@/components/documents/document-status-badge";
import { ApiErrorMessage } from "@/components/errors/api-error-message";
import { useToast } from "@/components/ui/toast";
import { useReingestDocument } from "@/lib/hooks/use-documents";
import { formatBytes, formatDateTime, shortId } from "@/lib/utils/format";
import type { DocumentDto } from "@/lib/types/api";

interface Props {
  doc: DocumentDto;
  knowledgeBaseId: string;
  onDelete: (doc: DocumentDto) => void;
  deleting?: boolean;
}

export function DocumentRow({ doc, knowledgeBaseId, onDelete, deleting }: Props) {
  const toast = useToast();
  const reingest = useReingestDocument(knowledgeBaseId);

  function retry() {
    reingest.mutate(doc.id, {
      onSuccess: () =>
        toast.success("Re-ingestion started", `${doc.filename} was queued again.`),
    });
  }

  const isFailed = doc.status === "FAILED";

  return (
    <Card className="p-4">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
        <div className="min-w-0 flex-1">
          <div className="flex flex-wrap items-center gap-2">
            <h3 className="truncate text-sm font-semibold text-slate-900" title={doc.filename}>
              {doc.filename}
            </h3>
            <DocumentStatusBadge status={doc.status} />
          </div>

          <dl className="mt-2 grid grid-cols-2 gap-x-4 gap-y-1 text-xs text-slate-500 sm:grid-cols-3">
            <Meta label="Size" value={formatBytes(doc.sizeBytes)} />
            <Meta
              label="Chunks"
              value={doc.chunkCount != null ? String(doc.chunkCount) : "—"}
            />
            <Meta
              label="Retries"
              value={doc.retryCount != null ? String(doc.retryCount) : "0"}
            />
            <Meta label="Uploaded" value={formatDateTime(doc.createdAt)} />
            <Meta label="Updated" value={formatDateTime(doc.updatedAt)} />
            <Meta label="Ingested" value={formatDateTime(doc.ingestedAt)} />
          </dl>

          <p className="mt-2 text-[11px] text-slate-400">ID {shortId(doc.id)}</p>
        </div>

        <div className="flex shrink-0 items-center gap-2">
          {isFailed && (
            <Button size="sm" variant="secondary" onClick={retry} loading={reingest.isPending}>
              Retry ingestion
            </Button>
          )}
          <Button
            size="sm"
            variant="ghost"
            className="text-red-600 hover:bg-red-50"
            onClick={() => onDelete(doc)}
            loading={deleting}
          >
            Delete
          </Button>
        </div>
      </div>

      {isFailed && doc.failureReason && (
        <p className="mt-3 rounded-md bg-red-50 px-3 py-2 text-xs text-red-800">
          <span className="font-semibold">Ingestion failed:</span> {doc.failureReason}
        </p>
      )}
      {isFailed && !doc.failureReason && (
        <p className="mt-3 rounded-md bg-red-50 px-3 py-2 text-xs text-red-800">
          Ingestion failed. Use “Retry ingestion” to try again.
        </p>
      )}

      {reingest.isError && (
        <div className="mt-3">
          <ApiErrorMessage
            error={reingest.error}
            title="Could not start re-ingestion"
            onRetry={retry}
            retrying={reingest.isPending}
            compact
          />
        </div>
      )}
    </Card>
  );
}

function Meta({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex flex-col">
      <dt className="text-[10px] uppercase tracking-wide text-slate-400">{label}</dt>
      <dd className="font-medium text-slate-600">{value}</dd>
    </div>
  );
}

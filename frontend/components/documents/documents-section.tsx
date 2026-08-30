"use client";

import { useState } from "react";
import { DocumentUpload } from "@/components/documents/document-upload";
import { DocumentRow } from "@/components/documents/document-row";
import { Pagination } from "@/components/ui/pagination";
import { EmptyState } from "@/components/ui/empty-state";
import { Skeleton } from "@/components/ui/skeleton";
import { Spinner } from "@/components/ui/spinner";
import { ApiErrorMessage } from "@/components/errors/api-error-message";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import { useToast } from "@/components/ui/toast";
import { useDocuments, useDeleteDocument } from "@/lib/hooks/use-documents";
import { DEFAULT_PAGE_SIZE } from "@/lib/config";
import { IN_PROGRESS_STATUSES, type ApiError, type DocumentDto } from "@/lib/types/api";

export function DocumentsSection({ knowledgeBaseId }: { knowledgeBaseId: string }) {
  const toast = useToast();
  const [page, setPage] = useState(0);
  const [pendingDelete, setPendingDelete] = useState<DocumentDto | null>(null);
  const [deleteError, setDeleteError] = useState<ApiError | null>(null);

  const { data, isLoading, isError, error, isFetching, refetch } = useDocuments(
    knowledgeBaseId,
    { page, size: DEFAULT_PAGE_SIZE },
  );
  const deleteMutation = useDeleteDocument(knowledgeBaseId);

  const documents = data?.content ?? [];
  const polling =
    isFetching && documents.some((d) => IN_PROGRESS_STATUSES.includes(d.status));

  function confirmDelete() {
    if (!pendingDelete) return;
    setDeleteError(null);
    deleteMutation.mutate(pendingDelete.id, {
      onSuccess: () => {
        toast.success("Document deleted", pendingDelete.filename);
        setPendingDelete(null);
        if (documents.length === 1 && page > 0) setPage((p) => p - 1);
      },
      onError: (err) => setDeleteError(err),
    });
  }

  return (
    <div className="space-y-5">
      <DocumentUpload knowledgeBaseId={knowledgeBaseId} />

      <div className="flex items-center justify-between">
        <h2 className="text-sm font-semibold text-slate-900">
          Documents
          {data?.totalElements ? (
            <span className="ml-1 font-normal text-slate-500">({data.totalElements})</span>
          ) : null}
        </h2>
        {polling && (
          <span className="flex items-center gap-1.5 text-xs text-slate-500">
            <Spinner className="h-3.5 w-3.5" />
            Updating ingestion status…
          </span>
        )}
      </div>

      {isLoading && (
        <div className="space-y-3">
          {Array.from({ length: 3 }).map((_, i) => (
            <Skeleton key={i} className="h-28 w-full" />
          ))}
        </div>
      )}

      {isError && !isLoading && (
        <ApiErrorMessage
          error={error}
          title="Could not load documents"
          onRetry={() => refetch()}
          retrying={isFetching}
        />
      )}

      {!isLoading && !isError && documents.length === 0 && (
        <EmptyState
          title="No documents yet."
          description="Upload a PDF to build this knowledge base."
        />
      )}

      {!isLoading && !isError && documents.length > 0 && (
        <>
          <div className="space-y-3">
            {documents.map((doc) => (
              <DocumentRow
                key={doc.id}
                doc={doc}
                knowledgeBaseId={knowledgeBaseId}
                onDelete={setPendingDelete}
                deleting={deleteMutation.isPending && pendingDelete?.id === doc.id}
              />
            ))}
          </div>
          <Pagination
            page={data?.page ?? page}
            totalPages={data?.totalPages ?? 1}
            totalElements={data?.totalElements ?? documents.length}
            pageSize={data?.size ?? DEFAULT_PAGE_SIZE}
            onPageChange={setPage}
            disabled={isFetching}
          />
        </>
      )}

      <ConfirmDialog
        open={pendingDelete !== null}
        title="Delete document?"
        description={
          pendingDelete
            ? `"${pendingDelete.filename}" will be permanently removed from this knowledge base.`
            : undefined
        }
        confirmLabel="Delete"
        destructive
        loading={deleteMutation.isPending}
        error={deleteError}
        onConfirm={confirmDelete}
        onCancel={() => {
          if (deleteMutation.isPending) return;
          setPendingDelete(null);
          setDeleteError(null);
        }}
      />
    </div>
  );
}

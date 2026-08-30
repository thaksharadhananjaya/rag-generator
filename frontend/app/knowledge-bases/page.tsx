"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { Button } from "@/components/ui/button";
import { SkeletonList } from "@/components/ui/skeleton";
import { EmptyState } from "@/components/ui/empty-state";
import { Pagination } from "@/components/ui/pagination";
import { ApiErrorMessage } from "@/components/errors/api-error-message";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import { useToast } from "@/components/ui/toast";
import { KnowledgeBaseCard } from "@/components/knowledge-bases/kb-card";
import { CreateKnowledgeBaseDialog } from "@/components/knowledge-bases/create-kb-dialog";
import {
  useKnowledgeBases,
  useDeleteKnowledgeBase,
} from "@/lib/hooks/use-knowledge-bases";
import { DEFAULT_PAGE_SIZE } from "@/lib/config";
import type { ApiError, KnowledgeBase } from "@/lib/types/api";

export default function KnowledgeBasesPage() {
  const router = useRouter();
  const toast = useToast();

  const [page, setPage] = useState(0);
  const [createOpen, setCreateOpen] = useState(false);
  const [pendingDelete, setPendingDelete] = useState<KnowledgeBase | null>(null);
  const [deleteError, setDeleteError] = useState<ApiError | null>(null);

  const { data, isLoading, isError, error, isFetching, refetch } =
    useKnowledgeBases({ page, size: DEFAULT_PAGE_SIZE });

  const deleteMutation = useDeleteKnowledgeBase();

  const knowledgeBases = data?.content ?? [];

  function confirmDelete() {
    if (!pendingDelete) return;
    setDeleteError(null);
    deleteMutation.mutate(pendingDelete.id, {
      onSuccess: () => {
        toast.success("Knowledge base deleted", pendingDelete.name);
        setPendingDelete(null);
        // Step back a page if we just emptied the last one.
        if (knowledgeBases.length === 1 && page > 0) setPage((p) => p - 1);
      },
      onError: (err) => setDeleteError(err),
    });
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-xl font-semibold text-slate-900">Knowledge bases</h1>
          <p className="mt-1 text-sm text-slate-600">
            Create a knowledge base, upload PDFs, and ask grounded questions.
          </p>
        </div>
        <Button onClick={() => setCreateOpen(true)}>Create knowledge base</Button>
      </div>

      {isLoading && <SkeletonList count={4} />}

      {isError && !isLoading && (
        <ApiErrorMessage
          error={error}
          title="Could not load knowledge bases"
          onRetry={() => refetch()}
          retrying={isFetching}
        />
      )}

      {!isLoading && !isError && knowledgeBases.length === 0 && (
        <EmptyState
          title="No knowledge bases yet."
          description="Create a knowledge base to start working with documents."
          action={
            <Button onClick={() => setCreateOpen(true)}>Create knowledge base</Button>
          }
        />
      )}

      {!isLoading && !isError && knowledgeBases.length > 0 && (
        <>
          <div className="grid gap-4 sm:grid-cols-2">
            {knowledgeBases.map((kb) => (
              <KnowledgeBaseCard
                key={kb.id}
                kb={kb}
                onDelete={setPendingDelete}
                deleting={
                  deleteMutation.isPending && pendingDelete?.id === kb.id
                }
              />
            ))}
          </div>
          <Pagination
            page={data?.page ?? page}
            totalPages={data?.totalPages ?? 1}
            totalElements={data?.totalElements ?? knowledgeBases.length}
            pageSize={data?.size ?? DEFAULT_PAGE_SIZE}
            onPageChange={setPage}
            disabled={isFetching}
          />
        </>
      )}

      <CreateKnowledgeBaseDialog
        open={createOpen}
        onClose={() => setCreateOpen(false)}
        onCreated={(kb) => router.push(`/knowledge-bases/${kb.id}`)}
      />

      <ConfirmDialog
        open={pendingDelete !== null}
        title="Delete knowledge base?"
        description={
          pendingDelete
            ? `"${pendingDelete.name}" and its documents will be permanently removed. This cannot be undone.`
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

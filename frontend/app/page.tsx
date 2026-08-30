"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import { Card } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { EmptyState } from "@/components/ui/empty-state";
import { ApiErrorMessage } from "@/components/errors/api-error-message";
import { AskQuestions } from "@/components/rag/ask-questions";
import { CreateKnowledgeBaseDialog } from "@/components/knowledge-bases/create-kb-dialog";
import { useKnowledgeBases } from "@/lib/hooks/use-knowledge-bases";
import type { KnowledgeBase } from "@/lib/types/api";

// One page is plenty for a picker; management/pagination lives on /knowledge-bases.
const PICKER_PAGE_SIZE = 100;

export default function HomePage() {
  // The user's explicit choice (null until they pick / create one).
  const [chosenId, setChosenId] = useState<string | null>(null);
  const [createOpen, setCreateOpen] = useState(false);
  // KBs created from this page, kept selectable immediately even before the
  // list query refetch resolves. Deduped against the fetched list below.
  const [locallyCreated, setLocallyCreated] = useState<KnowledgeBase[]>([]);

  const { data, isLoading, isError, error, isFetching, refetch } =
    useKnowledgeBases({ page: 0, size: PICKER_PAGE_SIZE });

  const options = useMemo<KnowledgeBase[]>(() => {
    const seen = new Set<string>();
    const merged: KnowledgeBase[] = [];
    for (const kb of [...locallyCreated, ...(data?.content ?? [])]) {
      if (seen.has(kb.id)) continue;
      seen.add(kb.id);
      merged.push(kb);
    }
    return merged;
  }, [data?.content, locallyCreated]);

  // Derived, not stored: fall back to the first knowledge base so the user can
  // ask immediately, without an effect that syncs state.
  const selectedId =
    chosenId && options.some((kb) => kb.id === chosenId)
      ? chosenId
      : options[0]?.id ?? null;

  const selectedKb = options.find((kb) => kb.id === selectedId) ?? null;

  function handleCreated(kb: KnowledgeBase) {
    setLocallyCreated((prev) => [kb, ...prev.filter((k) => k.id !== kb.id)]);
    setChosenId(kb.id);
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-xl font-semibold text-slate-900 sm:text-2xl">
          Ask your knowledge bases
        </h1>
        <p className="mt-1 text-sm text-slate-600">
          Pick a knowledge base, ask a question, and get an answer grounded in its
          documents - with the sources it used.
        </p>
      </div>

      <Card className="p-4 sm:p-5">
        {isLoading && (
          <div className="space-y-2">
            <Skeleton className="h-4 w-32" />
            <Skeleton className="h-10 w-full" />
          </div>
        )}

        {isError && !isLoading && (
          <ApiErrorMessage
            error={error}
            title="Could not load knowledge bases"
            onRetry={() => refetch()}
            retrying={isFetching}
          />
        )}

        {!isLoading && !isError && options.length === 0 && (
          <EmptyState
            title="No knowledge bases yet."
            description="Create a knowledge base and upload PDFs to start asking questions."
            action={
              <Button onClick={() => setCreateOpen(true)}>
                Create knowledge base
              </Button>
            }
          />
        )}

        {!isLoading && !isError && options.length > 0 && (
          <div className="space-y-3">
            <div className="flex flex-col gap-3 sm:flex-row sm:items-end">
              <div className="flex-1">
                <label
                  htmlFor="kb-select"
                  className="mb-1.5 block text-sm font-medium text-slate-800"
                >
                  Knowledge base
                </label>
                <select
                  id="kb-select"
                  value={selectedId ?? ""}
                  onChange={(e) => setChosenId(e.target.value)}
                  className="block w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 shadow-sm focus:outline-2 focus:outline-offset-0 focus:outline-slate-900"
                >
                  {options.map((kb) => (
                    <option key={kb.id} value={kb.id}>
                      {kb.name}
                    </option>
                  ))}
                </select>
              </div>
              <div className="flex gap-2">
                <Button variant="secondary" onClick={() => setCreateOpen(true)}>
                  New
                </Button>
              </div>
            </div>

            <div className="flex flex-wrap items-center gap-x-4 gap-y-1 text-sm">
              {selectedKb && (
                <Link
                  href={`/knowledge-bases/${selectedKb.id}`}
                  className="text-xs pl-0.5 font-medium text-slate-700 underline underline-offset-2 hover:text-slate-900"
                >
                  Manage Documents
                </Link>
              )}
              
            </div>
          </div>
        )}
      </Card>

      {selectedId && (
        <AskQuestions key={selectedId} knowledgeBaseId={selectedId} />
      )}

      <CreateKnowledgeBaseDialog
        open={createOpen}
        onClose={() => setCreateOpen(false)}
        onCreated={handleCreated}
      />
    </div>
  );
}

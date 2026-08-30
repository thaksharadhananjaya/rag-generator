"use client";

import { useState } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import { Skeleton } from "@/components/ui/skeleton";
import { ApiErrorMessage } from "@/components/errors/api-error-message";
import { DocumentsSection } from "@/components/documents/documents-section";
import { AskQuestions } from "@/components/rag/ask-questions";
import { useKnowledgeBase } from "@/lib/hooks/use-knowledge-bases";
import { formatDateTime } from "@/lib/utils/format";
import { cn } from "@/lib/utils/cn";

type Tab = "documents" | "ask";

export default function KnowledgeBaseDetailPage() {
  const params = useParams<{ id: string }>();
  const id = params?.id;
  const [tab, setTab] = useState<Tab>("documents");

  const { data: kb, isLoading, isError, error, refetch, isFetching } =
    useKnowledgeBase(id);

  return (
    <div className="space-y-6">
      <div>
        <Link
          href="/knowledge-bases"
          className="text-sm text-slate-500 hover:text-slate-800"
        >
          ← All knowledge bases
        </Link>

        {isLoading && (
          <div className="mt-2 space-y-2">
            <Skeleton className="h-7 w-64" />
            <Skeleton className="h-4 w-80" />
          </div>
        )}

        {isError && !isLoading && (
          <div className="mt-3">
            <ApiErrorMessage
              error={error}
              title="Could not load knowledge base"
              onRetry={() => refetch()}
              retrying={isFetching}
            />
          </div>
        )}

        {kb && !isLoading && (
          <div className="mt-2">
            <h1 className="text-xl font-semibold text-slate-900">{kb.name}</h1>
            {kb.description && (
              <p className="mt-1 max-w-2xl text-sm text-slate-600">{kb.description}</p>
            )}
            <p className="mt-1 text-xs text-slate-400">
              Created {formatDateTime(kb.createdAt)}
            </p>
          </div>
        )}
      </div>

      {kb && !isLoading && !isError && (
        <>
          <div
            role="tablist"
            aria-label="Knowledge base sections"
            className="flex gap-1 border-b border-slate-200"
          >
            <TabButton active={tab === "documents"} onClick={() => setTab("documents")}>
              Documents
            </TabButton>
            <TabButton active={tab === "ask"} onClick={() => setTab("ask")}>
              Ask questions
            </TabButton>
          </div>

          <div role="tabpanel">
            {tab === "documents" ? (
              <DocumentsSection knowledgeBaseId={kb.id} />
            ) : (
              <AskQuestions knowledgeBaseId={kb.id} />
            )}
          </div>
        </>
      )}
    </div>
  );
}

function TabButton({
  active,
  onClick,
  children,
}: {
  active: boolean;
  onClick: () => void;
  children: React.ReactNode;
}) {
  return (
    <button
      type="button"
      role="tab"
      aria-selected={active}
      onClick={onClick}
      className={cn(
        "-mb-px border-b-2 px-4 py-2 text-sm font-medium transition-colors focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-slate-900",
        active
          ? "border-slate-900 text-slate-900"
          : "border-transparent text-slate-500 hover:text-slate-800",
      )}
    >
      {children}
    </button>
  );
}

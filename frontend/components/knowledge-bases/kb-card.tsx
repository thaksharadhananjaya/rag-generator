"use client";

import Link from "next/link";
import { Card } from "@/components/ui/card";
import { Button, buttonClasses } from "@/components/ui/button";
import { formatRelative } from "@/lib/utils/format";
import type { KnowledgeBase } from "@/lib/types/api";

interface Props {
  kb: KnowledgeBase;
  onDelete: (kb: KnowledgeBase) => void;
  deleting?: boolean;
}

export function KnowledgeBaseCard({ kb, onDelete, deleting }: Props) {
  return (
    <Card className="flex flex-col">
      <div className="flex-1 p-5">
        <h3 className="text-base font-semibold text-slate-900">
          <Link
            href={`/knowledge-bases/${kb.id}`}
            className="rounded hover:underline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-slate-900"
          >
            {kb.name}
          </Link>
        </h3>
        {kb.description ? (
          <p className="mt-1 line-clamp-3 text-sm text-slate-600">{kb.description}</p>
        ) : (
          <p className="mt-1 text-sm italic text-slate-400">No description</p>
        )}
        <dl className="mt-3 flex flex-wrap gap-x-4 gap-y-1 text-xs text-slate-500">
          <div className="flex gap-1">
            <dt>Created</dt>
            <dd className="font-medium text-slate-600">{formatRelative(kb.createdAt)}</dd>
          </div>
          {typeof kb.documentCount === "number" && (
            <div className="flex gap-1">
              <dt>Documents</dt>
              <dd className="font-medium text-slate-600">{kb.documentCount}</dd>
            </div>
          )}
        </dl>
      </div>
      <div className="flex items-center gap-2 border-t border-slate-100 p-4">
        <Link
          href={`/knowledge-bases/${kb.id}`}
          className={buttonClasses("secondary", "sm")}
        >
          Open
        </Link>
        <Button
          size="sm"
          variant="ghost"
          className="text-red-600 hover:bg-red-50"
          onClick={() => onDelete(kb)}
          loading={deleting}
        >
          Delete
        </Button>
      </div>
    </Card>
  );
}

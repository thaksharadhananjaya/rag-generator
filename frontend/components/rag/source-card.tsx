import { formatScore, shortId } from "@/lib/utils/format";
import type { RagSource } from "@/lib/types/api";

export function SourceCard({ source, index }: { source: RagSource; index: number }) {
  return (
    <li className="rounded-lg border border-slate-200 bg-white p-3">
      <div className="flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-slate-500">
        <span className="flex h-5 w-5 items-center justify-center rounded-full bg-slate-900 text-[11px] font-semibold text-white">
          {index + 1}
        </span>
        <MetaPair label="Document" value={shortId(source.documentId)} title={source.documentId} />
        <MetaPair label="Chunk" value={shortId(source.chunkId)} title={source.chunkId} />
        <MetaPair label="Ordinal" value={String(source.ordinal)} />
        {source.page != null && <MetaPair label="Page" value={String(source.page)} />}
        <MetaPair label="Score" value={formatScore(source.score)} />
      </div>
      <blockquote className="mt-2 border-l-2 border-slate-200 pl-3 text-sm text-slate-700">
        {source.excerpt}
      </blockquote>
    </li>
  );
}

function MetaPair({
  label,
  value,
  title,
}: {
  label: string;
  value: string;
  title?: string;
}) {
  return (
    <span className="inline-flex items-center gap-1" title={title}>
      <span className="uppercase tracking-wide text-slate-400 text-[0.625rem]">
        {label}
      </span>
      <span className="font-medium text-slate-700">{value}</span>
    </span>
  );
}

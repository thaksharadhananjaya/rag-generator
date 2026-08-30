import { SourceCard } from "@/components/rag/source-card";
import { Card } from "@/components/ui/card";
import type { RagQueryResponse } from "@/lib/types/api";

export function AnswerDisplay({ result }: { result: RagQueryResponse }) {
  const sources = result.sources ?? [];

  return (
    <div className="space-y-4">
      <Card className="p-5">
        <h3 className="text-xs font-semibold uppercase tracking-wide text-slate-400">
          Answer
        </h3>
        <div className="mt-2 whitespace-pre-wrap text-[15px] leading-relaxed text-slate-900">
          {result.answer?.trim() ? result.answer : "The model did not return an answer."}
        </div>

        <div className="mt-4 flex flex-wrap gap-x-4 gap-y-1 border-t border-slate-100 pt-3 text-xs text-slate-500">
          {result.model && (
            <span>
              Model <span className="font-medium text-slate-700">{result.model}</span>
            </span>
          )}
          {typeof result.tokensUsed === "number" && (
            <span>
              Tokens{" "}
              <span className="font-medium text-slate-700">{result.tokensUsed}</span>
            </span>
          )}
          {typeof result.retrievedChunks === "number" && (
            <span>
              Retrieved chunks{" "}
              <span className="font-medium text-slate-700">
                {result.retrievedChunks}
              </span>
            </span>
          )}
        </div>
      </Card>

      <section aria-labelledby="sources-heading">
        <h3 id="sources-heading" className="text-sm font-semibold text-slate-900">
          Sources
        </h3>
        <p className="mt-0.5 text-xs text-slate-500">
          These retrieved chunks are the evidence the answer was generated from.
        </p>

        {sources.length === 0 ? (
          <p className="mt-3 rounded-lg border border-dashed border-slate-300 bg-slate-50 px-3 py-4 text-sm text-slate-600">
            No sources were returned for this answer.
          </p>
        ) : (
          <ul className="mt-3 space-y-2">
            {sources.map((source, i) => (
              <SourceCard
                key={`${source.chunkId}-${source.ordinal}-${i}`}
                source={source}
                index={i}
              />
            ))}
          </ul>
        )}
      </section>
    </div>
  );
}

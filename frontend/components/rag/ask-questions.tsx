"use client";

import { useState } from "react";
import { useForm, useWatch } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Button } from "@/components/ui/button";
import { TextField, TextAreaField } from "@/components/ui/field";
import { ApiErrorMessage } from "@/components/errors/api-error-message";
import { EmptyState } from "@/components/ui/empty-state";
import { Card } from "@/components/ui/card";
import { Spinner } from "@/components/ui/spinner";
import { AnswerDisplay } from "@/components/rag/answer-display";
import { useQueryKnowledgeBase } from "@/lib/hooks/use-rag";
import {
  ragQuerySchema,
  QUESTION_MAX_LENGTH,
  type RagQueryFormValues,
  type RagQueryValues,
} from "@/lib/validation/schemas";
import type { RagQueryRequest } from "@/lib/types/api";

export function AskQuestions({ knowledgeBaseId }: { knowledgeBaseId: string }) {
  const [showAdvanced, setShowAdvanced] = useState(false);
  const [lastRequest, setLastRequest] = useState<RagQueryRequest | null>(null);
  const mutation = useQueryKnowledgeBase(knowledgeBaseId);

  const {
    register,
    handleSubmit,
    control,
    formState: { errors },
  } = useForm<RagQueryFormValues, unknown, RagQueryValues>({
    resolver: zodResolver(ragQuerySchema),
    defaultValues: { question: "", topK: "", minScore: "" },
  });

  const question = useWatch({ control, name: "question" }) ?? "";

  function toRequest(values: RagQueryValues): RagQueryRequest {
    const body: RagQueryRequest = { question: values.question.trim() };
    if (typeof values.topK === "number") body.topK = values.topK;
    if (typeof values.minScore === "number") body.minScore = values.minScore;
    return body;
  }

  const submit = handleSubmit((values) => {
    const request = toRequest(values);
    setLastRequest(request);
    mutation.mutate(request);
  });

  return (
    <div className="space-y-5">
      <form onSubmit={submit} className="space-y-4" noValidate>
        <TextAreaField
          label="Question"
          placeholder="Ask a question about your documents..."
          rows={4}
          maxLength={QUESTION_MAX_LENGTH}
          className="resize-none"
          hint={`${question.length}/${QUESTION_MAX_LENGTH}`}
          error={errors.question?.message}
          {...register("question")}
        />

        <div className="rounded-lg border border-slate-200 bg-white">
          <button
            type="button"
            onClick={() => setShowAdvanced((v) => !v)}
            aria-expanded={showAdvanced}
            className="flex w-full items-center justify-between px-3 py-2 text-sm font-medium text-slate-700"
          >
            Advanced options
            <span aria-hidden className="text-slate-400">
              {showAdvanced ? "−" : "+"}
            </span>
          </button>
          {showAdvanced && (
            <div className="grid gap-4 border-t border-slate-100 p-3 sm:grid-cols-2">
              <TextField
                label="topK"
                type="number"
                inputMode="numeric"
                min={0}
                max={50}
                step={1}
                placeholder="5"
                hint="Number of chunks to retrieve (0–50)."
                error={errors.topK?.message}
                {...register("topK")}
              />
              <TextField
                label="minScore"
                type="number"
                inputMode="decimal"
                min={0}
                max={1}
                step={0.05}
                placeholder="0.6"
                hint="Minimum similarity score (0–1)."
                error={errors.minScore?.message}
                {...register("minScore")}
              />
            </div>
          )}
        </div>

        <div className="flex items-center gap-3">
          <Button type="submit" loading={mutation.isPending}>
            Ask
          </Button>
          {mutation.isPending && (
            <span className="flex items-center gap-1.5 text-sm text-slate-500">
              <Spinner className="h-4 w-4" />
              Generating a grounded answer…
            </span>
          )}
        </div>
      </form>

      {mutation.isError && (
        <ApiErrorMessage
          error={mutation.error}
          title="Unable to generate an answer"
          onRetry={
            lastRequest ? () => mutation.mutate(lastRequest) : undefined
          }
          retrying={mutation.isPending}
        />
      )}

      {mutation.isPending && (
        <Card className="space-y-3 p-5">
          <div className="h-4 w-24 animate-pulse rounded bg-slate-200" />
          <div className="h-4 w-full animate-pulse rounded bg-slate-200" />
          <div className="h-4 w-11/12 animate-pulse rounded bg-slate-200" />
          <div className="h-4 w-4/5 animate-pulse rounded bg-slate-200" />
        </Card>
      )}

      {mutation.isSuccess && !mutation.isPending && (
        <AnswerDisplay result={mutation.data} />
      )}

      {mutation.isIdle && (
        <EmptyState
          title="Ask a question about your documents."
          description="Answers are generated only from the documents in this knowledge base, with the supporting sources shown below."
        />
      )}
    </div>
  );
}

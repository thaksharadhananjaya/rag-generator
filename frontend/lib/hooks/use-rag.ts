"use client";

import { useMutation } from "@tanstack/react-query";
import { ragApi } from "@/lib/api/rag-api";
import type {
  ApiError,
  RagQueryRequest,
  RagQueryResponse,
} from "@/lib/types/api";

/**
 * Ask a question against a knowledge base via
 *   POST /api/v1/knowledge-bases/{knowledgeBaseId}/query
 *
 * Modelled as a mutation: it is a user-triggered action, we want explicit
 * loading state, and failures must not clear the user's question (the form
 * owns that state, not the cache).
 */
export function useQueryKnowledgeBase(knowledgeBaseId: string) {
  return useMutation<RagQueryResponse, ApiError, RagQueryRequest>({
    mutationFn: (body) => ragApi.query(knowledgeBaseId, body),
    retry: false,
  });
}

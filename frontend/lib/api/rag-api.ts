import { apiClient } from "@/lib/api/client";
import type { RagQueryRequest, RagQueryResponse } from "@/lib/types/api";

/**
 * RAG Query API — the ONLY question-answering endpoint:
 *   POST /api/v1/knowledge-bases/{knowledgeBaseId}/query
 *
 * There is no separate conversation/chat endpoint. Every question goes here.
 */
export const ragApi = {
  query(
    knowledgeBaseId: string,
    body: RagQueryRequest,
  ): Promise<RagQueryResponse> {
    return apiClient
      .post<RagQueryResponse>(
        `/api/v1/knowledge-bases/${knowledgeBaseId}/query`,
        body,
      )
      .then((res) => res.data);
  },
};

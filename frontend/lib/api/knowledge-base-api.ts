import { apiClient, pageableParams } from "@/lib/api/client";
import { normalizePage } from "@/lib/api/pagination";
import type {
  CreateKnowledgeBaseRequest,
  KnowledgeBase,
  Page,
  Pageable,
} from "@/lib/types/api";

/**
 * Knowledge Base API — maps 1:1 to the backend endpoints:
 *   POST   /api/v1/knowledge-bases
 *   GET    /api/v1/knowledge-bases            (pageable)
 *   GET    /api/v1/knowledge-bases/{id}
 *   DELETE /api/v1/knowledge-bases/{id}       (204, no body)
 */

const BASE = "/api/v1/knowledge-bases";

export const knowledgeBaseApi = {
  list(pageable: Pageable): Promise<Page<KnowledgeBase>> {
    return apiClient
      .get(BASE, { params: pageableParams(pageable) })
      .then((res) => normalizePage<KnowledgeBase>(res.data, pageable.size));
  },

  get(id: string): Promise<KnowledgeBase> {
    return apiClient.get<KnowledgeBase>(`${BASE}/${id}`).then((res) => res.data);
  },

  create(body: CreateKnowledgeBaseRequest): Promise<KnowledgeBase> {
    return apiClient.post<KnowledgeBase>(BASE, body).then((res) => res.data);
  },

  remove(id: string): Promise<void> {
    return apiClient.delete(`${BASE}/${id}`).then(() => undefined);
  },
};

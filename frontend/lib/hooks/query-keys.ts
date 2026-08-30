import type { Pageable } from "@/lib/types/api";

/** Centralized React Query cache keys. */
export const queryKeys = {
  knowledgeBases: {
    all: ["knowledge-bases"] as const,
    list: (pageable: Pageable) =>
      ["knowledge-bases", "list", pageable] as const,
    detail: (id: string) => ["knowledge-bases", "detail", id] as const,
  },
  documents: {
    all: ["documents"] as const,
    list: (knowledgeBaseId: string, pageable: Pageable) =>
      ["documents", "list", knowledgeBaseId, pageable] as const,
    listForKb: (knowledgeBaseId: string) =>
      ["documents", "list", knowledgeBaseId] as const,
    detail: (documentId: string) =>
      ["documents", "detail", documentId] as const,
  },
} as const;

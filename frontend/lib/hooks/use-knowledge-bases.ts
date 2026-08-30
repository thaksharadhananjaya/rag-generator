"use client";

import {
  useMutation,
  useQuery,
  useQueryClient,
  keepPreviousData,
} from "@tanstack/react-query";
import { knowledgeBaseApi } from "@/lib/api/knowledge-base-api";
import { queryKeys } from "@/lib/hooks/query-keys";
import { DEFAULT_PAGE_SIZE } from "@/lib/config";
import type {
  ApiError,
  CreateKnowledgeBaseRequest,
  KnowledgeBase,
  Page,
  Pageable,
} from "@/lib/types/api";

const DEFAULT_SORT = "createdAt,desc";

export function useKnowledgeBases(params?: Partial<Pageable>) {
  const pageable: Pageable = {
    page: params?.page ?? 0,
    size: params?.size ?? DEFAULT_PAGE_SIZE,
    sort: params?.sort ?? DEFAULT_SORT,
  };

  return useQuery<Page<KnowledgeBase>, ApiError>({
    queryKey: queryKeys.knowledgeBases.list(pageable),
    queryFn: () => knowledgeBaseApi.list(pageable),
    placeholderData: keepPreviousData,
  });
}

export function useKnowledgeBase(id: string | undefined) {
  return useQuery<KnowledgeBase, ApiError>({
    queryKey: queryKeys.knowledgeBases.detail(id ?? ""),
    queryFn: () => knowledgeBaseApi.get(id as string),
    enabled: Boolean(id),
  });
}

export function useCreateKnowledgeBase() {
  const qc = useQueryClient();
  return useMutation<KnowledgeBase, ApiError, CreateKnowledgeBaseRequest>({
    mutationFn: (body) => knowledgeBaseApi.create(body),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: queryKeys.knowledgeBases.all });
    },
  });
}

export function useDeleteKnowledgeBase() {
  const qc = useQueryClient();
  return useMutation<void, ApiError, string>({
    mutationFn: (id) => knowledgeBaseApi.remove(id),
    onSuccess: (_data, id) => {
      qc.removeQueries({ queryKey: queryKeys.knowledgeBases.detail(id) });
      qc.invalidateQueries({ queryKey: queryKeys.knowledgeBases.all });
    },
  });
}

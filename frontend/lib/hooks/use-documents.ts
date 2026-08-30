"use client";

import {
  useMutation,
  useQuery,
  useQueryClient,
  keepPreviousData,
} from "@tanstack/react-query";
import { documentApi } from "@/lib/api/document-api";
import { queryKeys } from "@/lib/hooks/query-keys";
import {
  DEFAULT_PAGE_SIZE,
  INGESTION_POLL_INTERVAL_MS,
} from "@/lib/config";
import {
  IN_PROGRESS_STATUSES,
  type ApiError,
  type DocumentDto,
  type Page,
  type Pageable,
} from "@/lib/types/api";

const DEFAULT_SORT = "createdAt,desc";

function hasInProgressDocuments(page: Page<DocumentDto> | undefined): boolean {
  if (!page) return false;
  return page.content.some((d) => IN_PROGRESS_STATUSES.includes(d.status));
}

export function useDocuments(
  knowledgeBaseId: string | undefined,
  params?: Partial<Pageable>,
) {
  const pageable: Pageable = {
    page: params?.page ?? 0,
    size: params?.size ?? DEFAULT_PAGE_SIZE,
    sort: params?.sort ?? DEFAULT_SORT,
  };

  return useQuery<Page<DocumentDto>, ApiError>({
    queryKey: queryKeys.documents.list(knowledgeBaseId ?? "", pageable),
    queryFn: () => documentApi.list(knowledgeBaseId as string, pageable),
    enabled: Boolean(knowledgeBaseId),
    placeholderData: keepPreviousData,
    // Poll the existing list endpoint while any document is still ingesting.
    refetchInterval: (query) =>
      hasInProgressDocuments(query.state.data as Page<DocumentDto> | undefined)
        ? INGESTION_POLL_INTERVAL_MS
        : false,
  });
}

export function useDocument(
  documentId: string | undefined,
  options?: { poll?: boolean },
) {
  return useQuery<DocumentDto, ApiError>({
    queryKey: queryKeys.documents.detail(documentId ?? ""),
    queryFn: () => documentApi.get(documentId as string),
    enabled: Boolean(documentId),
    refetchInterval: (query) => {
      if (!options?.poll) return false;
      const doc = query.state.data as DocumentDto | undefined;
      return doc && IN_PROGRESS_STATUSES.includes(doc.status)
        ? INGESTION_POLL_INTERVAL_MS
        : false;
    },
  });
}

export function useUploadDocument(knowledgeBaseId: string) {
  const qc = useQueryClient();
  return useMutation<
    DocumentDto,
    ApiError,
    { file: File; onProgress?: (percent: number) => void }
  >({
    mutationFn: ({ file, onProgress }) =>
      documentApi.upload(knowledgeBaseId, file, onProgress),
    onSuccess: () => {
      qc.invalidateQueries({
        queryKey: queryKeys.documents.listForKb(knowledgeBaseId),
      });
      qc.invalidateQueries({
        queryKey: queryKeys.knowledgeBases.detail(knowledgeBaseId),
      });
    },
  });
}

export function useDeleteDocument(knowledgeBaseId: string) {
  const qc = useQueryClient();
  return useMutation<void, ApiError, string>({
    mutationFn: (documentId) => documentApi.remove(documentId),
    onSuccess: (_data, documentId) => {
      qc.removeQueries({ queryKey: queryKeys.documents.detail(documentId) });
      qc.invalidateQueries({
        queryKey: queryKeys.documents.listForKb(knowledgeBaseId),
      });
      qc.invalidateQueries({
        queryKey: queryKeys.knowledgeBases.detail(knowledgeBaseId),
      });
    },
  });
}

export function useReingestDocument(knowledgeBaseId: string) {
  const qc = useQueryClient();
  return useMutation<void, ApiError, string>({
    mutationFn: (documentId) => documentApi.reingest(documentId),
    onSuccess: (_data, documentId) => {
      qc.invalidateQueries({ queryKey: queryKeys.documents.detail(documentId) });
      qc.invalidateQueries({
        queryKey: queryKeys.documents.listForKb(knowledgeBaseId),
      });
    },
  });
}

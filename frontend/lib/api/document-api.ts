import { apiClient, pageableParams } from "@/lib/api/client";
import { normalizePage } from "@/lib/api/pagination";
import type { DocumentDto, Page, Pageable } from "@/lib/types/api";

/**
 * Document API — maps 1:1 to the backend endpoints:
 *   POST   /api/v1/knowledge-bases/{knowledgeBaseId}/documents   (multipart, field "file")
 *   GET    /api/v1/knowledge-bases/{knowledgeBaseId}/documents   (pageable)
 *   GET    /api/v1/documents/{documentId}
 *   DELETE /api/v1/documents/{documentId}                        (204, no body)
 *   POST   /api/v1/documents/{documentId}/ingest                 (202, re-ingest)
 */

export const documentApi = {
  list(knowledgeBaseId: string, pageable: Pageable): Promise<Page<DocumentDto>> {
    return apiClient
      .get(`/api/v1/knowledge-bases/${knowledgeBaseId}/documents`, {
        params: pageableParams(pageable),
      })
      .then((res) => normalizePage<DocumentDto>(res.data, pageable.size));
  },

  get(documentId: string): Promise<DocumentDto> {
    return apiClient
      .get<DocumentDto>(`/api/v1/documents/${documentId}`)
      .then((res) => res.data);
  },

  upload(
    knowledgeBaseId: string,
    file: File,
    onProgress?: (percent: number) => void,
  ): Promise<DocumentDto> {
    const form = new FormData();
    form.append("file", file);
    // Let Axios/the browser set `Content-Type: multipart/form-data` with the
    // correct boundary — setting it by hand omits the boundary and breaks parsing.
    return apiClient
      .post<DocumentDto>(
        `/api/v1/knowledge-bases/${knowledgeBaseId}/documents`,
        form,
        {
          onUploadProgress: (e) => {
            if (onProgress && e.total) {
              onProgress(Math.round((e.loaded / e.total) * 100));
            }
          },
        },
      )
      .then((res) => res.data);
  },

  remove(documentId: string): Promise<void> {
    return apiClient.delete(`/api/v1/documents/${documentId}`).then(() => undefined);
  },

  /** Retry a failed ingestion. Returns 202 Accepted with no meaningful body. */
  reingest(documentId: string): Promise<void> {
    return apiClient
      .post(`/api/v1/documents/${documentId}/ingest`)
      .then(() => undefined);
  },
};

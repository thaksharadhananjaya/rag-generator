"use client";

import { useState, type ReactNode } from "react";
import {
  QueryClient,
  QueryClientProvider,
  type QueryClientConfig,
} from "@tanstack/react-query";
import { ToastProvider } from "@/components/ui/toast";
import { isApiError } from "@/lib/api/errors";

const queryClientConfig: QueryClientConfig = {
  defaultOptions: {
    queries: {
      staleTime: 15_000,
      refetchOnWindowFocus: false,
      retry: (failureCount, error) => {
        // Don't retry deterministic client errors (4xx). Retry transient ones.
        if (isApiError(error)) {
          const retryable = error.status === 0 || error.status >= 500 || error.status === 429;
          return retryable && failureCount < 2;
        }
        return failureCount < 2;
      },
    },
    mutations: {
      retry: false,
    },
  },
};

export function Providers({ children }: { children: ReactNode }) {
  const [queryClient] = useState(() => new QueryClient(queryClientConfig));

  return (
    <QueryClientProvider client={queryClient}>
      <ToastProvider>{children}</ToastProvider>
    </QueryClientProvider>
  );
}

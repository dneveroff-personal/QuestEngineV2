import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { type ReactNode, useState } from "react";

/**
 * Единая точка настройки TanStack Query (architecture.md §11).
 *
 * `useState(() => new QueryClient())` — QueryClient создаётся один раз за
 * время жизни компонента, а не на каждый рендер.
 */
export function QueryProvider({ children }: { children: ReactNode }) {
  const [queryClient] = useState(
    () =>
      new QueryClient({
        defaultOptions: {
          queries: {
            // Server state в QuestEngine (Quest/Team/QuestProgress/...)
            // не обновляется настолько часто, чтобы агрессивный refetch
            // при каждом переключении вкладки был оправдан по умолчанию.
            // Экраны, которым это действительно нужно (игровой режим),
            // переопределяют это на уровне конкретного useQuery.
            refetchOnWindowFocus: false,
            retry: 1,
          },
        },
      }),
  );

  return (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
}

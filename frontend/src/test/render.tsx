import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render } from "@testing-library/react";
import type { ReactElement } from "react";
import { MemoryRouter } from "react-router-dom";

/**
 * Общая обвязка для component-тестов (testing-strategy.md, уровень 2).
 * Свежий QueryClient на каждый рендер — тесты не должны делиться кэшем
 * между собой (иначе порядок запуска тестов начнёт влиять на результат).
 * retry: false — иначе неудачный запрос в тесте ждёт реальных retry-таймеров
 * (несколько секунд), не связанных с логикой самого теста.
 */
export function renderWithProviders(ui: ReactElement, { route = "/" } = {}) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });

  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[route]}>{ui}</MemoryRouter>
    </QueryClientProvider>,
  );
}

import "@testing-library/jest-dom/vitest";
import { afterAll, afterEach, beforeAll } from "vitest";

import { server } from "@/test/msw/server";

/**
 * MSW перехватывает все apiFetch-запросы в тестах — тот же принцип, что
 * и в dev-режиме (testing-strategy.md, "MSW как основа разработки").
 * onUnhandledRequest: "error" — тест падает явно, если компонент дёрнул
 * эндпоинт, для которого не завели handler, а не тихо шлёт реальный
 * сетевой запрос, которого в тестовой среде всё равно нет.
 */
beforeAll(() => server.listen({ onUnhandledRequest: "error" }));
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

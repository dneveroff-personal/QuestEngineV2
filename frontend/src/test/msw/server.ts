import { setupServer } from "msw/node";

import { handlers } from "@/test/msw/handlers";

/** setupServer (не setupWorker) — тесты выполняются в Node/jsdom, не в браузере. */
export const server = setupServer(...handlers);

import { useEffect, useRef, useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { Client, type IMessage } from "@stomp/stompjs";

import { getSession } from "@/lib/auth-token";

export type GameplaySocketStatus = "idle" | "connecting" | "connected" | "error";

interface GameplayEventMessage {
  type: string;
  version: number;
  eventId: string;
  questId: number;
  questProgressId: number;
  levelProgressId?: number | null;
  occurredAt: string;
  payload?: Record<string, unknown>;
}

const MAX_SEEN_IDS = 200;

/**
 * STOMP client for gameplay notifications (ADR-022, docs/frontend/realtime.md).
 * On event: dedupe by eventId → invalidate TanStack Query gameplay keys.
 * REST remains source of truth.
 */
export function useGameplaySocket(
  questId: number,
  teamId: number,
  questProgressId: number | null | undefined,
  enabled: boolean,
): GameplaySocketStatus {
  const queryClient = useQueryClient();
  const [status, setStatus] = useState<GameplaySocketStatus>("idle");
  const seenIdsRef = useRef<Set<string>>(new Set());

  useEffect(() => {
    if (!enabled || !questProgressId) {
      setStatus("idle");
      return;
    }

    const session = getSession();
    if (!session?.accessToken) {
      setStatus("error");
      return;
    }

    const protocol = window.location.protocol === "https:" ? "wss:" : "ws:";
    const brokerURL = `${protocol}//${window.location.host}/ws`;

    const client = new Client({
      brokerURL,
      connectHeaders: {
        Authorization: `Bearer ${session.accessToken}`,
      },
      reconnectDelay: 3000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      onConnect: () => {
        setStatus("connected");
        const dest = `/topic/quest-progress/${questProgressId}/gameplay`;
        client.subscribe(dest, (message: IMessage) => {
          try {
            const event = JSON.parse(message.body) as GameplayEventMessage;
            if (!event.eventId) return;
            if (seenIdsRef.current.has(event.eventId)) return;
            seenIdsRef.current.add(event.eventId);
            if (seenIdsRef.current.size > MAX_SEEN_IDS) {
              const first = seenIdsRef.current.values().next().value;
              if (first) seenIdsRef.current.delete(first);
            }
            void queryClient.invalidateQueries({
              queryKey: ["gameplay", questId, teamId],
            });
          } catch {
            // ignore malformed frames
          }
        });
        // After reconnect, refresh authoritative state
        void queryClient.invalidateQueries({
          queryKey: ["gameplay", questId, teamId],
        });
      },
      onDisconnect: () => setStatus("connecting"),
      onStompError: () => setStatus("error"),
      onWebSocketError: () => setStatus("error"),
    });

    setStatus("connecting");
    client.activate();

    return () => {
      void client.deactivate();
      setStatus("idle");
    };
  }, [enabled, questProgressId, questId, teamId, queryClient]);

  return status;
}

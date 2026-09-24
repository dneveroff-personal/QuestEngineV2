import { apiFetch } from "@/api/client";

/** Сверено с CreateManualTimeAdjustmentRequest / ManualTimeAdjustmentResponse / TimeAdjustmentType */

export type TimeAdjustmentType = "BONUS" | "PENALTY";

export interface ManualTimeAdjustment {
  id: number;
  questProgressId: number;
  type: TimeAdjustmentType;
  seconds: number;
  reason: string;
  createdByUserId: number;
  createdAt: string;
  revokedAt: string | null;
  revokedByUserId: number | null;
}

export interface CreateManualTimeAdjustmentRequest {
  type: TimeAdjustmentType;
  /** Positive seconds; sign is determined by type (BONUS vs PENALTY). */
  seconds: number;
  reason: string;
}

/** POST /api/quest-progress/{questProgressId}/adjustments — author/admin only. */
export function createManualAdjustment(
  questProgressId: number,
  request: CreateManualTimeAdjustmentRequest,
): Promise<ManualTimeAdjustment> {
  return apiFetch<ManualTimeAdjustment>(`/api/quest-progress/${questProgressId}/adjustments`, {
    method: "POST",
    body: request,
  });
}

/** GET /api/quest-progress/{questProgressId}/adjustments — includes revoked (audit). */
export function listManualAdjustments(
  questProgressId: number,
): Promise<ManualTimeAdjustment[]> {
  return apiFetch<ManualTimeAdjustment[]>(
    `/api/quest-progress/${questProgressId}/adjustments`,
  );
}

/** POST /api/adjustments/{id}/revoke — only before quest officially finished. */
export function revokeManualAdjustment(adjustmentId: number): Promise<ManualTimeAdjustment> {
  return apiFetch<ManualTimeAdjustment>(`/api/adjustments/${adjustmentId}/revoke`, {
    method: "POST",
  });
}

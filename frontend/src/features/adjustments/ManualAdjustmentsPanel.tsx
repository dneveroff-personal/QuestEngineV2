import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { z } from "zod";

import {
  createManualAdjustment,
  listManualAdjustments,
  revokeManualAdjustment,
  type ManualTimeAdjustment,
  type TimeAdjustmentType,
} from "@/api/adjustments";
import { getQuestProgress } from "@/api/gameplay";
import { ApiError } from "@/api/errors";
import { Button } from "@/components/ui/button";
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form";
import { Input } from "@/components/ui/input";
import { formatDateTime } from "@/lib/format";

const schema = z.object({
  type: z.enum(["BONUS", "PENALTY"]),
  seconds: z.coerce
    .number({ invalid_type_error: "Введите число секунд" })
    .int("Секунды — целое число")
    .positive("Количество секунд должно быть больше 0"),
  reason: z
    .string()
    .min(1, "Причина обязательна")
    .max(1000, "Причина не более 1000 символов"),
});

type FormValues = z.infer<typeof schema>;

function AdjustmentsList({
  adjustments,
  canRevoke,
  onRevoke,
  isRevoking,
}: {
  adjustments: ManualTimeAdjustment[];
  canRevoke: boolean;
  onRevoke: (id: number) => void;
  isRevoking: boolean;
}) {
  if (adjustments.length === 0) {
    return <p className="text-muted-foreground text-sm">Корректировок пока нет.</p>;
  }

  const sorted = [...adjustments].sort(
    (a, b) => Date.parse(b.createdAt) - Date.parse(a.createdAt),
  );

  return (
    <ul className="divide-y divide-border rounded-lg border border-border text-sm">
      {sorted.map((a) => {
        const revoked = a.revokedAt != null;
        return (
          <li
            key={a.id}
            className={`flex flex-wrap items-baseline gap-x-3 gap-y-1 px-3 py-2 ${
              revoked ? "opacity-60" : ""
            }`}
          >
            <span className={a.type === "BONUS" ? "text-success" : "text-warning"}>
              {a.type === "BONUS" ? "−" : "+"}
              {a.seconds}s {a.type}
            </span>
            <span className="min-w-0 flex-1 break-words">{a.reason}</span>
            <span className="text-muted-foreground shrink-0">
              {formatDateTime(a.createdAt)}
              {revoked ? " · отозвано" : ""}
            </span>
            {canRevoke && !revoked && (
              <Button
                type="button"
                size="sm"
                variant="ghost"
                disabled={isRevoking}
                onClick={() => onRevoke(a.id)}
              >
                Отозвать
              </Button>
            )}
          </li>
        );
      })}
    </ul>
  );
}

/**
 * Author/Admin: list + create + revoke for one team's QuestProgress.
 * Resolves progress id via GET /api/quests/progress/{questId}/{teamId}.
 */
export function ManualAdjustmentsPanel({
  questId,
  teamId,
  teamName,
  /** Revoke is blocked after quest is officially FINISHED (backend). */
  questFinished,
}: {
  questId: number;
  teamId: number;
  teamName: string;
  questFinished: boolean;
}) {
  const queryClient = useQueryClient();

  const progressQuery = useQuery({
    queryKey: ["gameplay", questId, teamId, "progress"],
    queryFn: () => getQuestProgress(questId, teamId),
  });

  const progressId = progressQuery.data?.id;

  const listQuery = useQuery({
    queryKey: ["adjustments", progressId],
    queryFn: () => listManualAdjustments(progressId!),
    enabled: progressId != null,
  });

  const form = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { type: "PENALTY", seconds: 60, reason: "" },
  });

  const createMutation = useMutation({
    mutationFn: (values: FormValues) =>
      createManualAdjustment(progressId!, {
        type: values.type as TimeAdjustmentType,
        seconds: values.seconds,
        reason: values.reason.trim(),
      }),
    onSuccess: () => {
      form.reset({ type: form.getValues("type"), seconds: 60, reason: "" });
      queryClient.invalidateQueries({ queryKey: ["adjustments", progressId] });
      queryClient.invalidateQueries({ queryKey: ["gameplay", questId, teamId] });
    },
    onError: (error) => {
      if (error instanceof ApiError) {
        const reasonErr = error.fieldError("reason");
        if (reasonErr) form.setError("reason", { message: reasonErr });
        const secErr = error.fieldError("seconds");
        if (secErr) form.setError("seconds", { message: secErr });
      }
    },
  });

  const revokeMutation = useMutation({
    mutationFn: revokeManualAdjustment,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["adjustments", progressId] });
      queryClient.invalidateQueries({ queryKey: ["gameplay", questId, teamId] });
    },
  });

  if (progressQuery.isLoading) {
    return <p className="text-muted-foreground text-sm">Загрузка прогресса...</p>;
  }

  if (progressQuery.isError || progressId == null) {
    const message =
      progressQuery.error instanceof ApiError
        ? progressQuery.error.message
        : "Нет прогресса команды (ещё не вошли в игру).";
    return (
      <div className="space-y-1">
        <h3 className="text-sm font-medium">{teamName}</h3>
        <p className="text-muted-foreground text-sm">{message}</p>
      </div>
    );
  }

  const generalCreateError =
    createMutation.error instanceof ApiError &&
    createMutation.error.fieldErrors.length === 0
      ? createMutation.error.message
      : null;
  const revokeError =
    revokeMutation.error instanceof ApiError ? revokeMutation.error.message : null;

  return (
    <div className="space-y-3 rounded-lg border border-border p-3">
      <div className="flex flex-wrap items-baseline justify-between gap-2">
        <h3 className="text-sm font-medium">{teamName}</h3>
        {progressQuery.data?.bonusPenaltySeconds != null && (
          <span className="text-muted-foreground text-xs">
            Σ BP: {progressQuery.data.bonusPenaltySeconds}s
          </span>
        )}
      </div>

      <AdjustmentsList
        adjustments={listQuery.data ?? []}
        canRevoke={!questFinished}
        onRevoke={(id) => revokeMutation.mutate(id)}
        isRevoking={revokeMutation.isPending}
      />
      {revokeError && <p className="text-destructive text-sm">{revokeError}</p>}

      {!questFinished && (
        <Form {...form}>
          <form
            onSubmit={form.handleSubmit((v) => createMutation.mutate(v))}
            className="space-y-2 border-t border-border pt-3"
            noValidate
          >
            <p className="text-muted-foreground text-xs">
              Новая корректировка (BONUS уменьшает итоговое время, PENALTY — увеличивает)
            </p>
            <div className="flex flex-wrap gap-2">
              <FormField
                control={form.control}
                name="type"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel className="sr-only">Тип</FormLabel>
                    <FormControl>
                      <select
                        className="border-input bg-background h-9 rounded-md border px-2 text-sm"
                        {...field}
                      >
                        <option value="BONUS">BONUS</option>
                        <option value="PENALTY">PENALTY</option>
                      </select>
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="seconds"
                render={({ field }) => (
                  <FormItem className="w-24">
                    <FormLabel className="sr-only">Секунды</FormLabel>
                    <FormControl>
                      <Input type="number" min={1} step={1} {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="reason"
                render={({ field }) => (
                  <FormItem className="min-w-[12rem] flex-1">
                    <FormLabel className="sr-only">Причина</FormLabel>
                    <FormControl>
                      <Input placeholder="Причина (обязательно)" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <Button type="submit" disabled={createMutation.isPending}>
                {createMutation.isPending ? "Сохраняем..." : "Начислить"}
              </Button>
            </div>
            {generalCreateError && (
              <p role="alert" className="text-destructive text-sm">
                {generalCreateError}
              </p>
            )}
          </form>
        </Form>
      )}

      {questFinished && (
        <p className="text-muted-foreground text-xs">
          Квест завершён — новые корректировки и отзыв недоступны.
        </p>
      )}
    </div>
  );
}

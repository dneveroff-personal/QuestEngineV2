import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { z } from "zod";

import { createHint, deleteHint, getHintsByLevel, type Hint } from "@/api/hints";
import { ApiError } from "@/api/errors";
import { Button } from "@/components/ui/button";
import { Form, FormControl, FormField, FormItem, FormMessage } from "@/components/ui/form";
import { Input } from "@/components/ui/input";

/**
 * Сверено с CreateHintRequest.java (0.6.11) и HintServiceImpl.validateHintData:
 * bonusPenaltySeconds обязателен для BONUS/PENALTY, недопустим для REGULAR —
 * та же симметричная модель, что и у CodeType в CodesPanel.tsx.
 */
const hintSchema = z
  .object({
    orderIndex: z.string().min(1, "Укажите порядковый номер"),
    delaySeconds: z.string().min(1, "Укажите задержку показа"),
    content: z.string().min(1, "Текст подсказки не может быть пустым"),
    type: z.enum(["REGULAR", "BONUS", "PENALTY"]),
    bonusPenaltySeconds: z.string(),
  })
  .refine((v) => v.type === "REGULAR" || v.bonusPenaltySeconds !== "", {
    message: "Обязательно для BONUS/PENALTY",
    path: ["bonusPenaltySeconds"],
  })
  .refine((v) => v.type !== "REGULAR" || v.bonusPenaltySeconds === "", {
    message: "Недопустимо для REGULAR",
    path: ["bonusPenaltySeconds"],
  });

type HintFormValues = z.infer<typeof hintSchema>;

const TYPE_LABEL: Record<string, string> = {
  REGULAR: "Обычная",
  BONUS: "Бонус",
  PENALTY: "Штраф",
};

export function HintsPanel({ questId, levelId }: { questId: number; levelId: number }) {
  const queryClient = useQueryClient();
  const { data: hints, isLoading } = useQuery({
    queryKey: ["levels", levelId, "hints"],
    queryFn: () => getHintsByLevel(questId, levelId),
  });

  const form = useForm<HintFormValues>({
    resolver: zodResolver(hintSchema),
    defaultValues: {
      orderIndex: String((hints?.length ?? 0) + 1),
      delaySeconds: "",
      content: "",
      type: "REGULAR",
      bonusPenaltySeconds: "",
    },
  });

  const createMutation = useMutation({
    mutationFn: (values: HintFormValues) =>
      createHint(questId, levelId, {
        orderIndex: Number(values.orderIndex),
        delaySeconds: Number(values.delaySeconds),
        content: values.content,
        type: values.type,
        bonusPenaltySeconds: values.bonusPenaltySeconds ? Number(values.bonusPenaltySeconds) : null,
      }),
    onSuccess: () => {
      form.reset({
        orderIndex: String((hints?.length ?? 0) + 2),
        delaySeconds: "",
        content: "",
        type: "REGULAR",
        bonusPenaltySeconds: "",
      });
      queryClient.invalidateQueries({ queryKey: ["levels", levelId, "hints"] });
    },
  });

  const deleteMutation = useMutation({
    mutationFn: deleteHint,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["levels", levelId, "hints"] });
    },
  });

  const error = createMutation.error ?? deleteMutation.error;
  const errorMessage = error instanceof ApiError ? error.message : null;

  const watchType = form.watch("type");

  return (
    <div className="space-y-2">
      <h4 className="text-muted-foreground text-xs font-medium uppercase tracking-wide">
        Подсказки (auto-reveal)
      </h4>

      {isLoading && <p className="text-muted-foreground text-xs">Загрузка...</p>}
      {errorMessage && <p className="text-destructive text-xs">{errorMessage}</p>}

      {hints && hints.length > 0 && (
        <ul className="space-y-1">
          {hints
            .slice()
            .sort((a, b) => a.orderIndex - b.orderIndex)
            .map((hint: Hint) => (
              <li key={hint.id} className="flex items-start justify-between gap-2 text-xs">
                <span className="flex-1">
                  <span className="text-muted-foreground">
                    #{hint.orderIndex}, +{hint.delaySeconds}с, {TYPE_LABEL[hint.type]}
                    {hint.bonusPenaltySeconds ? ` (${hint.bonusPenaltySeconds}с)` : ""}:
                  </span>{" "}
                  {hint.content}
                </span>
                <Button
                  size="sm"
                  variant="ghost"
                  className="h-6 px-2"
                  onClick={() => deleteMutation.mutate(hint.id)}
                  disabled={deleteMutation.isPending}
                >
                  Удалить
                </Button>
              </li>
            ))}
        </ul>
      )}

      <Form {...form}>
        <form
          onSubmit={form.handleSubmit((values) => createMutation.mutate(values))}
          className="flex flex-wrap items-start gap-2"
          noValidate
        >
          <FormField
            control={form.control}
            name="orderIndex"
            render={({ field }) => (
              <FormItem>
                <FormControl>
                  <Input type="number" min={0} placeholder="№" className="w-16" {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />
          <FormField
            control={form.control}
            name="delaySeconds"
            render={({ field }) => (
              <FormItem>
                <FormControl>
                  <Input type="number" min={0} placeholder="задержка, с" className="w-28" {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />
          <FormField
            control={form.control}
            name="content"
            render={({ field }) => (
              <FormItem className="min-w-40 flex-1">
                <FormControl>
                  <Input placeholder="Текст подсказки" {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />
          <FormField
            control={form.control}
            name="type"
            render={({ field }) => (
              <FormItem>
                <FormControl>
                  <select
                    {...field}
                    className="border-input flex h-8 rounded-lg border bg-transparent px-2 text-sm shadow-xs outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50"
                  >
                    <option value="REGULAR">Обычная</option>
                    <option value="BONUS">Бонус</option>
                    <option value="PENALTY">Штраф</option>
                  </select>
                </FormControl>
              </FormItem>
            )}
          />
          {watchType !== "REGULAR" && (
            <FormField
              control={form.control}
              name="bonusPenaltySeconds"
              render={({ field }) => (
                <FormItem>
                  <FormControl>
                    <Input
                      type="number"
                      placeholder={watchType === "BONUS" ? "бонус, с" : "штраф, с"}
                      className="w-24"
                      {...field}
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
          )}
          <Button type="submit" size="sm" disabled={createMutation.isPending}>
            Добавить
          </Button>
        </form>
      </Form>
    </div>
  );
}

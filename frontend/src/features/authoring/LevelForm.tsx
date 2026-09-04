import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { z } from "zod";

import { ApiError } from "@/api/errors";
import type { CreateLevelRequest, Level } from "@/api/levels";
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

/**
 * Сверено с CreateLevelRequest.java. ВАЖНО (ADR-0005, "аномальный
 * уровень"): backend запрещает публикацию квеста, если у уровня нет ни
 * timeoutSeconds (автопереход), ни кодов — но CreateLevelRequest этого
 * не проверяет на создании уровня, только позже на publishQuest. Клиент
 * это не дублирует — тот же принцип, что и с датами в QuestForm.
 */
const levelSchema = z.object({
  title: z.string().min(1, "Название не может быть пустым").max(255),
  content: z.string().max(10000, "Содержимое должно быть не более 10000 символов"),
  timeoutSeconds: z.string(),
  requiredMainCodesCount: z.string(),
});

type LevelFormValues = z.infer<typeof levelSchema>;

interface LevelFormProps {
  level?: Level;
  onSubmit: (request: CreateLevelRequest) => void;
  isPending: boolean;
  error: unknown;
  submitLabel: string;
  onCancel?: () => void;
}

export function LevelForm({ level, onSubmit, isPending, error, submitLabel, onCancel }: LevelFormProps) {
  const form = useForm<LevelFormValues>({
    resolver: zodResolver(levelSchema),
    defaultValues: {
      title: level?.title ?? "",
      content: level?.content ?? "",
      timeoutSeconds: level?.timeoutSeconds ? String(level.timeoutSeconds) : "",
      requiredMainCodesCount: level?.requiredMainCodesCount
        ? String(level.requiredMainCodesCount)
        : "",
    },
  });

  function handleSubmit(values: LevelFormValues) {
    onSubmit({
      title: values.title,
      content: values.content || undefined,
      timeoutSeconds: values.timeoutSeconds ? Number(values.timeoutSeconds) : undefined,
      requiredMainCodesCount: values.requiredMainCodesCount
        ? Number(values.requiredMainCodesCount)
        : undefined,
    });
  }

  const generalError = error instanceof ApiError ? error.message : null;

  return (
    <Form {...form}>
      <form onSubmit={form.handleSubmit(handleSubmit)} className="space-y-3" noValidate>
        <FormField
          control={form.control}
          name="title"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Название уровня</FormLabel>
              <FormControl>
                <Input autoFocus {...field} />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />

        <FormField
          control={form.control}
          name="content"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Содержимое (текст/легенда уровня)</FormLabel>
              <FormControl>
                <textarea
                  {...field}
                  rows={4}
                  className="border-input flex w-full rounded-lg border bg-transparent px-2.5 py-1.5 text-sm shadow-xs outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50"
                />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />

        <div className="grid grid-cols-2 gap-3">
          <FormField
            control={form.control}
            name="timeoutSeconds"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Автопереход (сек.)</FormLabel>
                <FormControl>
                  <Input type="number" min={1} placeholder="необязательно" {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />
          <FormField
            control={form.control}
            name="requiredMainCodesCount"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Нужно MAIN-кодов</FormLabel>
                <FormControl>
                  <Input type="number" min={1} placeholder="необязательно" {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />
        </div>

        {generalError && <p className="text-destructive text-sm">{generalError}</p>}

        <div className="flex gap-2">
          <Button type="submit" size="sm" disabled={isPending}>
            {isPending ? "Сохраняем..." : submitLabel}
          </Button>
          {onCancel && (
            <Button type="button" size="sm" variant="ghost" onClick={onCancel}>
              Отмена
            </Button>
          )}
        </div>
      </form>
    </Form>
  );
}

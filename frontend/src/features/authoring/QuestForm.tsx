import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { z } from "zod";

import { ApiError } from "@/api/errors";
import type { CreateQuestRequest, Quest } from "@/api/quests";
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

/** Сверено с CreateQuestRequest.java: title 1–255, description max 5000. */
const questSchema = z
  .object({
    title: z.string().min(1, "Название не может быть пустым").max(255),
    description: z.string().max(5000, "Описание должно быть не более 5000 символов"),
    type: z.enum(["SINGLE", "TEAM"]),
    // datetime-local input отдаёт "" для пустого поля, а не undefined —
    // приводим к null явно перед отправкой (см. onSubmit).
    startTime: z.string(),
    finishTime: z.string(),
  })
  .refine(
    (values) => !values.startTime || !values.finishTime || values.finishTime > values.startTime,
    {
      // Backend это не проверяет (QuestServiceImpl.buildQuest не валидирует
      // порядок дат) — чисто клиентская подсказка, не гарантия.
      message: "Дата завершения должна быть позже даты начала",
      path: ["finishTime"],
    },
  );

type QuestFormValues = z.infer<typeof questSchema>;

function toDatetimeLocal(iso?: string | null): string {
  if (!iso) return "";
  // datetime-local ожидает "YYYY-MM-DDTHH:mm" в локальном времени, без секунд/зоны.
  const date = new Date(iso);
  const offset = date.getTimezoneOffset();
  return new Date(date.getTime() - offset * 60000).toISOString().slice(0, 16);
}

interface QuestFormProps {
  quest?: Quest;
  onSubmit: (request: CreateQuestRequest) => void;
  isPending: boolean;
  error: unknown;
  submitLabel: string;
}

export function QuestForm({ quest, onSubmit, isPending, error, submitLabel }: QuestFormProps) {
  const form = useForm<QuestFormValues>({
    resolver: zodResolver(questSchema),
    defaultValues: {
      title: quest?.title ?? "",
      description: quest?.description ?? "",
      type: quest?.type ?? "TEAM",
      startTime: toDatetimeLocal(quest?.startTime),
      finishTime: toDatetimeLocal(quest?.finishTime),
    },
  });

  function handleSubmit(values: QuestFormValues) {
    onSubmit({
      title: values.title,
      description: values.description,
      type: values.type,
      startTime: values.startTime ? new Date(values.startTime).toISOString() : null,
      finishTime: values.finishTime ? new Date(values.finishTime).toISOString() : null,
    });
  }

  const generalError = error instanceof ApiError ? error.message : null;

  return (
    <Form {...form}>
      <form onSubmit={form.handleSubmit(handleSubmit)} className="space-y-4" noValidate>
        <FormField
          control={form.control}
          name="title"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Название</FormLabel>
              <FormControl>
                <Input autoFocus {...field} />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />

        <FormField
          control={form.control}
          name="description"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Описание</FormLabel>
              <FormControl>
                <textarea
                  {...field}
                  rows={6}
                  className="border-input flex w-full rounded-lg border bg-transparent px-2.5 py-1.5 text-sm shadow-xs outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50"
                />
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
              <FormLabel>Тип</FormLabel>
              <FormControl>
                <select
                  {...field}
                  className="border-input flex h-8 w-full rounded-lg border bg-transparent px-2.5 text-sm shadow-xs outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50"
                >
                  <option value="TEAM">Командный</option>
                  <option value="SINGLE">Одиночный</option>
                </select>
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />

        <div className="grid grid-cols-2 gap-3">
          <FormField
            control={form.control}
            name="startTime"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Начало</FormLabel>
                <FormControl>
                  <Input type="datetime-local" {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />

          <FormField
            control={form.control}
            name="finishTime"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Завершение</FormLabel>
                <FormControl>
                  <Input type="datetime-local" {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />
        </div>

        {generalError && <p className="text-destructive text-sm">{generalError}</p>}

        <Button type="submit" disabled={isPending}>
          {isPending ? "Сохраняем..." : submitLabel}
        </Button>
      </form>
    </Form>
  );
}

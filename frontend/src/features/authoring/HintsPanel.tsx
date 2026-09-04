import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { z } from "zod";

import { createHint, deleteHint, getHintsByLevel } from "@/api/hints";
import { ApiError } from "@/api/errors";
import { Button } from "@/components/ui/button";
import { Form, FormControl, FormField, FormItem, FormMessage } from "@/components/ui/form";
import { Input } from "@/components/ui/input";

/** CreateHintRequest.java: @NotBlank content. С 0.6.6 явный @Size(max=2048) убран — не дублируем произвольный лимит, которого нет на backend. */
const hintSchema = z.object({
  content: z.string().min(1, "Текст подсказки не может быть пустым"),
});
type HintFormValues = z.infer<typeof hintSchema>;

export function HintsPanel({ questId, levelId }: { questId: number; levelId: number }) {
  const queryClient = useQueryClient();
  const { data: hints, isLoading } = useQuery({
    queryKey: ["levels", levelId, "hints"],
    queryFn: () => getHintsByLevel(questId, levelId),
  });

  const form = useForm<HintFormValues>({
    resolver: zodResolver(hintSchema),
    defaultValues: { content: "" },
  });

  const createMutation = useMutation({
    mutationFn: (values: HintFormValues) => createHint(questId, levelId, values),
    onSuccess: () => {
      form.reset();
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

  return (
    <div className="space-y-2">
      <h4 className="text-xs font-medium uppercase tracking-wide text-muted-foreground">
        Подсказки
      </h4>

      {isLoading && <p className="text-muted-foreground text-xs">Загрузка...</p>}
      {errorMessage && <p className="text-destructive text-xs">{errorMessage}</p>}

      {hints && hints.length > 0 && (
        <ul className="space-y-1">
          {hints.map((hint) => (
            <li key={hint.id} className="flex items-start justify-between gap-2 text-xs">
              <span className="flex-1">{hint.content}</span>
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
          className="flex gap-2"
          noValidate
        >
          <FormField
            control={form.control}
            name="content"
            render={({ field }) => (
              <FormItem className="flex-1">
                <FormControl>
                  <Input placeholder="Текст новой подсказки" {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />
          <Button type="submit" size="sm" disabled={createMutation.isPending}>
            Добавить
          </Button>
        </form>
      </Form>
    </div>
  );
}

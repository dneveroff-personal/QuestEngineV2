import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { z } from "zod";

import { createCode, deleteCode, getCodesByLevel } from "@/api/codes";
import { ApiError } from "@/api/errors";
import { Button } from "@/components/ui/button";
import { Form, FormControl, FormField, FormItem, FormMessage } from "@/components/ui/form";
import { Input } from "@/components/ui/input";

/** Сверено с CreateCodeRequest.java. */
const codeSchema = z.object({
  value: z.string().min(1, "Значение не может быть пустым").max(255),
  type: z.enum(["MAIN", "BONUS", "PENALTY"]),
  points: z.string(),
});
type CodeFormValues = z.infer<typeof codeSchema>;

const TYPE_LABEL: Record<string, string> = {
  MAIN: "Основной",
  BONUS: "Бонус",
  PENALTY: "Штраф",
};

/**
 * ИЗВЕСТНАЯ ГОНКА (roadmap.md §3): backend — проблема глобальной
 * уникальности значения кода (code-submission.md). 409 здесь означает
 * "такое значение уже используется в другом уровне", а не поломку.
 */
export function CodesPanel({ questId, levelId }: { questId: number; levelId: number }) {
  const queryClient = useQueryClient();
  const { data: codes, isLoading } = useQuery({
    queryKey: ["levels", levelId, "codes"],
    queryFn: () => getCodesByLevel(questId, levelId),
  });

  const form = useForm<CodeFormValues>({
    resolver: zodResolver(codeSchema),
    defaultValues: { value: "", type: "MAIN", points: "" },
  });

  const createMutation = useMutation({
    mutationFn: (values: CodeFormValues) =>
      createCode(questId, levelId, {
        value: values.value,
        type: values.type,
        points: values.points ? Number(values.points) : undefined,
      }),
    onSuccess: () => {
      form.reset();
      queryClient.invalidateQueries({ queryKey: ["levels", levelId, "codes"] });
    },
  });

  const deleteMutation = useMutation({
    mutationFn: deleteCode,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["levels", levelId, "codes"] });
    },
  });

  const error = createMutation.error ?? deleteMutation.error;
  const errorMessage =
    error instanceof ApiError
      ? error.message
      : error
        ? "Не удалось выполнить действие."
        : null;

  return (
    <div className="space-y-2">
      <h4 className="text-xs font-medium uppercase tracking-wide text-muted-foreground">Коды</h4>

      {isLoading && <p className="text-muted-foreground text-xs">Загрузка...</p>}
      {errorMessage && <p className="text-destructive text-xs">{errorMessage}</p>}

      {codes && codes.length > 0 && (
        <ul className="space-y-1">
          {codes.map((code) => (
            <li key={code.id} className="flex items-center justify-between gap-2 text-xs">
              <span>
                <span className="font-mono">{code.value}</span>{" "}
                <span className="text-muted-foreground">
                  ({TYPE_LABEL[code.type] ?? code.type}
                  {code.points ? `, ${code.points} очк.` : ""})
                </span>
              </span>
              <Button
                size="sm"
                variant="ghost"
                className="h-6 px-2"
                onClick={() => deleteMutation.mutate(code.id)}
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
          className="flex flex-wrap gap-2"
          noValidate
        >
          <FormField
            control={form.control}
            name="value"
            render={({ field }) => (
              <FormItem className="flex-1">
                <FormControl>
                  <Input placeholder="Значение кода" {...field} />
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
                    <option value="MAIN">Основной</option>
                    <option value="BONUS">Бонус</option>
                    <option value="PENALTY">Штраф</option>
                  </select>
                </FormControl>
              </FormItem>
            )}
          />
          <FormField
            control={form.control}
            name="points"
            render={({ field }) => (
              <FormItem>
                <FormControl>
                  <Input type="number" placeholder="очки" className="w-20" {...field} />
                </FormControl>
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

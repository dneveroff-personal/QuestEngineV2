import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { z } from "zod";

import { createTeam } from "@/api/teams";
import { ApiError, NetworkError } from "@/api/errors";
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

/** Сверено с CreateTeamRequest.java: @Size(min = 1, max = 255). */
const createTeamSchema = z.object({
  name: z.string().min(1, "Название команды не может быть пустым").max(255),
});

type CreateTeamFormValues = z.infer<typeof createTeamSchema>;

export function CreateTeamForm() {
  const queryClient = useQueryClient();
  const form = useForm<CreateTeamFormValues>({
    resolver: zodResolver(createTeamSchema),
    defaultValues: { name: "" },
  });

  const mutation = useMutation({
    mutationFn: createTeam,
    onSuccess: () => {
      // Инвалидируем, а не пишем в кэш вручную — простое и надёжное решение
      // для одноразового действия без гонок (architecture.md §11).
      queryClient.invalidateQueries({ queryKey: ["teams", "my"] });
    },
    onError: (error) => {
      if (error instanceof ApiError) {
        const fieldError = error.fieldError("name");
        if (fieldError) form.setError("name", { message: fieldError });
      }
    },
  });

  const generalError =
    mutation.error instanceof NetworkError
      ? mutation.error.message
      : mutation.error instanceof ApiError && mutation.error.fieldErrors.length === 0
        ? mutation.error.message
        : null;

  return (
    <Form {...form}>
      <form
        onSubmit={form.handleSubmit((values) => mutation.mutate(values))}
        className="space-y-4"
        noValidate
      >
        <FormField
          control={form.control}
          name="name"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Название команды</FormLabel>
              <FormControl>
                <Input autoFocus {...field} />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />

        {generalError && (
          <p role="alert" className="text-destructive text-sm">
            {generalError}
          </p>
        )}

        <Button type="submit" disabled={mutation.isPending}>
          {mutation.isPending ? "Создаём..." : "Создать команду"}
        </Button>
      </form>
    </Form>
  );
}

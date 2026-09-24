import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { useNavigate } from "react-router-dom";
import { z } from "zod";

import { leaveTeam, renameTeam, sendJoinRequest, type Team } from "@/api/teams";
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
import { useAuth } from "@/features/auth";
import { isCaptainOf } from "@/features/teams/utils";

/** Same validation as CreateTeamRequest / CreateTeamForm. */
const renameSchema = z.object({
  name: z.string().min(1, "Название команды не может быть пустым").max(255),
});
type RenameFormValues = z.infer<typeof renameSchema>;

const inviteSchema = z.object({
  username: z.string().min(1, "Введите имя пользователя (username, не публичное имя)"),
});
type InviteFormValues = z.infer<typeof inviteSchema>;

export function TeamManagementPanel({ team }: { team: Team }) {
  const { username } = useAuth();
  const isCaptain = isCaptainOf(team, username);

  if (!isCaptain) {
    return <LeaveTeamPanel />;
  }

  return (
    <div className="space-y-6">
      <RenameTeamPanel team={team} />
      <InvitePanel teamId={team.id} />
    </div>
  );
}

function RenameTeamPanel({ team }: { team: Team }) {
  const queryClient = useQueryClient();
  const form = useForm<RenameFormValues>({
    resolver: zodResolver(renameSchema),
    defaultValues: { name: team.name },
  });

  // Keep input in sync after successful rename (invalidate → new team prop).
  useEffect(() => {
    form.reset({ name: team.name });
  }, [team.name, form]);

  const mutation = useMutation({
    mutationFn: (values: RenameFormValues) => renameTeam(team.id, values),
    onSuccess: () => {
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
    mutation.error instanceof ApiError && mutation.error.fieldErrors.length === 0
      ? mutation.error.message
      : null;

  return (
    <div className="space-y-2">
      <h2 className="text-sm font-medium">Переименовать команду</h2>
      <Form {...form}>
        <form
          onSubmit={form.handleSubmit((values) => mutation.mutate(values))}
          className="flex flex-col gap-2 sm:flex-row sm:items-start"
          noValidate
        >
          <FormField
            control={form.control}
            name="name"
            render={({ field }) => (
              <FormItem className="flex-1">
                <FormLabel className="sr-only">Новое название</FormLabel>
                <FormControl>
                  <Input placeholder="Новое название команды" {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />
          <Button type="submit" disabled={mutation.isPending}>
            {mutation.isPending ? "Сохраняем..." : "Сохранить"}
          </Button>
        </form>
      </Form>
      {generalError && (
        <p role="alert" className="text-destructive text-sm">
          {generalError}
        </p>
      )}
      {mutation.isSuccess && (
        <p className="text-muted-foreground text-sm">Название обновлено.</p>
      )}
    </div>
  );
}

function InvitePanel({ teamId }: { teamId: number }) {
  const queryClient = useQueryClient();
  const form = useForm<InviteFormValues>({
    resolver: zodResolver(inviteSchema),
    defaultValues: { username: "" },
  });

  const mutation = useMutation({
    mutationFn: (values: InviteFormValues) => sendJoinRequest(teamId, values.username),
    onSuccess: () => {
      form.reset();
      queryClient.invalidateQueries({ queryKey: ["teams", "requests"] });
    },
  });

  const errorMessage = mutation.error instanceof ApiError ? mutation.error.message : null;

  return (
    <div className="space-y-2">
      <h2 className="text-sm font-medium">Пригласить игрока</h2>
      <Form {...form}>
        <form
          onSubmit={form.handleSubmit((values) => mutation.mutate(values))}
          className="flex gap-2"
          noValidate
        >
          <FormField
            control={form.control}
            name="username"
            render={({ field }) => (
              <FormItem className="flex-1">
                <FormControl>
                  <Input placeholder="username приглашаемого" {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />
          <Button type="submit" disabled={mutation.isPending}>
            Пригласить
          </Button>
        </form>
      </Form>
      {errorMessage && <p className="text-destructive text-sm">{errorMessage}</p>}
      {mutation.isSuccess && (
        <p className="text-muted-foreground text-sm">Приглашение отправлено.</p>
      )}
    </div>
  );
}

/** Backend запрещает капитану покинуть команду (validateCaptain в leaveTeam) — поэтому кнопка только не-капитанам. */
function LeaveTeamPanel() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const mutation = useMutation({
    mutationFn: leaveTeam,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["teams", "my"] });
      navigate("/team", { replace: true });
    },
  });

  const errorMessage = mutation.error instanceof ApiError ? mutation.error.message : null;

  return (
    <div className="space-y-2">
      {errorMessage && <p className="text-destructive text-sm">{errorMessage}</p>}
      <Button
        variant="outline"
        onClick={() => mutation.mutate()}
        disabled={mutation.isPending}
      >
        {mutation.isPending ? "Выходим..." : "Покинуть команду"}
      </Button>
    </div>
  );
}

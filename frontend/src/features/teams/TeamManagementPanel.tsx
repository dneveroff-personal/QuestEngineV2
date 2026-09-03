import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { useNavigate } from "react-router-dom";
import { z } from "zod";

import { leaveTeam, sendJoinRequest, type Team } from "@/api/teams";
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

const inviteSchema = z.object({
  username: z.string().min(1, "Введите имя пользователя (username, не публичное имя)"),
});
type InviteFormValues = z.infer<typeof inviteSchema>;

export function TeamManagementPanel({ team }: { team: Team }) {
  const { username } = useAuth();
  const isCaptain = isCaptainOf(team, username);

  return isCaptain ? <InvitePanel teamId={team.id} /> : <LeaveTeamPanel />;
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

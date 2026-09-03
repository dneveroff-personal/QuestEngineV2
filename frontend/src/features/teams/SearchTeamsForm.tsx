import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useState, type FormEvent } from "react";

import { searchTeams, sendJoinRequest, type Team } from "@/api/teams";
import { ApiError } from "@/api/errors";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";

export function SearchTeamsForm() {
  const [query, setQuery] = useState("");
  const [results, setResults] = useState<Team[] | null>(null);
  const queryClient = useQueryClient();

  const searchMutation = useMutation({
    mutationFn: searchTeams,
    onSuccess: setResults,
  });

  const joinMutation = useMutation({
    mutationFn: (teamId: number) => sendJoinRequest(teamId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["teams", "requests"] });
    },
  });

  function handleSearch(event: FormEvent) {
    event.preventDefault();
    if (query.trim()) searchMutation.mutate(query.trim());
  }

  const searchError =
    searchMutation.error instanceof ApiError
      ? searchMutation.error.message
      : searchMutation.error
        ? "Не удалось выполнить поиск."
        : null;

  const joinError =
    joinMutation.error instanceof ApiError
      ? joinMutation.error.message
      : joinMutation.error
        ? "Не удалось отправить заявку."
        : null;

  return (
    <div className="space-y-3">
      <form onSubmit={handleSearch} className="flex gap-2">
        <Input
          placeholder="Название команды"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
        />
        <Button type="submit" disabled={searchMutation.isPending || !query.trim()}>
          Найти
        </Button>
      </form>

      {searchError && <p className="text-destructive text-sm">{searchError}</p>}
      {joinError && <p className="text-destructive text-sm">{joinError}</p>}

      {results && results.length === 0 && (
        <p className="text-muted-foreground text-sm">Команды не найдены.</p>
      )}

      {results && results.length > 0 && (
        <ul className="divide-y divide-border rounded-lg border border-border">
          {results.map((team) => (
            <li key={team.id} className="flex items-center justify-between px-4 py-2 text-sm">
              <div>
                <p className="font-medium">{team.name}</p>
                <p className="text-muted-foreground text-xs">Капитан: {team.captainName}</p>
              </div>
              {joinMutation.isSuccess && joinMutation.variables === team.id ? (
                <span className="text-muted-foreground text-xs">Заявка отправлена</span>
              ) : (
                <Button
                  size="sm"
                  variant="outline"
                  onClick={() => joinMutation.mutate(team.id)}
                  disabled={joinMutation.isPending}
                >
                  Отправить заявку
                </Button>
              )}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useState, type FormEvent } from "react";

import { searchTeams, sendJoinRequest, type Team } from "@/api/teams";
import { ApiError } from "@/api/errors";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";

export function SearchTeamsForm() {
  const queryClient = useQueryClient();
  const [query, setQuery] = useState("");
  const [results, setResults] = useState<Team[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  const searchMutation = useMutation({
    mutationFn: (name: string) => searchTeams(name),
    onSuccess: (teams) => {
      setResults(teams);
      setError(null);
    },
    onError: (err) => {
      setResults(null);
      setError(err instanceof ApiError ? err.detail || err.title : "Ошибка поиска");
    },
  });

  const joinMutation = useMutation({
    mutationFn: (teamId: number) => sendJoinRequest(teamId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["teams", "join-requests"] });
    },
  });

  const onSubmit = (e: FormEvent) => {
    e.preventDefault();
    if (!query.trim()) return;
    searchMutation.mutate(query.trim());
  };

  return (
    <div className="space-y-4">
      <form onSubmit={onSubmit} className="flex gap-2">
        <Input
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="Название команды"
        />
        <Button type="submit" disabled={searchMutation.isPending}>
          Найти
        </Button>
      </form>
      {error && <p className="text-sm text-destructive">{error}</p>}
      {results && results.length === 0 && (
        <p className="text-sm text-muted-foreground">Ничего не найдено</p>
      )}
      {results && results.length > 0 && (
        <ul className="divide-y divide-border rounded-lg border border-border">
          {results.map((team) => (
            <li key={team.id} className="flex items-center justify-between px-4 py-3 text-sm">
              <div>
                <p className="font-medium">{team.name}</p>
                <p className="text-muted-foreground text-xs">
                  Капитан: {team.captainDisplayName}
                </p>
              </div>
              <Button
                size="sm"
                variant="outline"
                disabled={joinMutation.isPending}
                onClick={() => joinMutation.mutate(team.id)}
              >
                Вступить
              </Button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

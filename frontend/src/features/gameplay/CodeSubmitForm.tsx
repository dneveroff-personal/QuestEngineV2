import { useState, type FormEvent } from "react";

import { ApiError } from "@/api/errors";
import type { CodeSubmissionResponse } from "@/api/gameplay";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { useSubmitCode } from "@/features/gameplay/useGameplay";

const RESULT_LABEL: Record<string, { text: string; className: string }> = {
  CORRECT_MAIN: { text: "Верно!", className: "text-success" },
  CORRECT_BONUS: { text: "Бонусный код!", className: "text-success" },
  CORRECT_PENALTY: { text: "Штрафной код принят.", className: "text-warning" },
  INCORRECT: { text: "Неверный код.", className: "text-destructive" },
};

export function CodeSubmitForm({ questId, teamId }: { questId: number; teamId: number }) {
  const [value, setValue] = useState("");
  const [lastResult, setLastResult] = useState<CodeSubmissionResponse | null>(null);
  const mutation = useSubmitCode(questId, teamId);

  function handleSubmit(event: FormEvent) {
    event.preventDefault();
    if (!value.trim()) return;
    mutation.mutate(value.trim(), {
      onSuccess: (response) => {
        setLastResult(response);
        setValue("");
      },
    });
  }

  const errorMessage = mutation.error instanceof ApiError ? mutation.error.message : null;
  const resultInfo = lastResult ? RESULT_LABEL[lastResult.result] : null;

  return (
    <div className="space-y-3 rounded-lg border border-border p-4">
      <h2 className="text-sm font-medium">Ввести код</h2>

      <form onSubmit={handleSubmit} className="flex gap-2">
        <Input
          value={value}
          onChange={(e) => setValue(e.target.value)}
          placeholder="Код"
          autoFocus
          autoComplete="off"
        />
        <Button type="submit" disabled={mutation.isPending || !value.trim()}>
          {mutation.isPending ? "Проверяем..." : "Отправить"}
        </Button>
      </form>

      {errorMessage && <p className="text-destructive text-sm">{errorMessage}</p>}

      {resultInfo && (
        <div className="text-sm">
          <p className={resultInfo.className}>{resultInfo.text}</p>
          {lastResult?.remainingMainCodes != null && (
            <p className="text-muted-foreground">
              Осталось основных кодов: {lastResult.remainingMainCodes}
            </p>
          )}
          {lastResult?.levelCompleted && (
            <p className="text-success font-medium">
              {lastResult.questFinished ? "Квест завершён! 🎉" : "Уровень пройден, переходим дальше."}
            </p>
          )}
        </div>
      )}
    </div>
  );
}

import { Link } from "react-router-dom";

import { formatDateTime } from "@/lib/format";
import type { QuestShort } from "@/api/quests";

export function QuestCard({ quest }: { quest: QuestShort }) {
  return (
    <Link
      to={`/quests/${quest.id}`}
      className="block rounded-lg border border-border p-4 transition-colors hover:bg-accent"
    >
      <h3 className="font-medium">{quest.title}</h3>
      <p className="text-muted-foreground text-sm">{formatDateTime(quest.startTime)}</p>
    </Link>
  );
}

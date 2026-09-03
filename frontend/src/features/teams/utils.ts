import type { Team } from "@/api/teams";

/** См. комментарий в TeamMembersList.tsx — сравнение по username (из JWT), не publicName. */
export function isCaptainOf(team: Team, username: string | null): boolean {
  if (!username) return false;
  return team.members.some((m) => m.name === username && m.role === "CAPTAIN");
}

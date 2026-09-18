import type { Team } from "@/api/teams";

/** Captain check by stable username (JWT sub), not displayName. */
export function isCaptainOf(team: Team, username: string | null): boolean {
  if (!username) return false;
  return team.members.some((m) => m.username === username && m.role === "CAPTAIN");
}

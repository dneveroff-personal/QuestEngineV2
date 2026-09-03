import { useQuery } from "@tanstack/react-query";

import { getJoinRequests } from "@/api/teams";

export function useJoinRequests() {
  return useQuery({
    queryKey: ["teams", "requests"],
    queryFn: getJoinRequests,
  });
}

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import {
  createQuest,
  deleteQuest,
  finishQuest,
  getQuestsByAuthor,
  publishQuest,
  updateQuest,
  type CreateQuestRequest,
} from "@/api/quests";
import { useMyAuthorId } from "@/features/authoring/useMyAuthorId";

export function useMyAuthoredQuests() {
  const { data: authorId, isLoading: isAuthorIdLoading } = useMyAuthorId();

  const query = useQuery({
    queryKey: ["quests", "by-author", authorId],
    queryFn: () => getQuestsByAuthor(authorId as number),
    enabled: authorId != null,
  });

  return { ...query, isLoading: isAuthorIdLoading || query.isLoading, authorId };
}

export function useCreateQuest() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: createQuest,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["quests", "by-author"] });
    },
  });
}

export function useUpdateQuest(questId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (request: CreateQuestRequest) => updateQuest(questId, request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["quests", questId] });
      queryClient.invalidateQueries({ queryKey: ["quests", "by-author"] });
    },
  });
}

export function useQuestLifecycleActions(questId: number) {
  const queryClient = useQueryClient();
  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ["quests", questId] });
    queryClient.invalidateQueries({ queryKey: ["quests", "by-author"] });
  };

  const publish = useMutation({ mutationFn: () => publishQuest(questId), onSuccess: invalidate });
  const finish = useMutation({ mutationFn: () => finishQuest(questId), onSuccess: invalidate });
  const remove = useMutation({ mutationFn: () => deleteQuest(questId) });

  return { publish, finish, remove };
}

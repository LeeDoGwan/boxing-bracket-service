import { getApi, postApi } from './client';

export function getJudgeScores(boutId, judgeId, token) {
  return getApi(`/api/judge/bouts/${boutId}/scores`, { judgeId }, { token });
}

export function submitRoundScore(boutId, roundNo, score, token) {
  return postApi(`/api/judge/bouts/${boutId}/rounds/${roundNo}/scores`, score, { token });
}

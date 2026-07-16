import { getApi, postApi } from './client';

export function getJudgeScores(boutId, tokenOrJudgeId, maybeToken) {
  return getApi(`/api/judge/bouts/${boutId}/scores`, undefined, { token: maybeToken || tokenOrJudgeId });
}

export function submitRoundScore(boutId, roundNo, score, token) {
  return postApi(`/api/judge/bouts/${boutId}/rounds/${roundNo}/scores`, score, { token });
}

import { translate as t } from './i18n';

const statusKeys = {
  CANCELED: 'common:status.canceled',
  CLOSED: 'common:finished',
  SCORING: 'common:scoring',
  SCHEDULED: 'common:scheduled',
  READY: 'common:ready',
  IN_PROGRESS: 'common:inProgress',
  FINISHED: 'common:finished',
};

export function statusLabel(status) {
  return statusKeys[status] ? t(statusKeys[status]) : status || t('common:unavailable');
}

export function decisionLabel(decisionType) {
  if (decisionType === 'KO') return 'KO';
  if (decisionType === 'RSC') return 'TKO';
  const keys = { POINTS: 'common:pointsDecision', ABD: 'common:abandoned', DSQ: 'common:disqualified', WALKOVER: 'common:walkover' };
  return keys[decisionType] ? t(keys[decisionType]) : decisionType || t('common:pointsDecision');
}

export function streamStateLabel(state) {
  const keys = { connected: 'common:connected', connecting: 'common:connecting', reconnecting: 'common:reconnecting', offline: 'common:disconnected' };
  return t(keys[state] || 'common:disconnected');
}

export function effectiveResultScores(result) {
  if (!result) {
    return null;
  }
  return {
    red: result.redEffectiveScore ?? ((result.redTotalScore ?? 0) + (result.bluePenaltyTotal ?? 0)),
    blue: result.blueEffectiveScore ?? ((result.blueTotalScore ?? 0) + (result.redPenaltyTotal ?? 0)),
  };
}

export function athleteLabel(athlete) {
  if (!athlete) {
    return t('common:athleteUnavailable');
  }
  return athlete.affiliation ? `${athlete.name} · ${athlete.affiliation}` : athlete.name;
}

export function winnerLabel(bout) {
  if (!bout?.resultConfirmed || !bout.winnerSide || bout.winnerSide === 'NONE') {
    return null;
  }
  if (bout.winnerSide === 'DRAW') {
    return t('common:draw');
  }
  if (bout.winnerSide !== 'RED' && bout.winnerSide !== 'BLUE') {
    return null;
  }
  const winner = bout.winnerSide === 'RED' ? bout.redAthlete : bout.blueAthlete;
  return winner?.name || t('common:confirmed');
}

export function winnerText(bout) {
  const winner = winnerLabel(bout);
  if (!winner) {
    return '-';
  }
  return bout.winnerSide === 'DRAW' ? winner : t('common:winner', { value1: winner });
}

export const statusLabels = {
  SCHEDULED: '예정',
  READY: '준비',
  IN_PROGRESS: '진행 중',
  FINISHED: '종료',
};

export function statusLabel(status) {
  return statusLabels[status] || status || '정보 없음';
}

export function athleteLabel(athlete) {
  if (!athlete) {
    return '선수 정보 없음';
  }
  return athlete.affiliation ? `${athlete.name} · ${athlete.affiliation}` : athlete.name;
}

export function winnerLabel(bout) {
  if (!bout?.resultConfirmed || !bout.winnerSide || bout.winnerSide === 'NONE') {
    return null;
  }
  const winner = bout.winnerSide === 'RED' ? bout.redAthlete : bout.blueAthlete;
  return winner?.name || '확정됨';
}

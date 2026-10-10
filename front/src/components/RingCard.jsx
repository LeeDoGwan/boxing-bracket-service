import { useTranslation } from 'react-i18next';
import { translate as t, resolveMessage } from '../i18n';
import { athleteLabel, statusLabel } from '../utils';

function BoutSummary({ bout, sideLabel, emptyKey }) {
  useTranslation();
  if (!bout) {
    return <p className="empty-copy">{t(emptyKey)}</p>;
  }
  return (
    <div className="bout-summary">
      <div className="bout-summary-heading">
        <p className="summary-label">{sideLabel}</p>
        <p className="bout-number">{t('common:boutNumber', { value1: bout.boutNumber })}</p>
      </div>
      <p className="match-type">{bout.matchType || t('common:standardBout')}</p>
      <div className="fighter-pair red-fighter">
        <span className="red-label">{t('common:redCorner')}</span>
        <span><b>{bout.redAthleteName}</b><small>{bout.redAthleteAffiliation}</small></span>
      </div>
      <div className="fighter-pair blue-fighter">
        <span className="blue-label">{t('common:blueCorner')}</span>
        <span><b>{bout.blueAthleteName}</b><small>{bout.blueAthleteAffiliation}</small></span>
      </div>
      <p className="round-copy">{resolveMessage(statusLabel(bout.boutStatus))} · {bout.currentRound ? t('common:roundOrdinal', { value1: bout.currentRound }) : t('audience:ring.awaitingRound')}</p>
    </div>
  );
}

export function RingCard({ laterBouts = [], ring, onSelectBout }) {
  useTranslation();
  return (
    <article className={`ring-card ring-card-${ring.ringStatus?.toLowerCase() || 'unknown'}`}>
      <div className="ring-card-header">
        <div>
          <p className="eyebrow">{t('common:ring')}</p>
          <h3>{ring.ringName}</h3>
        </div>
        <span className={`ring-status status-pill status-${ring.ringStatus?.toLowerCase()}`}>
          <span aria-hidden="true" className="ring-status-dot" />
          {resolveMessage(statusLabel(ring.ringStatus))}
        </span>
      </div>
      <button
        aria-label={ring.currentBout ? t('audience:bout.showDetail', { value1: ring.currentBout.boutNumber }) : t('audience:ring.noCurrent')}
        className="current-bout-button"
        disabled={!ring.currentBout}
        onClick={() => onSelectBout(ring.currentBout?.boutId)}
        type="button"
      >
        <BoutSummary bout={ring.currentBout} sideLabel={t('common:currentBout')} emptyKey="audience:ring.noCurrent" />
      </button>
      <BoutSummary bout={ring.nextBout} sideLabel={t('common:nextBout')} emptyKey="audience:ring.noNext" />
      {laterBouts.length > 0 && (
        <div className="later-bouts">
          <p className="summary-label">{t('audience:ring.later')}</p>
          {laterBouts.map((bout) => (
            <button className="later-bout-button" key={bout.boutId} onClick={() => onSelectBout(bout.boutId)} type="button">
              <span>{t('common:boutNumber', { value1: bout.boutNumber })}</span>
              <strong>{resolveMessage(athleteLabel(bout.redAthlete))} vs {resolveMessage(athleteLabel(bout.blueAthlete))}</strong>
              <small>{resolveMessage(statusLabel(bout.status))}</small>
            </button>
          ))}
        </div>
      )}
    </article>
  );
}

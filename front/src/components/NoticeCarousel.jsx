import { useTranslation } from 'react-i18next';
import { translate as t } from '../i18n';
import { useEffect, useState } from 'react';

export function NoticeCarousel({ notices }) {
  useTranslation();
  const [activeIndex, setActiveIndex] = useState(0);
  const [isPaused, setIsPaused] = useState(false);

  useEffect(() => {
    setActiveIndex(0);
  }, [notices]);

  useEffect(() => {
    if (notices.length < 2 || isPaused) {
      return undefined;
    }
    const interval = window.setInterval(() => {
      setActiveIndex((current) => (current + 1) % notices.length);
    }, 6000);
    return () => window.clearInterval(interval);
  }, [isPaused, notices.length]);

  if (!notices.length) {
    return null;
  }

  const safeActiveIndex = Math.min(activeIndex, notices.length - 1);
  const notice = notices[safeActiveIndex];
  return (
    <section
      aria-label={t('audience:notice.title')}
      className="notice-banner"
      onFocus={() => setIsPaused(true)}
      onMouseEnter={() => setIsPaused(true)}
      onMouseLeave={() => setIsPaused(false)}
      onBlur={(event) => {
        if (!event.currentTarget.contains(event.relatedTarget)) setIsPaused(false);
      }}
    >
      <div aria-live="polite">
        <p className="eyebrow">{t('common:notice')}</p>
        <h2>{notice.title}</h2>
        <p>{notice.content}</p>
      </div>
      {notices.length > 1 && (
        <div aria-label={t('audience:notice.select')} className="notice-dots">
          {notices.map((item, index) => (
            <button
              aria-label={t('audience:notice.show', { value1: index + 1 })}
              aria-pressed={index === safeActiveIndex}
              className={index === safeActiveIndex ? 'active' : ''}
              key={item.noticeId}
              onClick={() => setActiveIndex(index)}
              type="button"
            />
          ))}
        </div>
      )}
    </section>
  );
}

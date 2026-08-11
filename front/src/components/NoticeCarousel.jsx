import { useEffect, useState } from 'react';

export function NoticeCarousel({ notices }) {
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
      aria-label="대회 공지"
      className="notice-banner"
      onFocus={() => setIsPaused(true)}
      onMouseEnter={() => setIsPaused(true)}
      onMouseLeave={() => setIsPaused(false)}
      onBlur={(event) => {
        if (!event.currentTarget.contains(event.relatedTarget)) setIsPaused(false);
      }}
    >
      <div aria-live="polite">
        <p className="eyebrow">NOTICE</p>
        <h2>{notice.title}</h2>
        <p>{notice.content}</p>
      </div>
      {notices.length > 1 && (
        <div aria-label="공지 선택" className="notice-dots">
          {notices.map((item, index) => (
            <button
              aria-label={`${index + 1}번째 공지 보기`}
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

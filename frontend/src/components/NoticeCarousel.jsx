import { useEffect, useState } from 'react';

export function NoticeCarousel({ notices }) {
  const [activeIndex, setActiveIndex] = useState(0);

  useEffect(() => {
    setActiveIndex(0);
  }, [notices]);

  useEffect(() => {
    if (notices.length < 2) {
      return undefined;
    }
    const interval = window.setInterval(() => {
      setActiveIndex((current) => (current + 1) % notices.length);
    }, 6000);
    return () => window.clearInterval(interval);
  }, [notices.length]);

  if (!notices.length) {
    return null;
  }

  const notice = notices[activeIndex];
  return (
    <section aria-label="대회 공지" className="notice-banner">
      <div>
        <p className="eyebrow">NOTICE</p>
        <h2>{notice.title}</h2>
        <p>{notice.content}</p>
      </div>
      {notices.length > 1 && (
        <div aria-label="공지 선택" className="notice-dots">
          {notices.map((item, index) => (
            <button
              aria-label={`${index + 1}번째 공지 보기`}
              aria-pressed={index === activeIndex}
              className={index === activeIndex ? 'active' : ''}
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

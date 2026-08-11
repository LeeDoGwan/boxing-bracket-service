import { act, fireEvent, render, screen } from '@testing-library/react';
import { NoticeCarousel } from './NoticeCarousel';

describe('NoticeCarousel', () => {
  afterEach(() => {
    vi.useRealTimers();
  });

  it('shows an empty state when no notices exist', () => {
    const { container } = render(<NoticeCarousel notices={[]} />);

    expect(container.firstChild).toBeNull();
  });

  it('changes notices by timer and dot selection', () => {
    vi.useFakeTimers();
    render(<NoticeCarousel notices={[
      { content: 'First content', noticeId: 1, title: 'First notice' },
      { content: 'Second content', noticeId: 2, title: 'Second notice' },
    ]} />);

    expect(screen.getByRole('heading', { name: 'First notice' })).toBeInTheDocument();
    act(() => vi.advanceTimersByTime(6000));
    expect(screen.getByRole('heading', { name: 'Second notice' })).toBeInTheDocument();

    fireEvent.click(screen.getAllByRole('button')[0]);
    expect(screen.getByRole('heading', { name: 'First notice' })).toBeInTheDocument();
  });

  it('keeps rendering when the notice list shrinks', () => {
    const { rerender } = render(<NoticeCarousel notices={[
      { content: 'First content', noticeId: 1, title: 'First notice' },
      { content: 'Second content', noticeId: 2, title: 'Second notice' },
    ]} />);

    fireEvent.click(screen.getAllByRole('button')[1]);
    rerender(<NoticeCarousel notices={[{ content: 'First content', noticeId: 1, title: 'First notice' }]} />);

    expect(screen.getByRole('heading', { name: 'First notice' })).toBeInTheDocument();
  });
});

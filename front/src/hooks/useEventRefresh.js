import { useCallback, useEffect, useRef } from 'react';

export function useEventRefresh(refresh, delay = 120) {
  const refreshHandler = useRef(refresh);
  const timer = useRef(null);
  const inFlight = useRef(false);
  const queued = useRef(false);
  const mounted = useRef(false);
  const refreshRun = useRef(null);

  useEffect(() => {
    refreshHandler.current = refresh;
  }, [refresh]);

  useEffect(() => {
    mounted.current = true;
    return () => {
      mounted.current = false;
      if (timer.current) {
        clearTimeout(timer.current);
      }
    };
  }, []);

  const schedule = useCallback(() => {
    if (!mounted.current) {
      return;
    }
    if (timer.current) {
      clearTimeout(timer.current);
    }
    timer.current = setTimeout(() => {
      timer.current = null;
      refreshRun.current?.();
    }, delay);
  }, [delay]);

  refreshRun.current = async () => {
    if (!mounted.current) {
      return;
    }
    if (inFlight.current) {
      queued.current = true;
      return;
    }
    inFlight.current = true;
    try {
      await refreshHandler.current?.();
    } catch {
      // The page keeps its current data and exposes request errors itself.
    } finally {
      inFlight.current = false;
      if (queued.current && mounted.current) {
        queued.current = false;
        schedule();
      }
    }
  };

  return schedule;
}

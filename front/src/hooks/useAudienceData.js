import { useCallback, useEffect, useRef, useState } from 'react';
import { getBouts, getHome } from '../api/audience';

const initialState = {
  bouts: [],
  dataTournamentId: null,
  home: null,
  loading: true,
  error: null,
};

export function useAudienceData(tournamentId) {
  const [state, setState] = useState(initialState);
  const requestRef = useRef({ controller: null, id: 0 });

  const reload = useCallback(async () => {
    requestRef.current.controller?.abort();
    const requestId = requestRef.current.id + 1;
    const controller = new AbortController();
    requestRef.current = { controller, id: requestId };
    setState((current) => ({ ...current, loading: true, error: null }));
    try {
      const [homeResult, boutsResult] = await Promise.allSettled([
        getHome(tournamentId, { signal: controller.signal }),
        getBouts(tournamentId, { signal: controller.signal }),
      ]);
      if (requestRef.current.id !== requestId) {
        return;
      }
      if (homeResult.status === 'rejected') {
        throw homeResult.reason;
      }
      setState({
        bouts: boutsResult.status === 'fulfilled' ? boutsResult.value || [] : [],
        dataTournamentId: tournamentId,
        home: homeResult.value,
        loading: false,
        error: null,
      });
    } catch (error) {
      if (error?.name === 'AbortError' || requestRef.current.id !== requestId) {
        return;
      }
      setState((current) => ({ ...current, loading: false, error }));
    }
  }, [tournamentId]);

  useEffect(() => {
    reload();
    return () => {
      requestRef.current.controller?.abort();
      requestRef.current.id += 1;
    };
  }, [reload]);

  return { ...state, reload };
}

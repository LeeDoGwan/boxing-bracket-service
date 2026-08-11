import { useCallback, useEffect, useRef, useState } from 'react';
import { getBouts, getHome } from '../api/audience';

const initialState = {
  bouts: [],
  boutsError: null,
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
    setState((current) => ({ ...current, boutsError: null, loading: true, error: null }));
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
      const partialError = boutsResult.status === 'rejected' ? boutsResult.reason : null;
      setState({
        bouts: boutsResult.status === 'fulfilled' ? boutsResult.value || [] : [],
        boutsError: partialError,
        dataTournamentId: tournamentId,
        home: homeResult.value,
        loading: false,
        error: null,
      });
    } catch (error) {
      if (error?.name === 'AbortError' || requestRef.current.id !== requestId) {
        return;
      }
      setState((current) => ({ ...current, boutsError: null, loading: false, error }));
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

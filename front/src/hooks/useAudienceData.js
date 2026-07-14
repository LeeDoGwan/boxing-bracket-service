import { useCallback, useEffect, useState } from 'react';
import { getHome, getNotices, getRingStatuses } from '../api/audience';

const initialState = {
  home: null,
  notices: [],
  ringStatuses: [],
  loading: true,
  error: null,
};

export function useAudienceData(tournamentId) {
  const [state, setState] = useState(initialState);

  const reload = useCallback(async () => {
    setState((current) => ({ ...current, loading: true, error: null }));
    try {
      const [home, notices, ringStatuses] = await Promise.all([
        getHome(tournamentId),
        getNotices(tournamentId),
        getRingStatuses(tournamentId),
      ]);
      setState({ home, notices, ringStatuses, loading: false, error: null });
    } catch (error) {
      setState((current) => ({ ...current, loading: false, error }));
    }
  }, [tournamentId]);

  useEffect(() => {
    reload();
  }, [reload]);

  return { ...state, reload };
}

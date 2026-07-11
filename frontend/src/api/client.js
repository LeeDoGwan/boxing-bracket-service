const configuredBaseUrl = (import.meta.env.VITE_API_BASE_URL || '').replace(/\/$/, '');

export function buildApiUrl(path, params = {}) {
  const url = new URL(`${configuredBaseUrl}${path}`, window.location.origin);
  Object.entries(params).forEach(([key, value]) => {
    if (value !== null && value !== undefined && value !== '') {
      url.searchParams.set(key, value);
    }
  });
  return configuredBaseUrl ? url.toString() : `${url.pathname}${url.search}`;
}

export async function getApi(path, params) {
  const response = await fetch(buildApiUrl(path, params), {
    headers: { Accept: 'application/json' },
  });

  let payload;
  try {
    payload = await response.json();
  } catch {
    throw new Error('INVALID_RESPONSE');
  }

  if (!response.ok || !payload.success) {
    throw new Error(payload.message || 'REQUEST_FAILED');
  }
  return payload.data;
}

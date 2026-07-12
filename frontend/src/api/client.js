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

function requestHeaders(token, hasBody, isFormData) {
  return {
    Accept: 'application/json',
    ...(hasBody && !isFormData ? { 'Content-Type': 'application/json' } : {}),
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
  };
}

export async function requestApi(path, { body, method = 'GET', params, token } = {}) {
  const isFormData = typeof FormData !== 'undefined' && body instanceof FormData;
  const response = await fetch(buildApiUrl(path, params), {
    body: body === undefined ? undefined : isFormData ? body : JSON.stringify(body),
    headers: requestHeaders(token, body !== undefined, isFormData),
    method,
  });

  let payload;
  try {
    payload = await response.json();
  } catch {
    throw new Error('INVALID_RESPONSE');
  }

  if (!response.ok || !payload.success) {
    const error = new Error(payload.message || 'REQUEST_FAILED');
    error.status = response.status;
    throw error;
  }
  return payload.data;
}

export function getApi(path, params, options = {}) {
  return requestApi(path, { ...options, params });
}

export function postApi(path, body, options = {}) {
  return requestApi(path, { ...options, body, method: 'POST' });
}

export function putApi(path, body, options = {}) {
  return requestApi(path, { ...options, body, method: 'PUT' });
}

export function deleteApi(path, options = {}) {
  return requestApi(path, { ...options, method: 'DELETE' });
}

export function postFormDataApi(path, body, options = {}) {
  return postApi(path, body, options);
}

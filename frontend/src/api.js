const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

// Token Management
export const getAccessToken = () => localStorage.getItem('accessToken');
export const getRefreshToken = () => localStorage.getItem('refreshToken');
export const getUser = () => {
  const user = localStorage.getItem('user');
  return user ? JSON.parse(user) : null;
};

export const setAuthSession = (authResponse) => {
  if (authResponse.accessToken) localStorage.setItem('accessToken', authResponse.accessToken);
  if (authResponse.refreshToken) localStorage.setItem('refreshToken', authResponse.refreshToken);
  const user = {
    userId: authResponse.userId,
    name: authResponse.name,
    phoneNumber: authResponse.phoneNumber,
  };
  localStorage.setItem('user', JSON.stringify(user));
};

export const clearAuthSession = () => {
  localStorage.removeItem('accessToken');
  localStorage.removeItem('refreshToken');
  localStorage.removeItem('user');
};

let isRefreshing = false;
let refreshSubscribers = [];

const subscribeTokenRefresh = (cb) => {
  refreshSubscribers.push(cb);
};

const onRefreshed = (newAccessToken) => {
  refreshSubscribers.forEach((cb) => cb(newAccessToken));
  refreshSubscribers = [];
};

/**
 * Core HTTP Request Wrapper with Auto-Bearer and Single 401 Refresh Retry
 */
export async function request(endpoint, options = {}, isRetry = false) {
  const url = `${API_BASE_URL}${endpoint}`;
  const headers = {
    'Content-Type': 'application/json',
    ...(options.headers || {}),
  };

  const token = getAccessToken();
  if (token && !headers['Authorization']) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  const config = {
    ...options,
    headers,
  };

  let response;
  try {
    response = await fetch(url, config);
  } catch (networkError) {
    throw new Error('Network error: Unable to connect to Pockets server.');
  }

  // Handle 401 Unauthorized with token refresh (only if not already retrying and not on login/register/refresh itself)
  if (response.status === 401 && !isRetry && !endpoint.startsWith('/api/auth/')) {
    const refreshToken = getRefreshToken();
    if (!refreshToken) {
      clearAuthSession();
      window.location.href = '/login';
      throw new Error('Session expired. Please log in again.');
    }

    if (!isRefreshing) {
      isRefreshing = true;
      try {
        const refreshRes = await fetch(`${API_BASE_URL}/api/auth/refresh`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ refreshToken }),
        });

        if (!refreshRes.ok) {
          throw new Error('Refresh token revoked or expired');
        }

        const newAuthData = await refreshRes.json();
        setAuthSession(newAuthData);
        isRefreshing = false;
        onRefreshed(newAuthData.accessToken);
      } catch (refreshErr) {
        isRefreshing = false;
        refreshSubscribers = [];
        clearAuthSession();
        window.location.href = '/login';
        throw new Error('Your session has expired. Please log in again.');
      }
    }

    // Wait for the token refresh to complete, then retry original request
    return new Promise((resolve, reject) => {
      subscribeTokenRefresh(async (newToken) => {
        try {
          const retryHeaders = {
            ...headers,
            Authorization: `Bearer ${newToken}`,
          };
          const retryRes = await request(endpoint, { ...options, headers: retryHeaders }, true);
          resolve(retryRes);
        } catch (err) {
          reject(err);
        }
      });
    });
  }

  // Handle 204 No Content
  if (response.status === 204) {
    return null;
  }

  // Parse JSON response body if present
  let data;
  const contentType = response.headers.get('content-type');
  if (contentType && contentType.includes('application/json')) {
    data = await response.json();
  } else {
    data = await response.text();
  }

  if (!response.ok) {
    let errorMessage = 'An error occurred';
    if (data && typeof data === 'object') {
      if (data.errors && typeof data.errors === 'object' && Object.keys(data.errors).length > 0) {
        errorMessage = Object.values(data.errors).join(', ');
      } else if (data.message) {
        errorMessage = data.message;
      } else if (data.error) {
        errorMessage = data.error;
      }
    } else if (typeof data === 'string' && data.length > 0) {
      errorMessage = data;
    }
    const error = new Error(errorMessage);
    error.status = response.status;
    error.data = data;
    throw error;
  }

  return data;
}

// API Module mapping to backend endpoints
export const api = {
  auth: {
    login: (phoneNumber, password) =>
      request('/api/auth/login', {
        method: 'POST',
        body: JSON.stringify({ phoneNumber, password }),
      }),
    register: (phoneNumber, password, name, payDay) =>
      request('/api/auth/register', {
        method: 'POST',
        body: JSON.stringify({
          phoneNumber,
          password,
          name: name || null,
          payDay: parseInt(payDay, 10),
        }),
      }),
    refresh: (refreshToken) =>
      request('/api/auth/refresh', {
        method: 'POST',
        body: JSON.stringify({ refreshToken }),
      }),
    logout: () => {
      clearAuthSession();
      window.location.href = '/login';
    },
  },

  pockets: {
    list: () => request('/api/pockets'),
    get: (id) => request(`/api/pockets/${id}`),
    create: (name, monthlyLimit) =>
      request('/api/pockets', {
        method: 'POST',
        body: JSON.stringify({ name, monthlyLimit: parseFloat(monthlyLimit) }),
      }),
    update: (id, name, monthlyLimit) =>
      request(`/api/pockets/${id}`, {
        method: 'PUT',
        body: JSON.stringify({ name, monthlyLimit: parseFloat(monthlyLimit) }),
      }),
    delete: (id) =>
      request(`/api/pockets/${id}`, {
        method: 'DELETE',
      }),
    reset: (id) =>
      request(`/api/pockets/${id}/reset`, {
        method: 'POST',
      }),
    resetAll: () =>
      request('/api/pockets/reset-all', {
        method: 'POST',
      }),
    overrideBalance: (id, currentBalance) =>
      request(`/api/pockets/${id}/balance`, {
        method: 'PATCH',
        body: JSON.stringify({ currentBalance: parseFloat(currentBalance) }),
      }),
  },

  transactions: {
    create: ({ pocketId, amount, payeeUpiId, note, idempotencyKey, rawQrPayload }) =>
      request('/api/transactions', {
        method: 'POST',
        body: JSON.stringify({
          pocketId,
          amount: amount !== undefined && amount !== null && amount !== '' ? parseFloat(amount) : null,
          payeeUpiId: payeeUpiId || null,
          note: note || null,
          idempotencyKey: idempotencyKey || crypto.randomUUID(),
          rawQrPayload: rawQrPayload || null,
        }),
      }),
    confirm: (id) =>
      request(`/api/transactions/${id}/confirm`, {
        method: 'POST',
      }),
    cancel: (id) =>
      request(`/api/transactions/${id}/cancel`, {
        method: 'POST',
      }),
    get: (id) => request(`/api/transactions/${id}`),
    listByPocket: (pocketId) => request(`/api/transactions?pocketId=${pocketId}`),
    log: ({ pocketId, amount, note }) =>
      request('/api/transactions/log', {
        method: 'POST',
        body: JSON.stringify({
          pocketId,
          amount: parseFloat(amount),
          note: note || null,
        }),
      }),
  },

  payments: {
    generateLink: (transactionId) =>
      request('/api/payments/generate-link', {
        method: 'POST',
        body: JSON.stringify({ transactionId }),
      }),
  },
};

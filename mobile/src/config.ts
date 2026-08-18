/** Change via app.json extra.apiUrl or EXPO_PUBLIC_API_URL env at build time. */
export const API_BASE_URL =
  process.env.EXPO_PUBLIC_API_URL ?? 'http://10.0.2.2:8000';

export const API_URL = `${API_BASE_URL}/api`;

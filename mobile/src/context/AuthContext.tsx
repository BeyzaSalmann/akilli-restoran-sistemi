import AsyncStorage from '@react-native-async-storage/async-storage';
import React, { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';

import { api, ApiError, setUnauthorizedHandler } from '../api/client';

const TOKEN_KEY = 'ars_waiter_token';
const NAME_KEY = 'ars_waiter_name';

interface AuthContextValue {
  token: string | null;
  waiterName: string | null;
  loading: boolean;
  login: (pin: string) => Promise<void>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [token, setToken] = useState<string | null>(null);
  const [waiterName, setWaiterName] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    (async () => {
      try {
        const [storedToken, storedName] = await Promise.all([
          AsyncStorage.getItem(TOKEN_KEY),
          AsyncStorage.getItem(NAME_KEY),
        ]);
        if (!storedToken) {
          return;
        }
        try {
          await api.getTables(storedToken);
          setToken(storedToken);
          setWaiterName(storedName);
        } catch (err) {
          const expired = err instanceof ApiError && err.status === 401;
          if (expired) {
            await AsyncStorage.multiRemove([TOKEN_KEY, NAME_KEY]);
          } else {
            setToken(storedToken);
            setWaiterName(storedName);
          }
        }
      } catch (err) {
        console.warn('AsyncStorage okunamadı, oturum sıfırlandı:', err);
        setToken(null);
        setWaiterName(null);
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  const login = useCallback(async (pin: string) => {
    const result = await api.login(pin);
    await AsyncStorage.setItem(TOKEN_KEY, result.token);
    await AsyncStorage.setItem(NAME_KEY, result.waiterName);
    setToken(result.token);
    setWaiterName(result.waiterName);
  }, []);

  const logout = useCallback(async () => {
    setToken(null);
    setWaiterName(null);
    await AsyncStorage.multiRemove([TOKEN_KEY, NAME_KEY]);
  }, []);

  useEffect(() => {
    setUnauthorizedHandler(() => {
      logout();
    });
    return () => setUnauthorizedHandler(null);
  }, [logout]);

  const value = useMemo(
    () => ({ token, waiterName, loading, login, logout }),
    [token, waiterName, loading, login, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error('useAuth must be used within AuthProvider');
  }
  return ctx;
}

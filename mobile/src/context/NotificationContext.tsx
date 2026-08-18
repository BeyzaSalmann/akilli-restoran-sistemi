import React, {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
} from 'react';
import { AppState, Platform } from 'react-native';
import * as Device from 'expo-device';

import { api } from '../api/client';
import { canRegisterRemotePush, getExpoPushToken, IS_EXPO_GO, showLocalNotification } from '../notifications/setup';
import { ORDER_STATUS_LABEL, formatOrderItemsSummary } from '../utils/labels';
import type { OrderSummary } from '../types';
import { useAuth } from './AuthContext';

const ORDER_POLL_MS = 3_000;
const EMOTION_POLL_MS = 10_000;

export type AppAlert = {
  kind: 'order' | 'emotion';
  title: string;
  body: string;
  emotionAlertId?: number;
};

interface NotificationContextValue {
  alert: AppAlert | null;
  dismissAlert: () => void;
}

const NotificationContext = createContext<NotificationContextValue | null>(null);

function orderFingerprint(orders: OrderSummary[]) {
  return orders
    .map((o) => `${o.id}:${o.status}`)
    .sort()
    .join('|');
}

async function raiseAlert(
  setAlert: (alert: AppAlert | null) => void,
  alert: AppAlert,
) {
  setAlert(alert);
  if (!IS_EXPO_GO) {
    await showLocalNotification(alert.title, alert.body);
  }
}

export function NotificationProvider({ children }: { children: React.ReactNode }) {
  const { token } = useAuth();
  const pushTokenRef = useRef<string | null>(null);
  const lastFingerprintRef = useRef<string>('');
  const lastEmotionAlertIdRef = useRef<number>(0);
  const emotionInitializedRef = useRef(false);
  const initializedRef = useRef(false);
  const [alert, setAlert] = useState<AppAlert | null>(null);
  const alertRef = useRef<AppAlert | null>(null);

  useEffect(() => {
    alertRef.current = alert;
  }, [alert]);

  const dismissAlert = useCallback(() => {
    const current = alertRef.current;
    setAlert(null);
    if (current?.kind === 'emotion' && current.emotionAlertId != null && token) {
      api.ackEmotionAlert(token, current.emotionAlertId).catch(() => {});
    }
  }, [token]);

  const registerPush = useCallback(async () => {
    if (!token || !canRegisterRemotePush() || !Device.isDevice) {
      return;
    }
    try {
      const pushToken = await getExpoPushToken();
      if (!pushToken || pushTokenRef.current === pushToken) {
        return;
      }
      pushTokenRef.current = pushToken;
      await api.registerPushToken(token, pushToken, Platform.OS);
    } catch (err) {
      console.warn('Push token kaydı başarısız:', err);
    }
  }, [token]);

  const pollOrders = useCallback(async () => {
    if (!token) {
      return;
    }
    try {
      const orders = await api.getActiveOrders(token);
      const fingerprint = orderFingerprint(orders);
      if (!initializedRef.current) {
        initializedRef.current = true;
        lastFingerprintRef.current = fingerprint;
        return;
      }
      if (fingerprint !== lastFingerprintRef.current) {
        const prevIds = new Set(
          lastFingerprintRef.current.split('|').filter(Boolean).map((x) => x.split(':')[0]),
        );
        const newcomers = orders.filter((o) => !prevIds.has(String(o.id)));
        if (newcomers.length > 0) {
          const order = newcomers[0];
          const title = 'Yeni sipariş';
          const itemsText = formatOrderItemsSummary(order.items);
          const body =
            order.tableNumber != null
              ? `Masa ${order.tableNumber} · ${itemsText}`
              : itemsText;
          await raiseAlert(setAlert, { kind: 'order', title, body });
        } else {
          const changed = orders.find((o) => {
            const prev = lastFingerprintRef.current
              .split('|')
              .find((entry) => entry.startsWith(`${o.id}:`));
            return prev && !prev.endsWith(`:${o.status}`);
          });
          if (changed) {
            const title =
              changed.tableNumber != null
                ? `Masa ${changed.tableNumber}`
                : `Sipariş #${changed.id}`;
            const body = `Durum: ${ORDER_STATUS_LABEL[changed.status]}`;
            await raiseAlert(setAlert, { kind: 'order', title, body });
          }
        }
        lastFingerprintRef.current = fingerprint;
      }
    } catch {
      // 401 → AuthContext otomatik çıkış
    }
  }, [token]);

  const pollEmotionAlerts = useCallback(async () => {
    if (!token) {
      return;
    }
    try {
      const alerts = await api.getEmotionAlerts(token);
      if (alerts.length === 0) {
        return;
      }
      const newest = alerts[0];
      if (!emotionInitializedRef.current) {
        emotionInitializedRef.current = true;
        lastEmotionAlertIdRef.current = newest.id;
        const title = '⚠ Duygu uyarısı';
        await raiseAlert(setAlert, {
          kind: 'emotion',
          title,
          body: newest.message,
          emotionAlertId: newest.id,
        });
        return;
      }
      if (newest.id > lastEmotionAlertIdRef.current) {
        lastEmotionAlertIdRef.current = newest.id;
        const title = '⚠ Duygu uyarısı';
        await raiseAlert(setAlert, {
          kind: 'emotion',
          title,
          body: newest.message,
          emotionAlertId: newest.id,
        });
      }
    } catch {
      // ignore
    }
  }, [token]);

  const pollAll = useCallback(async () => {
    await pollOrders();
  }, [pollOrders]);

  useEffect(() => {
    if (!token) {
      pushTokenRef.current = null;
      initializedRef.current = false;
      emotionInitializedRef.current = false;
      lastFingerprintRef.current = '';
      lastEmotionAlertIdRef.current = 0;
      setAlert(null);
      return;
    }
    registerPush();
    pollAll();
    pollEmotionAlerts();
    const orderInterval = setInterval(pollAll, ORDER_POLL_MS);
    const emotionInterval = setInterval(pollEmotionAlerts, EMOTION_POLL_MS);
    const sub = AppState.addEventListener('change', (state) => {
      if (state === 'active') {
        pollAll();
        pollEmotionAlerts();
      }
    });
    return () => {
      clearInterval(orderInterval);
      clearInterval(emotionInterval);
      sub.remove();
    };
  }, [token, registerPush, pollAll, pollEmotionAlerts]);

  useEffect(() => {
    if (!token || !pushTokenRef.current) {
      return;
    }
    return () => {
      const pushToken = pushTokenRef.current;
      if (pushToken) {
        api.unregisterPushToken(token, pushToken).catch(() => {});
      }
    };
  }, [token]);

  const value = useMemo(
    () => ({ alert, dismissAlert }),
    [alert, dismissAlert],
  );

  return (
    <NotificationContext.Provider value={value}>{children}</NotificationContext.Provider>
  );
}

export function useNotifications() {
  const ctx = useContext(NotificationContext);
  if (!ctx) {
    throw new Error('useNotifications must be used within NotificationProvider');
  }
  return ctx;
}

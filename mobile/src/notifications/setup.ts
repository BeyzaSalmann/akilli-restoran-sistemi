import Constants from 'expo-constants';

/** Expo Go cannot use expo-notifications on Android since SDK 53 — in-app banner only. */
export const IS_EXPO_GO = Constants.appOwnership === 'expo';

export function canRegisterRemotePush(): boolean {
  return !IS_EXPO_GO;
}

type NotificationsModule = typeof import('expo-notifications');

let configured = false;

async function loadNotifications(): Promise<NotificationsModule | null> {
  if (IS_EXPO_GO) {
    return null;
  }
  try {
    return await import('expo-notifications');
  } catch {
    return null;
  }
}

async function ensureHandler(Notifications: NotificationsModule) {
  if (configured) {
    return;
  }
  Notifications.setNotificationHandler({
    handleNotification: async () => ({
      shouldShowAlert: true,
      shouldPlaySound: true,
      shouldSetBadge: false,
      shouldShowBanner: true,
      shouldShowList: true,
    }),
  });
  configured = true;
}

export async function ensureNotificationPermissions(): Promise<boolean> {
  const Notifications = await loadNotifications();
  if (!Notifications) {
    return false;
  }
  const settings = await Notifications.getPermissionsAsync();
  if (settings.granted) {
    return true;
  }
  const requested = await Notifications.requestPermissionsAsync();
  return requested.granted;
}

/** Remote push token — development build / gerçek cihaz only. */
export async function getExpoPushToken(): Promise<string | null> {
  if (!canRegisterRemotePush()) {
    return null;
  }
  const Notifications = await loadNotifications();
  if (!Notifications) {
    return null;
  }
  const granted = await ensureNotificationPermissions();
  if (!granted) {
    return null;
  }
  try {
    const token = await Notifications.getExpoPushTokenAsync();
    return token.data;
  } catch {
    return null;
  }
}

/** Yerel bildirim — Expo Go'da atlanır (OrderAlertBanner kullanılır). */
export async function showLocalNotification(title: string, body: string) {
  if (IS_EXPO_GO) {
    return;
  }
  const Notifications = await loadNotifications();
  if (!Notifications) {
    return;
  }
  try {
    await ensureHandler(Notifications);
    const granted = await ensureNotificationPermissions();
    if (!granted) {
      return;
    }
    await Notifications.scheduleNotificationAsync({
      content: { title, body, sound: true },
      trigger: null,
    });
  } catch {
    // development build'de izin reddedilirse sessizce geç
  }
}

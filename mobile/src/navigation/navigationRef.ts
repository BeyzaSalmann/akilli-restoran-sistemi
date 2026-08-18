import { createNavigationContainerRef } from '@react-navigation/native';

import { RootStackParamList } from './types';

export const navigationRef = createNavigationContainerRef<RootStackParamList>();

export function navigateToActiveOrders() {
  if (navigationRef.isReady()) {
    navigationRef.navigate('ActiveOrders');
  }
}

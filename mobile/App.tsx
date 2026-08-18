import { NavigationContainer } from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { ActivityIndicator, View } from 'react-native';
import { StatusBar } from 'expo-status-bar';

import { AuthProvider, useAuth } from './src/context/AuthContext';
import { NotificationProvider, useNotifications } from './src/context/NotificationContext';
import OrderAlertBanner from './src/components/OrderAlertBanner';
import { navigationRef, navigateToActiveOrders } from './src/navigation/navigationRef';
import { RootStackParamList } from './src/navigation/types';
import ActiveOrdersScreen from './src/screens/ActiveOrdersScreen';
import LoginScreen from './src/screens/LoginScreen';
import OrderScreen from './src/screens/OrderScreen';
import TableDetailScreen from './src/screens/TableDetailScreen';
import TableListScreen from './src/screens/TableListScreen';

const Stack = createNativeStackNavigator<RootStackParamList>();

function AppNavigator() {
  const { token, loading } = useAuth();

  if (loading) {
    return (
      <View style={{ flex: 1, justifyContent: 'center', alignItems: 'center' }}>
        <ActivityIndicator size="large" />
      </View>
    );
  }

  return (
    <NavigationContainer ref={navigationRef} key={token ? 'authenticated' : 'guest'}>
      <Stack.Navigator
        initialRouteName={token ? 'TableList' : 'Login'}
        screenOptions={{ headerShown: true }}
      >
        {!token ? (
          <Stack.Screen name="Login" component={LoginScreen} options={{ headerShown: false }} />
        ) : (
          <>
            <Stack.Screen name="TableList" component={TableListScreen} options={{ title: 'Masalar' }} />
            <Stack.Screen
              name="TableDetail"
              component={TableDetailScreen}
              options={({ route }) => ({ title: `Masa ${route.params.tableNumber}` })}
            />
            <Stack.Screen
              name="Order"
              component={OrderScreen}
              options={({ route }) => ({ title: `Sipariş — Masa ${route.params.tableNumber}` })}
            />
            <Stack.Screen
              name="ActiveOrders"
              component={ActiveOrdersScreen}
              options={{ title: 'Aktif Siparişler' }}
            />
          </>
        )}
      </Stack.Navigator>
    </NavigationContainer>
  );
}

function AppShell() {
  const { alert, dismissAlert } = useNotifications();

  const onBannerPress = () => {
    dismissAlert();
    navigateToActiveOrders();
  };

  return (
    <>
      <AppNavigator />
      {alert ? (
        <OrderAlertBanner
          title={alert.title}
          body={alert.body}
          variant={alert.kind}
          onDismiss={dismissAlert}
          onPress={onBannerPress}
        />
      ) : null}
    </>
  );
}

export default function App() {
  return (
    <AuthProvider>
      <NotificationProvider>
        <StatusBar style="auto" />
        <AppShell />
      </NotificationProvider>
    </AuthProvider>
  );
}

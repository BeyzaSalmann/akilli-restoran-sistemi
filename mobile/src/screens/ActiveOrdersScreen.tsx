import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { useCallback, useState } from 'react';
import {
  ActivityIndicator,
  Alert,
  FlatList,
  Pressable,
  RefreshControl,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import { useFocusEffect } from '@react-navigation/native';

import { api, ApiError } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { RootStackParamList } from '../navigation/types';
import { OrderSummary, OrderStatus } from '../types';
import {
  formatPrice,
  NEXT_ORDER_STATUS,
  ORDER_STATUS_LABEL,
} from '../utils/labels';

type Props = NativeStackScreenProps<RootStackParamList, 'ActiveOrders'>;

export default function ActiveOrdersScreen({ navigation }: Props) {
  const { token } = useAuth();
  const [orders, setOrders] = useState<OrderSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [updatingId, setUpdatingId] = useState<number | null>(null);

  const load = useCallback(async () => {
    if (!token) return;
    try {
      const data = await api.getActiveOrders(token);
      setOrders(data);
    } catch {
      // 401 → AuthContext otomatik çıkış yapar
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, [token]);

  useFocusEffect(
    useCallback(() => {
      setLoading(true);
      load();
    }, [load]),
  );

  const advanceStatus = async (order: OrderSummary) => {
    if (!token || updatingId != null) return;
    const next = NEXT_ORDER_STATUS[order.status];
    if (!next) {
      Alert.alert('Bilgi', 'Bu sipariş için durum güncellenemez');
      return;
    }
    setUpdatingId(order.id);
    try {
      await api.updateOrderStatus(token, order.id, next);
      await load();
    } catch (err) {
      Alert.alert('Hata', err instanceof ApiError ? err.message : 'Güncellenemedi');
    } finally {
      setUpdatingId(null);
    }
  };

  const cancelOrder = (order: OrderSummary) => {
    if (!token) return;
    Alert.alert('İptal', `Sipariş #${order.id} iptal edilsin mi?`, [
      { text: 'Vazgeç', style: 'cancel' },
      {
        text: 'İptal Et',
        style: 'destructive',
        onPress: async () => {
          try {
            await api.updateOrderStatus(token, order.id, 'cancelled' satisfies OrderStatus);
            load();
          } catch (err) {
            Alert.alert('Hata', err instanceof ApiError ? err.message : 'İptal edilemedi');
          }
        },
      },
    ]);
  };

  if (loading && !refreshing) {
    return (
      <View style={styles.center}>
        <ActivityIndicator size="large" />
      </View>
    );
  }

  return (
    <FlatList
      style={styles.container}
      data={orders}
      keyExtractor={(item) => String(item.id)}
      refreshControl={
        <RefreshControl refreshing={refreshing} onRefresh={() => { setRefreshing(true); load(); }} />
      }
      contentContainerStyle={orders.length === 0 ? styles.emptyContainer : styles.list}
      ListEmptyComponent={<Text style={styles.empty}>Aktif sipariş yok</Text>}
      renderItem={({ item }) => (
        <View style={styles.card}>
          <Text style={styles.title}>
            Sipariş #{item.id}
            {item.tableNumber != null ? ` · Masa ${item.tableNumber}` : ''}
          </Text>
          <Text style={styles.meta}>
            Durum: <Text style={styles.statusValue}>{ORDER_STATUS_LABEL[item.status]}</Text>
          </Text>
          <Text style={styles.meta}>{formatPrice(item.totalAmount)}</Text>
          {item.items.map((line) => (
            <Text key={`${item.id}-${line.productId}`} style={styles.line}>
              {line.productName} x{line.quantity}
            </Text>
          ))}
          <View style={styles.actions}>
            {NEXT_ORDER_STATUS[item.status] ? (
              <Pressable
                style={[styles.advanceBtn, updatingId === item.id && styles.btnDisabled]}
                onPress={() => advanceStatus(item)}
                disabled={updatingId != null}
              >
                {updatingId === item.id ? (
                  <ActivityIndicator color="#fff" />
                ) : (
                  <Text style={styles.advanceText}>
                    {ORDER_STATUS_LABEL[item.status]} → {ORDER_STATUS_LABEL[NEXT_ORDER_STATUS[item.status]!]}
                  </Text>
                )}
              </Pressable>
            ) : item.status === 'served' ? (
              <Text style={styles.hint}>Hesap kapatmak için masa detayına gidin</Text>
            ) : null}
            {item.status !== 'cancelled' && item.status !== 'completed' ? (
              <Pressable style={styles.cancelBtn} onPress={() => cancelOrder(item)}>
                <Text style={styles.cancelText}>İptal</Text>
              </Pressable>
            ) : null}
          </View>
        </View>
      )}
    />
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#f5f5f5' },
  center: { flex: 1, justifyContent: 'center', alignItems: 'center' },
  list: { padding: 12 },
  emptyContainer: { flexGrow: 1, justifyContent: 'center', alignItems: 'center' },
  empty: { color: '#888', fontSize: 16 },
  card: {
    backgroundColor: '#fff',
    borderRadius: 8,
    padding: 14,
    marginBottom: 10,
  },
  title: { fontSize: 17, fontWeight: '700' },
  meta: { color: '#666', marginTop: 4 },
  statusValue: { color: '#1a73e8', fontWeight: '700' },
  line: { marginTop: 4 },
  actions: { flexDirection: 'row', marginTop: 12, gap: 8 },
  advanceBtn: {
    flex: 1,
    backgroundColor: '#1a73e8',
    borderRadius: 6,
    padding: 10,
    alignItems: 'center',
  },
  advanceText: { color: '#fff', fontWeight: '600' },
  btnDisabled: { opacity: 0.6 },
  hint: { flex: 1, color: '#856404', fontSize: 14, paddingVertical: 8 },
  cancelBtn: {
    paddingHorizontal: 16,
    borderRadius: 6,
    borderWidth: 1,
    borderColor: '#dc3545',
    justifyContent: 'center',
  },
  cancelText: { color: '#dc3545', fontWeight: '600' },
});

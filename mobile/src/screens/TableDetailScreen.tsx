import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { useCallback, useState } from 'react';
import {
  ActivityIndicator,
  Alert,
  Pressable,
  RefreshControl,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import { useFocusEffect } from '@react-navigation/native';

import { api, ApiError } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { RootStackParamList } from '../navigation/types';
import { TableDetail } from '../types';
import {
  formatPrice,
  ORDER_STATUS_LABEL,
  TABLE_STATUS_COLOR,
  TABLE_STATUS_LABEL,
} from '../utils/labels';

type Props = NativeStackScreenProps<RootStackParamList, 'TableDetail'>;

export default function TableDetailScreen({ route, navigation }: Props) {
  const { tableId, tableNumber } = route.params;
  const { token } = useAuth();
  const [table, setTable] = useState<TableDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [closing, setClosing] = useState(false);

  const load = useCallback(async () => {
    if (!token) return;
    try {
      const data = await api.getTable(token, tableId);
      setTable(data);
    } catch {
      // 401 → AuthContext otomatik çıkış yapar
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, [token, tableId]);

  useFocusEffect(
    useCallback(() => {
      setLoading(true);
      load();
    }, [load]),
  );

  const requestBill = async () => {
    if (!token) return;
    try {
      await api.updateTableStatus(token, tableId, 'bill_requested');
      load();
    } catch (err) {
      Alert.alert('Hata', err instanceof ApiError ? err.message : 'Durum güncellenemedi');
    }
  };

  const closeBill = async () => {
    if (!token) return;
    Alert.alert('Hesap Kapat', 'Masaya ait hesap kapatılsın mı?', [
      { text: 'İptal', style: 'cancel' },
      {
        text: 'Hesap Kapat',
        style: 'destructive',
        onPress: async () => {
          setClosing(true);
          try {
            const result = await api.closeBill(token, tableId);
            Alert.alert('Tamam', `Toplam: ${formatPrice(result.totalAmount)}`);
            navigation.goBack();
          } catch (err) {
            Alert.alert('Hata', err instanceof ApiError ? err.message : 'Hesap kapatılamadı');
          } finally {
            setClosing(false);
          }
        },
      },
    ]);
  };

  if (loading && !table) {
    return (
      <View style={styles.center}>
        <ActivityIndicator size="large" />
      </View>
    );
  }

  const status = table?.status ?? 'empty';

  return (
    <ScrollView
      style={styles.container}
      refreshControl={
        <RefreshControl refreshing={refreshing} onRefresh={() => { setRefreshing(true); load(); }} />
      }
    >
      <View style={styles.header}>
        <Text style={styles.title}>Masa {tableNumber}</Text>
        <Text style={[styles.status, { color: TABLE_STATUS_COLOR[status] }]}>
          {TABLE_STATUS_LABEL[status]}
        </Text>
      </View>

      <Pressable
        style={styles.primaryBtn}
        onPress={() => navigation.navigate('Order', { tableId, tableNumber })}
      >
        <Text style={styles.primaryBtnText}>Yeni Sipariş</Text>
      </Pressable>

      <Text style={styles.sectionTitle}>Aktif Siparişler</Text>
      {table?.activeOrders.length ? (
        table.activeOrders.map((order) => (
          <View key={order.id} style={styles.orderCard}>
            <Text style={styles.orderTitle}>Sipariş #{order.id}</Text>
            <Text style={styles.orderMeta}>{ORDER_STATUS_LABEL[order.status]}</Text>
            <Text style={styles.orderMeta}>{formatPrice(order.totalAmount)}</Text>
            {order.items.map((item) => (
              <Text key={`${order.id}-${item.productId}`} style={styles.itemLine}>
                {item.productName} x{item.quantity}
              </Text>
            ))}
          </View>
        ))
      ) : (
        <Text style={styles.empty}>Aktif sipariş yok</Text>
      )}

      <Pressable style={styles.secondaryBtn} onPress={requestBill}>
        <Text style={styles.secondaryBtnText}>Hesap İste</Text>
      </Pressable>

      <Pressable style={styles.dangerBtn} onPress={closeBill} disabled={closing}>
        <Text style={styles.dangerBtnText}>{closing ? '...' : 'Hesap Kapat'}</Text>
      </Pressable>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#f5f5f5' },
  center: { flex: 1, justifyContent: 'center', alignItems: 'center' },
  header: { padding: 16, backgroundColor: '#fff', marginBottom: 12 },
  title: { fontSize: 26, fontWeight: '700' },
  status: { marginTop: 6, fontSize: 16, fontWeight: '600' },
  primaryBtn: {
    marginHorizontal: 16,
    backgroundColor: '#1a73e8',
    borderRadius: 8,
    padding: 14,
    alignItems: 'center',
  },
  primaryBtnText: { color: '#fff', fontWeight: '600', fontSize: 16 },
  sectionTitle: { margin: 16, marginBottom: 8, fontSize: 18, fontWeight: '600' },
  orderCard: {
    backgroundColor: '#fff',
    marginHorizontal: 16,
    marginBottom: 10,
    borderRadius: 8,
    padding: 12,
  },
  orderTitle: { fontWeight: '700', fontSize: 16 },
  orderMeta: { color: '#666', marginTop: 4 },
  itemLine: { marginTop: 4 },
  empty: { marginHorizontal: 16, color: '#888' },
  secondaryBtn: {
    margin: 16,
    marginTop: 8,
    borderWidth: 1,
    borderColor: '#ffc107',
    borderRadius: 8,
    padding: 14,
    alignItems: 'center',
  },
  secondaryBtnText: { color: '#856404', fontWeight: '600' },
  dangerBtn: {
    marginHorizontal: 16,
    marginBottom: 32,
    backgroundColor: '#dc3545',
    borderRadius: 8,
    padding: 14,
    alignItems: 'center',
  },
  dangerBtnText: { color: '#fff', fontWeight: '600' },
});

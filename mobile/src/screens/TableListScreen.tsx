import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { useCallback, useState } from 'react';
import {
  ActivityIndicator,
  FlatList,
  Pressable,
  RefreshControl,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import { useFocusEffect } from '@react-navigation/native';

import { api } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { RootStackParamList } from '../navigation/types';
import { TableSummary } from '../types';
import { TABLE_STATUS_COLOR, TABLE_STATUS_LABEL } from '../utils/labels';

type Props = NativeStackScreenProps<RootStackParamList, 'TableList'>;

export default function TableListScreen({ navigation }: Props) {
  const { token, logout } = useAuth();
  const [tables, setTables] = useState<TableSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    if (!token) return;
    try {
      const data = await api.getTables(token);
      setTables(data);
      setError(null);
    } catch (err) {
      const message =
        err instanceof Error ? err.message : 'Masalar yüklenemedi.';
      setError(message);
      setTables([]);
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

  return (
    <View style={styles.container}>
      <View style={styles.header}>
        <View>
          <Text style={styles.title}>Masalar</Text>
          <Text style={styles.subtitle}>Hoş geldiniz</Text>
        </View>
        <Pressable onPress={() => navigation.navigate('ActiveOrders')}>
          <Text style={styles.link}>Siparişler</Text>
        </Pressable>
      </View>

      {loading && !refreshing ? (
        <ActivityIndicator style={styles.loader} size="large" />
      ) : error ? (
        <View style={styles.errorBox}>
          <Text style={styles.errorTitle}>Bağlantı hatası</Text>
          <Text style={styles.errorText}>{error}</Text>
          <Text style={styles.errorHint}>
            API&apos;nin çalıştığından emin olun (port 8000). Sorun devam ederse Çıkış yapıp PIN ile tekrar giriş yapın.
          </Text>
          <Pressable style={styles.retryBtn} onPress={() => { setLoading(true); load(); }}>
            <Text style={styles.retryText}>Yeniden dene</Text>
          </Pressable>
        </View>
      ) : (
        <FlatList
          data={tables}
          keyExtractor={(item) => String(item.id)}
          numColumns={2}
          refreshControl={
            <RefreshControl refreshing={refreshing} onRefresh={() => { setRefreshing(true); load(); }} />
          }
          contentContainerStyle={tables.length === 0 ? styles.listEmpty : styles.list}
          ListEmptyComponent={
            <Text style={styles.emptyText}>Gösterilecek masa bulunamadı.</Text>
          }
          renderItem={({ item }) => (
            <Pressable
              style={[styles.card, { borderColor: TABLE_STATUS_COLOR[item.status] }]}
              onPress={() => navigation.navigate('TableDetail', { tableId: item.id, tableNumber: item.number })}
            >
              <Text style={styles.cardTitle}>Masa {item.number}</Text>
              <Text style={[styles.cardStatus, { color: TABLE_STATUS_COLOR[item.status] }]}>
                {TABLE_STATUS_LABEL[item.status]}
              </Text>
            </Pressable>
          )}
        />
      )}

      <Pressable style={styles.logout} onPress={logout}>
        <Text style={styles.logoutText}>Çıkış</Text>
      </Pressable>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#f5f5f5' },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: 16,
    paddingTop: 56,
    backgroundColor: '#fff',
  },
  title: { fontSize: 24, fontWeight: '700' },
  subtitle: { color: '#666', marginTop: 4 },
  link: { color: '#1a73e8', fontWeight: '600', fontSize: 16 },
  loader: { marginTop: 40 },
  list: { padding: 12 },
  listEmpty: { padding: 24, flexGrow: 1, justifyContent: 'center' },
  emptyText: { textAlign: 'center', color: '#666', fontSize: 16 },
  errorBox: {
    margin: 16,
    marginTop: 32,
    padding: 20,
    backgroundColor: '#fff3cd',
    borderRadius: 12,
    borderWidth: 1,
    borderColor: '#ffc107',
  },
  errorTitle: { fontSize: 18, fontWeight: '700', color: '#856404', marginBottom: 8 },
  errorText: { fontSize: 15, color: '#856404', marginBottom: 12 },
  errorHint: { fontSize: 13, color: '#666', lineHeight: 20, marginBottom: 16 },
  retryBtn: {
    alignSelf: 'flex-start',
    backgroundColor: '#1a73e8',
    paddingHorizontal: 16,
    paddingVertical: 10,
    borderRadius: 8,
  },
  retryText: { color: '#fff', fontWeight: '600' },
  card: {
    flex: 1,
    margin: 8,
    minHeight: 100,
    backgroundColor: '#fff',
    borderRadius: 12,
    borderWidth: 3,
    padding: 16,
    justifyContent: 'center',
  },
  cardTitle: { fontSize: 22, fontWeight: '700' },
  cardStatus: { marginTop: 8, fontSize: 16, fontWeight: '600' },
  logout: { padding: 16, alignItems: 'center' },
  logoutText: { color: '#dc3545', fontWeight: '600' },
});

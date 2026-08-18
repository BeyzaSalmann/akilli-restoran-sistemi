import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  ActivityIndicator,
  Alert,
  FlatList,
  Pressable,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import { useFocusEffect } from '@react-navigation/native';

import { api, ApiError } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { RootStackParamList } from '../navigation/types';
import { MenuItem, RecommendationItem } from '../types';
import { formatPrice } from '../utils/labels';

type Props = NativeStackScreenProps<RootStackParamList, 'Order'>;

type CartLine = { product: MenuItem; quantity: number };

export default function OrderScreen({ route, navigation }: Props) {
  const { tableId, tableNumber } = route.params;
  const { token } = useAuth();
  const [menu, setMenu] = useState<MenuItem[]>([]);
  const [cart, setCart] = useState<Record<number, CartLine>>({});
  const [recommendations, setRecommendations] = useState<RecommendationItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadingRecs, setLoadingRecs] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  useFocusEffect(
    useCallback(() => {
      if (!token) return;
      let active = true;
      setLoading(true);
      (async () => {
        try {
          const items = await api.getMenuItems(token);
          if (active) setMenu(items);
        } catch {
          // 401 → AuthContext otomatik çıkış yapar
        } finally {
          if (active) setLoading(false);
        }
      })();
      return () => {
        active = false;
      };
    }, [token]),
  );

  const cartLines = useMemo(() => Object.values(cart), [cart]);
  const cartProductIds = useMemo(
    () => cartLines.map((line) => line.product.id),
    [cartLines],
  );
  const total = cartLines.reduce((sum, line) => sum + line.product.price * line.quantity, 0);

  useEffect(() => {
    if (!token || cartProductIds.length === 0) {
      setRecommendations([]);
      return;
    }
    const timer = setTimeout(async () => {
      setLoadingRecs(true);
      try {
        const items = await api.getRecommendations(token, cartProductIds);
        setRecommendations(items);
      } catch {
        setRecommendations([]);
      } finally {
        setLoadingRecs(false);
      }
    }, 400);
    return () => clearTimeout(timer);
  }, [token, cartProductIds.join(',')]);

  const addItem = (product: MenuItem) => {
    setCart((prev) => {
      const existing = prev[product.id];
      return {
        ...prev,
        [product.id]: {
          product,
          quantity: (existing?.quantity ?? 0) + 1,
        },
      };
    });
  };

  const removeItem = (productId: number) => {
    setCart((prev) => {
      const line = prev[productId];
      if (!line || line.quantity <= 1) {
        const next = { ...prev };
        delete next[productId];
        return next;
      }
      return { ...prev, [productId]: { ...line, quantity: line.quantity - 1 } };
    });
  };

  const submit = async () => {
    if (!token || cartLines.length === 0) {
      Alert.alert('Sepet boş', 'En az bir ürün seçin');
      return;
    }
    setSubmitting(true);
    try {
      await api.createOrder(
        token,
        tableId,
        cartLines.map((line) => ({ productId: line.product.id, quantity: line.quantity })),
      );
      Alert.alert('Başarılı', `Masa ${tableNumber} siparişi alındı`, [
        { text: 'Tamam', onPress: () => navigation.goBack() },
      ]);
      setCart({});
    } catch (err) {
      Alert.alert('Hata', err instanceof ApiError ? err.message : 'Sipariş gönderilemedi');
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <View style={styles.center}>
        <ActivityIndicator size="large" />
      </View>
    );
  }

  const recProduct = (id: number) => menu.find((m) => m.id === id);

  return (
    <View style={styles.container}>
      <Text style={styles.header}>Masa {tableNumber} — Menü</Text>
      <FlatList
        data={menu}
        keyExtractor={(item) => String(item.id)}
        contentContainerStyle={styles.list}
        ListHeaderComponent={
          cartProductIds.length > 0 ? (
            <View style={styles.recBox}>
              <Text style={styles.recTitle}>Önerilen (Apriori)</Text>
              {loadingRecs ? (
                <ActivityIndicator style={{ marginVertical: 8 }} />
              ) : recommendations.length === 0 ? (
                <Text style={styles.recEmpty}>Bu sepet için öneri yok</Text>
              ) : (
                recommendations.map((rec) => {
                  const product = recProduct(rec.productId);
                  if (!product) return null;
                  return (
                    <Pressable
                      key={rec.productId}
                      style={styles.recRow}
                      onPress={() => addItem(product)}
                    >
                      <View style={styles.recInfo}>
                        <Text style={styles.recName}>{rec.name}</Text>
                        <Text style={styles.recMeta}>
                          {formatPrice(rec.price)} · Güven %{Math.round(rec.confidence * 100)}
                        </Text>
                      </View>
                      <Text style={styles.recAdd}>+ Ekle</Text>
                    </Pressable>
                  );
                })
              )}
            </View>
          ) : null
        }
        renderItem={({ item }) => {
          const qty = cart[item.id]?.quantity ?? 0;
          return (
            <View style={styles.row}>
              <View style={styles.rowInfo}>
                <Text style={styles.name}>{item.name}</Text>
                <Text style={styles.meta}>{item.category} · {formatPrice(item.price)}</Text>
              </View>
              <View style={styles.qtyControls}>
                <Pressable style={styles.qtyBtn} onPress={() => removeItem(item.id)}>
                  <Text style={styles.qtyBtnText}>-</Text>
                </Pressable>
                <Text style={styles.qty}>{qty}</Text>
                <Pressable style={styles.qtyBtn} onPress={() => addItem(item)}>
                  <Text style={styles.qtyBtnText}>+</Text>
                </Pressable>
              </View>
            </View>
          );
        }}
      />
      <View style={styles.footer}>
        <Text style={styles.total}>Toplam: {formatPrice(total)}</Text>
        <Pressable style={styles.submit} onPress={submit} disabled={submitting}>
          <Text style={styles.submitText}>{submitting ? 'Gönderiliyor...' : 'Sipariş Ver'}</Text>
        </Pressable>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#f5f5f5' },
  center: { flex: 1, justifyContent: 'center', alignItems: 'center' },
  header: { padding: 16, paddingTop: 8, fontSize: 18, fontWeight: '600', backgroundColor: '#fff' },
  list: { paddingBottom: 120 },
  recBox: {
    backgroundColor: '#e8f4fd',
    marginHorizontal: 12,
    marginTop: 8,
    marginBottom: 4,
    borderRadius: 8,
    padding: 12,
    borderWidth: 1,
    borderColor: '#b3d9f2',
  },
  recTitle: { fontSize: 16, fontWeight: '700', marginBottom: 8, color: '#1a5a8a' },
  recEmpty: { color: '#666', fontSize: 14 },
  recRow: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#fff',
    borderRadius: 6,
    padding: 10,
    marginTop: 6,
  },
  recInfo: { flex: 1 },
  recName: { fontWeight: '600', fontSize: 15 },
  recMeta: { color: '#666', marginTop: 2, fontSize: 13 },
  recAdd: { color: '#1a73e8', fontWeight: '700' },
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#fff',
    marginHorizontal: 12,
    marginTop: 8,
    borderRadius: 8,
    padding: 12,
  },
  rowInfo: { flex: 1 },
  name: { fontSize: 16, fontWeight: '600' },
  meta: { color: '#666', marginTop: 4 },
  qtyControls: { flexDirection: 'row', alignItems: 'center' },
  qtyBtn: {
    width: 32,
    height: 32,
    borderRadius: 16,
    backgroundColor: '#eee',
    alignItems: 'center',
    justifyContent: 'center',
  },
  qtyBtnText: { fontSize: 18, fontWeight: '700' },
  qty: { width: 28, textAlign: 'center', fontWeight: '600' },
  footer: {
    position: 'absolute',
    left: 0,
    right: 0,
    bottom: 0,
    backgroundColor: '#fff',
    padding: 16,
    borderTopWidth: 1,
    borderTopColor: '#eee',
  },
  total: { fontSize: 18, fontWeight: '700', marginBottom: 12 },
  submit: {
    backgroundColor: '#1a73e8',
    borderRadius: 8,
    padding: 14,
    alignItems: 'center',
  },
  submitText: { color: '#fff', fontWeight: '600', fontSize: 16 },
});

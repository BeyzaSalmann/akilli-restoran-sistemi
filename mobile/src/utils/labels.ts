import { TableStatus, OrderStatus } from '../types';

export const TABLE_STATUS_LABEL: Record<TableStatus, string> = {
  empty: 'Boş',
  occupied: 'Dolu',
  bill_requested: 'Hesap İstendi',
};

export const ORDER_STATUS_LABEL: Record<OrderStatus, string> = {
  preparing: 'Hazırlanıyor',
  served: 'Servis Edildi',
  completed: 'Tamamlandı',
  cancelled: 'İptal Edildi',
};

export const TABLE_STATUS_COLOR: Record<TableStatus, string> = {
  empty: '#28a745',
  occupied: '#dc3545',
  bill_requested: '#ffc107',
};

export function formatPrice(value: number) {
  return `$${value.toFixed(2)}`;
}

/** Bildirim banner'ı için sipariş satır özeti (ürün adları). */
export function formatOrderItemsSummary(
  items: { productName: string; quantity: number }[],
): string {
  if (!items.length) {
    return 'Ürün bilgisi yok';
  }
  return items
    .map((item) =>
      item.quantity > 1 ? `${item.productName} x${item.quantity}` : item.productName,
    )
    .join(', ');
}

export const NEXT_ORDER_STATUS: Partial<Record<OrderStatus, OrderStatus>> = {
  preparing: 'served',
  // Tamamlandı yalnızca masa detayından "Hesap Kapat" ile (dashboard ile uyumlu)
};

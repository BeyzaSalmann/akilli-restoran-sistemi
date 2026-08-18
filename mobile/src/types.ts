export type TableStatus = 'empty' | 'occupied' | 'bill_requested';
export type OrderStatus = 'preparing' | 'served' | 'completed' | 'cancelled';

export interface TableSummary {
  id: number;
  number: number;
  status: TableStatus;
}

export interface OrderItemSummary {
  productId: number;
  productName: string;
  quantity: number;
  unitPrice: number;
  lineTotal: number;
}

export interface OrderSummary {
  id: number;
  tableNumber: number | null;
  status: OrderStatus;
  totalAmount: number;
  items: OrderItemSummary[];
}

export interface TableDetail extends TableSummary {
  activeOrders: OrderSummary[];
}

export interface MenuItem {
  id: number;
  name: string;
  category: string;
  price: number;
}

export interface CreateOrderResponse {
  orderId: number;
  tableNumber: number;
  totalAmount: number;
  status: OrderStatus;
}

export interface CloseBillResponse {
  tableNumber: number;
  totalAmount: number;
  completedOrderIds: number[];
}

export interface RecommendationItem {
  productId: number;
  name: string;
  price: number;
  confidence: number;
}

export interface EmotionAlertSummary {
  id: number;
  tableNumber: number;
  negativePercent: number;
  durationMinutes: number;
  message: string;
  createdAt: string;
}

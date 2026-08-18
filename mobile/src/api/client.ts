import { API_URL } from '../config';
import type {
  CloseBillResponse,
  CreateOrderResponse,
  EmotionAlertSummary,
  MenuItem,
  OrderStatus,
  OrderSummary,
  RecommendationItem,
  TableDetail,
  TableStatus,
  TableSummary,
} from '../types';

export class ApiError extends Error {
  status: number;

  constructor(status: number, message: string) {
    super(message);
    this.status = status;
  }
}

let onUnauthorized: (() => void) | null = null;
let unauthorizedHandled = false;

export function setUnauthorizedHandler(handler: (() => void) | null) {
  onUnauthorized = handler;
  unauthorizedHandled = false;
}

function handleUnauthorized() {
  if (!onUnauthorized || unauthorizedHandled) {
    return;
  }
  unauthorizedHandled = true;
  onUnauthorized();
}

async function request<T>(
  path: string,
  options: RequestInit = {},
  token?: string | null,
): Promise<T> {
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(options.headers as Record<string, string> | undefined),
  };
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  let response: Response;
  try {
    response = await fetch(`${API_URL}${path}`, { ...options, headers });
  } catch {
    throw new ApiError(
      0,
      'API\'ye bağlanılamadı. Sunucunun çalıştığından emin olun (port 8000).',
    );
  }
  if (!response.ok) {
    let detail = response.statusText;
    try {
      const body = await response.json();
      detail = body.detail ?? detail;
    } catch {
      // ignore parse errors
    }
    if (response.status === 401 && token) {
      handleUnauthorized();
    }
    throw new ApiError(response.status, String(detail));
  }
  if (response.status === 204) {
    return undefined as T;
  }
  return response.json() as Promise<T>;
}

export const api = {
  login(pin: string) {
    return request<{ token: string; waiterName: string }>('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ pin }),
    }).then((result) => {
      unauthorizedHandled = false;
      return result;
    });
  },

  getTables(token: string) {
    return request<TableSummary[]>('/tables', {}, token);
  },

  getTable(token: string, tableId: number) {
    return request<TableDetail>(`/tables/${tableId}`, {}, token);
  },

  updateTableStatus(token: string, tableId: number, status: TableStatus) {
    return request<TableSummary>(`/tables/${tableId}/status`, {
      method: 'PATCH',
      body: JSON.stringify({ status }),
    }, token);
  },

  closeBill(token: string, tableId: number) {
    return request<CloseBillResponse>(`/tables/${tableId}/close-bill`, {
      method: 'POST',
    }, token);
  },

  getMenuItems(token: string) {
    return request<MenuItem[]>('/menu/items', {}, token);
  },

  getRecommendations(token: string, productIds: number[]) {
    if (productIds.length === 0) {
      return Promise.resolve([] as RecommendationItem[]);
    }
    const query = productIds.join(',');
    return request<{ items: RecommendationItem[] }>(
      `/recommendations?productIds=${query}`,
      {},
      token,
    ).then((r) => r.items);
  },

  getActiveOrders(token: string, tableId?: number) {
    const query = tableId != null ? `?tableId=${tableId}` : '';
    return request<OrderSummary[]>(`/orders/active${query}`, {}, token);
  },

  createOrder(
    token: string,
    tableId: number,
    items: { productId: number; quantity: number }[],
  ) {
    return request<CreateOrderResponse>('/orders', {
      method: 'POST',
      body: JSON.stringify({ tableId, items }),
    }, token);
  },

  updateOrderStatus(token: string, orderId: number, status: OrderStatus) {
    return request<OrderSummary>(`/orders/${orderId}/status`, {
      method: 'PATCH',
      body: JSON.stringify({ status }),
    }, token);
  },

  registerPushToken(token: string, pushToken: string, platform?: string) {
    return request<{ status: string }>(
      '/notifications/register',
      {
        method: 'POST',
        body: JSON.stringify({ pushToken, platform }),
      },
      token,
    );
  },

  unregisterPushToken(token: string, pushToken: string) {
    return request<{ status: string }>(
      '/notifications/register',
      {
        method: 'DELETE',
        body: JSON.stringify({ pushToken }),
      },
      token,
    );
  },

  getEmotionAlerts(token: string) {
    return request<EmotionAlertSummary[]>('/emotion/alerts', {}, token);
  },

  ackEmotionAlert(token: string, alertId: number) {
    return request<{ status: string }>(`/emotion/alerts/${alertId}/ack`, {
      method: 'PATCH',
    }, token);
  },
};

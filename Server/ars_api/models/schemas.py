from typing import Literal

from pydantic import BaseModel, Field

TableStatus = Literal["empty", "occupied", "bill_requested"]
OrderStatus = Literal["preparing", "served", "completed", "cancelled"]


class LoginRequest(BaseModel):
    pin: str


class LoginResponse(BaseModel):
    token: str
    waiterName: str


class TableSummary(BaseModel):
    id: int
    number: int
    status: TableStatus


class OrderItemSummary(BaseModel):
    productId: int
    productName: str
    quantity: int
    unitPrice: float
    lineTotal: float


class OrderSummary(BaseModel):
    id: int
    tableNumber: int | None
    status: OrderStatus
    totalAmount: float
    items: list[OrderItemSummary] = Field(default_factory=list)


class TableDetail(BaseModel):
    id: int
    number: int
    status: TableStatus
    activeOrders: list[OrderSummary] = Field(default_factory=list)


class TableStatusUpdate(BaseModel):
    status: TableStatus


class MenuItem(BaseModel):
    id: int
    name: str
    category: str
    price: float


class OrderLineRequest(BaseModel):
    productId: int
    quantity: int = Field(ge=1)


class CreateOrderRequest(BaseModel):
    tableId: int
    items: list[OrderLineRequest] = Field(min_length=1)


class CreateOrderResponse(BaseModel):
    orderId: int
    tableNumber: int
    totalAmount: float
    status: OrderStatus


class OrderStatusUpdate(BaseModel):
    status: OrderStatus


class CloseBillResponse(BaseModel):
    tableNumber: int
    totalAmount: float
    completedOrderIds: list[int]


class RecommendationItem(BaseModel):
    productId: int
    name: str
    price: float
    confidence: float


class RecommendationsResponse(BaseModel):
    items: list[RecommendationItem] = Field(default_factory=list)


class PushTokenRegister(BaseModel):
    pushToken: str
    platform: str | None = None


class EmotionAlertSummary(BaseModel):
    id: int
    tableNumber: int
    negativePercent: float
    durationMinutes: int
    message: str
    createdAt: str

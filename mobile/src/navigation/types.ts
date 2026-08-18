export type RootStackParamList = {
  Login: undefined;
  TableList: undefined;
  TableDetail: { tableId: number; tableNumber: number };
  Order: { tableId: number; tableNumber: number };
  ActiveOrders: undefined;
};

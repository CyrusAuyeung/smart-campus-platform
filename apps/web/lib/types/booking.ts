export type UserItem = {
  id: string;
  studentNo: string;
  displayName: string;
  roleCode: string;
  creditScore: number;
  recentNoShowCount: number;
};

export type ResourceItem = {
  id: string;
  code: string;
  type: string;
  name: string;
  campus: string;
  building: string;
  capacity: number;
  status: string;
};

export type SportUnitItem = {
  id: string;
  facilityId: string;
  unitCode: string;
  name: string;
  status: string;
};

export type RuleResult = {
  allowed: boolean;
  ruleCode: string;
  message: string;
};

export type BookingReceipt = {
  orderId: string;
  orderNo: string;
  status: string;
  businessType: string;
  resourceType: string;
  resourceId: string;
  summary: string;
  displayStartAt: string | null;
  displayEndAt: string | null;
  effectiveStartAt: string | null;
  effectiveEndAt: string | null;
  bookingDate: string | null;
  slotIndices: number[];
  unitIds: string[];
  ruleResults: RuleResult[];
};

export type BookingPaymentConfirmRequest = {
  userId: string;
  transactionNo: string;
};

export type ReviewCase = {
  transactionNo: string;
  orderId: string;
  userId: string;
  status: string;
  callbackPayload: string;
  createdAt: string;
};

export type RuleConfigView = {
  ruleCode: string;
  enabled: boolean;
  priority: number;
  configJson: string;
};

export type CreditEvent = {
  id: string;
  userId: string;
  eventType: string;
  scoreDelta: number;
  reason: string;
  createdAt: string;
};

export type BootstrapPayload = {
  users: UserItem[];
  academicSpaces: ResourceItem[];
  sportFacilities: ResourceItem[];
  sportUnits: SportUnitItem[];
};

export type EventItem = {
  id: string;
  eventCode: string;
  title: string;
  status: string;
  startsAt: string;
  endsAt: string;
  totalStock: number;
  availableStock: number;
  limitPerUser: number;
};

export type EventReserveReceipt = {
  requestId: string;
  status: string;
  message: string;
  remainingStock: number;
};

export type EventHealthSnapshot = {
  pendingOrders: number;
  confirmedOrders: number;
  failedOrders: number;
};

export type EventReservationAudit = {
  requestId: string;
  eventId: string;
  userId: string;
  status: string;
  failureReason: string | null;
  createdAt: string;
};

export type EventReconciliationSnapshot = {
  eventId: string;
  databaseAvailableStock: number;
  cacheAvailableStock: number;
  consistent: boolean;
};

export type EventRepairAction = {
  eventId: string;
  actionType: string;
  previousCacheStock: number | null;
  databaseStock: number;
  operator: string;
  createdAt: string;
};

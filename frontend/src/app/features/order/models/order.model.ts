/**
 * Order Models
 *
 * 訂單相關的介面與類型定義
 */

// ============ 訂單狀態 ============

export type OrderStatus = '1' | '2' | '3' | '4' | '5' | '6';

export const ORDER_STATUS_MAP: Record<OrderStatus, string> = {
  '1': '草稿',
  '2': '報價',
  '3': '已付款',
  '4': '有效',
  '5': '結案',
  '6': '作廢'
};

// ============ 運送方式 ============

export type DeliveryMethod = 'N' | 'D' | 'V' | 'C' | 'F' | 'P';

export const DELIVERY_METHOD_MAP: Record<DeliveryMethod, string> = {
  'N': '運送',
  'D': '純運',
  'V': '直送',
  'C': '當場自取',
  'F': '宅配',
  'P': '下次自取'
};

// ============ 備貨方式 ============

export type StockMethod = 'X' | 'Y';

export const STOCK_METHOD_MAP: Record<StockMethod, string> = {
  'X': '現貨',
  'Y': '訂購'
};

// ============ 稅別 ============

export type TaxType = '0' | '1' | '2';

export const TAX_TYPE_MAP: Record<TaxType, string> = {
  '0': '零稅',
  '1': '應稅',
  '2': '免稅'
};

// ============ 會員折扣類型 ============

export type MemberDiscountType = '0' | '1' | '2' | 'SPECIAL';

export const MEMBER_DISCOUNT_TYPE_MAP: Record<MemberDiscountType, string> = {
  '0': '折價 (Discounting)',
  '1': '下降 (Down Margin)',
  '2': '成本加成 (Cost Markup)',
  'SPECIAL': '特殊會員'
};

// ============ 請求/回應介面 ============

export interface CustomerInfo {
  memberId: string | null;
  cardType: string | null;
  name: string;
  gender?: string;
  phone?: string;
  cellPhone: string;
  birthday?: string;
  contactName: string;
  contactPhone: string;
  vipType?: string;
  discountType?: MemberDiscountType;
  isTempCard: boolean;
}

export interface DeliveryAddress {
  zipCode: string;
  fullAddress: string;
}

export interface CreateOrderRequest {
  memberId?: string;
  customer: CustomerInfo;
  address: DeliveryAddress;
  storeId: string;
  channelId: string;
  lines?: OrderLineRequest[];
}

export interface OrderLineRequest {
  skuNo: string;
  quantity: number;
  deliveryMethod?: DeliveryMethod;
  stockMethod?: StockMethod;
}

export interface OrderResponse {
  orderId: string;
  projectId: string;
  status: OrderStatus;
  statusName: string;
  memberId?: string;
  memberName?: string;
  channelId?: string;
  storeId?: string;
  lines: OrderLineResponse[];
  calculation?: PriceCalculation;
  createdAt: string;
  createdBy?: string;
}

export interface AddOrderLineRequest {
  skuNo: string;
  quantity: number;
  deliveryMethod: DeliveryMethod;
  stockMethod: StockMethod;
}

export interface OrderLineResponse {
  lineId: string;
  serialNo: number;
  skuNo: string;
  skuName: string;
  quantity: number;
  unitPrice: number;
  actualUnitPrice: number;
  deliveryMethod: DeliveryMethod;
  deliveryMethodName: string;
  stockMethod: StockMethod;
  stockMethodName: string;
  taxType: TaxType;
  taxTypeName: string;
  subtotal: number;
  memberDisc: number;
  bonusDisc: number;
  couponDisc: number;
  // Installation & Delivery fields
  workTypeId?: string;
  workTypeName?: string;
  serviceTypes: string[];
  hasInstallation: boolean;
  installationCost: number;
  deliveryCost: number;
  deliveryDate?: string;
  receiverName?: string;
  receiverPhone?: string;
  deliveryAddress?: string;
}

export interface UpdateOrderLineRequest {
  quantity?: number;
  stockMethod?: StockMethod;
  deliveryMethod?: DeliveryMethod;
  workTypeId?: string;
  serviceTypes?: string[];
  receiverName?: string;
  receiverPhone?: string;
  deliveryAddress?: string;
  deliveryZipCode?: string;
  deliveryNote?: string;
}

// ============ 工種 ============

export interface WorkTypeResponse {
  workTypeId: string;
  workTypeName: string;
  category: string;
  categoryName: string;
  minimumWage: number;
  basicDiscount: number;
  advancedDiscount: number;
  deliveryDiscount: number;
}

// ============ 價格計算 ============

export interface PriceCalculation {
  productTotal: number;
  installationTotal: number;
  deliveryTotal: number;
  memberDiscount: number;
  directShipmentTotal: number;
  couponDiscount: number;
  taxAmount: number;
  grandTotal: number;
  memberDiscounts: MemberDiscVO[];
  warnings: string[];
  promotionSkipped: boolean;
  calculatedAt: string;
}

export interface MemberDiscVO {
  skuNo: string;
  discType: MemberDiscountType;
  discTypeName: string;
  originalPrice: number;
  discountPrice: number;
  discAmt: number;
  discRate?: number;
  markupRate?: number;
}

export interface ComputeTypeVO {
  computeType: string;
  computeName: string;
  totalPrice: number;
  discount: number;
  actTotalPrice: number;
}

export interface CalculationResponse {
  orderId: string;
  computeTypes: ComputeTypeVO[];
  memberDiscounts: MemberDiscVO[];
  grandTotal: number;
  taxAmount: number;
  promotionSkipped: boolean;
  warnings: string[];
  calculatedAt: string;
}

// ============ 會員 ============

export interface MemberResponse {
  memberId: string;
  cardType: string;
  name: string;
  birthday?: string;
  gender?: string;
  cellPhone: string;
  address?: string;
  zipCode?: string;
  discType?: MemberDiscountType;
  discTypeName?: string;
  discRate?: number;
  markupRate?: number;
  isTempCard: boolean;
}

export interface TempMemberRequest {
  name: string;
  cellPhone: string;
  address: string;
  zipCode: string;
}

// ============ 商品 ============

export interface ProductInfo {
  skuNo: string;
  skuName: string;
  category: string;
  taxType: TaxType;
  marketPrice: number;
  registeredPrice: number;
  posPrice: number;
  cost: number;
  allowSales: boolean;
  holdOrder: boolean;
  isSystemSku: boolean;
  isNegativeSku: boolean;
  freeDelivery: boolean;
  freeDeliveryShipping: boolean;
  allowDirectShipment: boolean;
  allowHomeDelivery: boolean;
}

export interface EligibilityResponse {
  eligible: boolean;
  failureReason?: string;
  failureLevel: number;
  product?: ProductInfo;
  availableServices: InstallationService[];
  availableStockMethods: StockMethod[];
  availableDeliveryMethods: DeliveryMethod[];
}

export interface InstallationService {
  serviceType: string;
  serviceName: string;
  serviceSku: string;
  basePrice: number;
  isMandatory: boolean;
  discountBase: number;
  discountExtra: number;
}

// ============ 優惠券 ============

export interface ApplyCouponRequest {
  couponId: string;
  quantity?: number;
}

export interface CouponValidation {
  valid: boolean;
  failureReason: string | null;
  discountAmount: Money | null;
  applicableSkus: string[];
  freeInstallation: boolean;
}

export interface Money {
  amount: number;
}

// ============ 紅利點數 ============

export interface RedeemBonusRequest {
  skuNo: string;
  points: number;
}

export interface BonusRedemption {
  memberId: string;
  skuNo: string;
  skuName: string;
  pointsUsed: number;
  discountAmount: Money;
  exchangeRate: number;
  remainingPoints: number;
}

// ============ 錯誤 ============

export interface ErrorResponse {
  errorCode: string;
  message: string;
  path: string;
  timestamp: string;
  traceId?: string;
  existingOrderId?: string;
}

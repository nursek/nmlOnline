export type ExchangeOfferStatus = 'PENDING' | 'ACCEPTED' | 'DECLINED' | 'CANCELLED' | 'EXPIRED';

export type ExchangeOfferStatusFilter = 'ALL' | ExchangeOfferStatus;

export interface ExchangeOfferItem {
  resourceName: string;
  quantity: number;
}

export interface ExchangeOffer {
  id: number;
  senderPlayerId: number;
  senderName: string;
  receiverPlayerId: number;
  receiverName: string;
  money: number;
  resources: ExchangeOfferItem[];
  status: ExchangeOfferStatus;
  createdTurn: number;
  expiresTurn: number;
  resolvedTurn: number | null;
  createdAt: string;
}

export interface CreateExchangeOfferPayload {
  receiverPlayerId: number;
  money: number;
  resources: ExchangeOfferItem[];
}

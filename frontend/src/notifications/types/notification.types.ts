export interface Notification {
  id: number;
  type: string;
  message: string;
  isRead: boolean;
  relatedEntityId: number | null;
  createdAt: string;
}

export interface NotificationListResponse {
  content: Notification[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

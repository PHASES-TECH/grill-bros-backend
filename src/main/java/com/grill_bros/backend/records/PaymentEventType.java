package com.grill_bros.backend.records;

public enum PaymentEventType {
    PAYMENT_INITIATED,
    REQUEST_TO_PAY_SENT,
    WEBHOOK_RECEIVED,
    PAYMENT_SUCCESS,
    PAYMENT_FAILED,
    PAYMENT_TIMEOUT,
    RECONCILIATION_CHECK
}

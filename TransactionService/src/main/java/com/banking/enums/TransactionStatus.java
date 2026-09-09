package com.banking.enums;
/*
* Transaction lifeCycle flow
* PENDING -> PROCESSING -> COMPLETED (clean transaction)
*                       -> PENDING.VERIFICATION(Suspicious Detected)
*                       -> completed (verified)
*                       -> FLAGGED (SAGA REFUND)
*         -> FAILED
*         -> FLAGGED
* */
public enum TransactionStatus {

    PENDING,
    PROCESSING,
    PENDING_VERIFICATION,
    COMPLETED,
    FAILED,
    FLAGGED



}

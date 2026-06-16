package com.global.order_api.core.exception;

import com.global.order_api.core.base.BaseException;

public class BusinessLogicException extends BaseException {

    public BusinessLogicException(String messageKey) {
        super(messageKey);
    }

    public BusinessLogicException(String messageKey, Object[] args) {
        super(messageKey, args);
    }
}

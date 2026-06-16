package com.global.order_api.core.exception;

import com.global.order_api.core.base.BaseException;

public class FileStorageException extends BaseException {

    public FileStorageException(String messageKey) {
        super(messageKey);
    }

    public FileStorageException(String messageKey, Object[] args) {
        super(messageKey, args);
    }
}

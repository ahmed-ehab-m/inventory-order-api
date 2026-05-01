package com.global.order_api.core.exception;

import com.global.order_api.core.base.BaseException;

public class DuplicateRecordException extends BaseException{
	
	public DuplicateRecordException(String messageKey)
	{
		super(messageKey);
	}
	
	public DuplicateRecordException(String messageKey, Object[] args) {
        super(messageKey, args);
    }
	
	public DuplicateRecordException(String resourceName, String fieldName, Object fieldValue) {
        super("error.resource.duplicate", resourceName, fieldName, fieldValue);
    }
}

package com.global.order_api.core.exception;

import com.global.order_api.core.base.BaseException;

public class ResourceNotFoundException extends BaseException{
	
	public ResourceNotFoundException(String messageKey)
	{
		super(messageKey);
	}
	
	public ResourceNotFoundException(String messageKey, Object[] args) {
        super(messageKey, args);
    }
}

package com.global.order_api.core.exception;

import com.global.order_api.core.base.BaseException;

public class BusinessLogicExceptionException extends BaseException{
	
	public BusinessLogicExceptionException(String messageKey)
	{
		super(messageKey);
	}
	
	public BusinessLogicExceptionException(String messageKey, Object[] args) {
        super(messageKey, args);
    }
}

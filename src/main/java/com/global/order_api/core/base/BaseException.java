package com.global.order_api.core.base;

import lombok.Getter;

@Getter
public class BaseException extends RuntimeException {
	
	private final String messageKey;
	private final Object[] args;
	
	public BaseException(String messageKey)
	{
		// to know RunTimeException class our message
		// to return our message in the console not null
		super(messageKey); 
		// we copy our message into our variable to send it to global exception handler
		// to present it or translate it
		this.messageKey=messageKey;
		this.args=null;
	}
	
	public BaseException(String messageKey, Object[] args) {
        super(messageKey);
        this.messageKey = messageKey;
        this.args = args;
    }
	
	// for me not user => logs in console in checked exceptions
	// to see the full cause (e) of exception (caused by) not just a message
	
	public BaseException(String messageKey ,Throwable cause)
	{
		super(messageKey,cause);
        this.messageKey = messageKey;
        this.args = null;
	}
	
}


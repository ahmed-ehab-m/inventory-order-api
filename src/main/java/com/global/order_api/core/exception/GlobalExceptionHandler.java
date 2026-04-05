package com.global.order_api.core.exception;

import java.util.ArrayList;
import java.util.List;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
 import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.global.order_api.core.response.ApiResponse;

import lombok.extern.log4j.Log4j2;

@RestControllerAdvice
@Log4j2
public class GlobalExceptionHandler {
	
	// to read messages properties files
	private final MessageSource messageSource;
	
	// inject message source
	public GlobalExceptionHandler(MessageSource messageSource)
	{
		this.messageSource=messageSource;
	}
	
	// DataBase and JPA Exceptions  
	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<?> handleDataBaseExceptions(DataIntegrityViolationException ex)
	{
		// message for user
		String message =translateMessage("error.data.integrity",null);
		return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(message));
	}
		
	// Validation Exceptions
	@ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationExceptions(MethodArgumentNotValidException ex) {
        List<String> errors = new ArrayList<>();
        // loop on errors to get field name + the error message
        // bindingResult => bind => linking user json request with DTO
        // Result => result of binding process to check if any validation annotations exceptions
        ex.getBindingResult().getFieldErrors().forEach(error ->
        {
        	String errorMessage=error.getField() + ": "+error.getDefaultMessage();
        	errors.add(errorMessage);
        }
            );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error("error.validation", errors));
    }
	
	//////////////////////////////////////////
	
	// 404 Not Found exception
	// to determine the type of exception will  be handled by this method
	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<?> handleResourceNotFoundException(ResourceNotFoundException ex)
	{
		String message =translateMessage(ex.getMessageKey(), ex.getArgs());
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(message));
	}
	
	// 409 Conflict exception
	@ExceptionHandler(DuplicateRecordException.class)
	public ResponseEntity<?> handleDuplicateRecordException(DuplicateRecordException ex)
	{
		String message =translateMessage(ex.getMessageKey(), ex.getArgs());
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(message));
	}
	
	// 400 Bad Request (user exceptions)
	// present message to inform user to change his mistake
	@ExceptionHandler(BusinessLogicException.class)
    public ResponseEntity<?> handleBusinessLogicException(BusinessLogicException ex) {
        String message = translateMessage(ex.getMessageKey(), ex.getArgs());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(message));
    }
	
	// 500 Internal Server Error
	// storage errors
	@ExceptionHandler(FileStorageException.class)
    public ResponseEntity<?> handleFileStorageException(FileStorageException ex) {
        
		log.error("File Storage Error", ex);
        String message = translateMessage(ex.getMessageKey(), ex.getArgs());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(message));
    }
	
	// 500 Internal Server Error
	// for global exceptions or unexpected exceptions (system exceptions)
	// like nullPointerException or DB connection failure or third party timeout
	// or arithmetic exception or parsing JSON error
	@ExceptionHandler(Exception.class)
	public ResponseEntity<?> handleGlobalException(Exception ex) {
		// logs in console for me
		log.error("Internal Server Error", ex);
	    // message for user
	    String message = translateMessage("error.internal.server", null);
	    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(message));
	}
	
	// for resource favicon (actuator)
	@ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<?> handleNoStaticResourceFound(NoResourceFoundException ex) {
        log.warn("Static resource not found: {}", ex.getResourcePath());
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Resource not found");
    }
		
	// helper method for translate message
	private String translateMessage(String messageKey , Object[] args)
	{
		try
		{	// get user language
			return messageSource.getMessage(messageKey, args ,LocaleContextHolder.getLocale());
		}
		catch (Exception e) {
			// return the same message key for me to know me that this key not found in messages files
			return messageKey;
		}
		
	}
	
}

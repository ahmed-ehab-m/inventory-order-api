package com.global.order_api.core.utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

@Component // to use it in another files without write new obj (DI)
@RequiredArgsConstructor
@Log4j2
public class AppTranslator {
    private final MessageSource messageSource;
    // helper method for translate message

    public String translateMessage(String messageKey, Object... args) {
        try {    // get user language
            return messageSource.getMessage(messageKey, args, LocaleContextHolder.getLocale());
        } catch (Exception e) {
            log.error("an error occured :" + e);
            // return the same message key for me to know me that this key not found in messages files
            return messageKey;
        }

    }

    // custom for entity name
    public String getTranslatedAction(String actionCode, String entityCode) {
        String translatedEntity = translateMessage(entityCode);
        return translateMessage(actionCode, translatedEntity);
    }
}

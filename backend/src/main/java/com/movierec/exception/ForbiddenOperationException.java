package com.movierec.exception;

import org.springframework.http.HttpStatus;

public class ForbiddenOperationException extends BusinessException {
    public ForbiddenOperationException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }
}

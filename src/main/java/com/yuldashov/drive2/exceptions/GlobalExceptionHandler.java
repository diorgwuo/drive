package com.yuldashov.drive2.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        ErrorResponse errorResponse = new ErrorResponse(
                400,
                message
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);

    }

    @ExceptionHandler(UserAlreadyExistException.class)
    public ResponseEntity<ErrorResponse> handleUsernameExist(UserAlreadyExistException exception) {
        ErrorResponse errorResponse = new ErrorResponse(
                409,
                exception.getMessage()
        );
        return new ResponseEntity<>(errorResponse,HttpStatus.CONFLICT);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAll(Exception exception) {
        ErrorResponse errorResponse = new ErrorResponse(
                500,
                "Неизвестная ошибка"
        );
        return new ResponseEntity<>(errorResponse,HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(InvalidCredentialsException exception) {
        ErrorResponse errorResponse = new ErrorResponse(
                401,
                exception.getMessage()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(UnautorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnautorizedUser(UnautorizedException exception) {
        ErrorResponse errorResponse = new ErrorResponse(
                401,
                exception.getMessage()
        );
        return new ResponseEntity<>(errorResponse,HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(InvalidPathException.class)
    public ResponseEntity<ErrorResponse> handleInvalidPath(InvalidPathException exception) {
        ErrorResponse errorResponse = new ErrorResponse(
                400,
                exception.getMessage()
        );
        return new ResponseEntity<>(errorResponse,HttpStatus.BAD_REQUEST);
    };

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFoundResource(ResourceNotFoundException exception) {
        ErrorResponse errorResponse = new ErrorResponse(
                404,
                exception.getMessage()
        );
        return new ResponseEntity<>(errorResponse,HttpStatus.NOT_FOUND);
    };

    @ExceptionHandler(ResourceAlreadyExistException.class)
    public ResponseEntity<ErrorResponse> handleResourceExist(ResourceAlreadyExistException exception) {
        ErrorResponse errorResponse = new ErrorResponse(
                409,
                exception.getMessage()
        );
        return new ResponseEntity<>(errorResponse,HttpStatus.CONFLICT);
    };

    @ExceptionHandler(InvalideFileException.class)
    public ResponseEntity<ErrorResponse> handleInvalidFile(InvalideFileException exception) {
        ErrorResponse errorResponse = new ErrorResponse(
                400,
                exception.getMessage()
        );
        return new ResponseEntity<>(errorResponse,HttpStatus.BAD_REQUEST);
    };


}

package com.teamchallenge.bookti.exception.handler;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

import com.teamchallenge.bookti.dto.ErrorResponse;
import com.teamchallenge.bookti.exception.BookNotFoundException;
import com.teamchallenge.bookti.exception.PasswordIsNotMatchesException;
import com.teamchallenge.bookti.exception.PasswordResetTokenIsExpiredException;
import com.teamchallenge.bookti.exception.PasswordResetTokenNotFoundException;
import com.teamchallenge.bookti.exception.RefreshTokenAlreadyRevokedException;
import com.teamchallenge.bookti.exception.UserAlreadyExistsException;
import com.teamchallenge.bookti.exception.UserNotFoundException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.mail.MailException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Implementation of {@link ResponseEntityExceptionHandler} that handles exceptions witch can be
 * thrown in application.
 *
 * @author Maksym Reva and Katherine Sokol
 */
@RestControllerAdvice
public class CustomExceptionHandler extends ResponseEntityExceptionHandler {

  @Override
  protected ResponseEntity<Object> handleExceptionInternal(
      @NonNull Exception e,
      Object body,
      @NonNull HttpHeaders headers,
      @NonNull HttpStatusCode statusCode,
      @NonNull WebRequest request) {
    ErrorResponse errorResponse = new ErrorResponse(statusCode.value(), e.getMessage());
    return super.handleExceptionInternal(e, errorResponse, headers, statusCode, request);
  }

  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      @NonNull MethodArgumentNotValidException e,
      @NonNull HttpHeaders headers,
      @NonNull HttpStatusCode status,
      @NonNull WebRequest request) {
    return e.getBindingResult().getAllErrors().stream()
        .map(ObjectError::getDefaultMessage)
        .collect(
            collectingAndThen(
                toList(),
                errorMessages ->
                    ResponseEntity.status(status)
                        .body(
                            new ErrorResponse(
                                status.value(), "Validation failed", errorMessages))));
  }

  @ExceptionHandler(UserAlreadyExistsException.class)
  public ResponseEntity<ErrorResponse> handleUserAlreadyExistException(
      UserAlreadyExistsException e) {
    ErrorResponse errorResponse = new ErrorResponse(CONFLICT.value(), e.getMessage());
    return ResponseEntity.status(CONFLICT).body(errorResponse);
  }

  @ExceptionHandler(PasswordIsNotMatchesException.class)
  public ResponseEntity<ErrorResponse> handlePasswordIsNotMatchesException(
      PasswordIsNotMatchesException e) {
    ErrorResponse errorResponse = new ErrorResponse(BAD_REQUEST.value(), e.getMessage());
    return ResponseEntity.status(BAD_REQUEST).body(errorResponse);
  }

  /**
   * Handles {@link AuthenticationException}.
   *
   * @param e {@link AuthenticationException} that can be thrown
   * @return {@link ErrorResponse} with {@link HttpStatus#UNAUTHORIZED unauthorized status}
   */
  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException e) {
    ErrorResponse errorResponse = new ErrorResponse(UNAUTHORIZED.value(), e.getMessage());
    return ResponseEntity.status(UNAUTHORIZED).body(errorResponse);
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException e) {
    ErrorResponse errorResponse = new ErrorResponse(FORBIDDEN.value(), e.getMessage());
    return ResponseEntity.status(FORBIDDEN).body(errorResponse);
  }

  @ExceptionHandler(UserNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleUserNotFoundException(UserNotFoundException e) {
    ErrorResponse errorResponse = new ErrorResponse(NOT_FOUND.value(), e.getMessage());
    return ResponseEntity.status(NOT_FOUND).body(errorResponse);
  }

  @ExceptionHandler(RefreshTokenAlreadyRevokedException.class)
  public ResponseEntity<ErrorResponse> handleRefreshTokenAlreadyRevokedException(
      RefreshTokenAlreadyRevokedException e) {
    ErrorResponse errorResponse = new ErrorResponse(CONFLICT.value(), e.getMessage());
    return ResponseEntity.status(CONFLICT).body(errorResponse);
  }

  /**
   * Handles {@link BadCredentialsException}.
   *
   * @param e {@link BadCredentialsException} that can be thrown
   * @return {@link ErrorResponse} with {@link HttpStatus#UNAUTHORIZED unauthorized status}
   */
  @ExceptionHandler(BadCredentialsException.class)
  public ResponseEntity<ErrorResponse> handleBadCredentialsException(BadCredentialsException e) {
    ErrorResponse errorResponse = new ErrorResponse(UNAUTHORIZED.value(), e.getMessage());
    return ResponseEntity.status(UNAUTHORIZED).body(errorResponse);
  }

  @ExceptionHandler(PasswordResetTokenNotFoundException.class)
  public ResponseEntity<ErrorResponse> handlePasswordResetTokenNotFoundException(
      PasswordResetTokenNotFoundException e) {
    ErrorResponse errorResponse = new ErrorResponse(NOT_FOUND.value(), e.getMessage());
    return ResponseEntity.status(NOT_FOUND).body(errorResponse);
  }

  @ExceptionHandler(PasswordResetTokenIsExpiredException.class)
  public ResponseEntity<ErrorResponse> handlePasswordResetTokenIsExpiredException(
      PasswordResetTokenIsExpiredException e) {
    ErrorResponse errorResponse = new ErrorResponse(NOT_FOUND.value(), e.getMessage());
    return ResponseEntity.status(NOT_FOUND).body(errorResponse);
  }

  @ExceptionHandler(MailException.class)
  public ResponseEntity<ErrorResponse> handleMailException(MailException e) {
    ErrorResponse errorResponse = new ErrorResponse(BAD_REQUEST.value(), e.getMessage());
    return ResponseEntity.status(BAD_REQUEST).body(errorResponse);
  }

  @ExceptionHandler(BookNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleBookNotFoundException(BookNotFoundException e) {
    ErrorResponse errorResponse = new ErrorResponse(NOT_FOUND.value(), e.getMessage());
    return ResponseEntity.status(NOT_FOUND).body(errorResponse);
  }

  /**
   * Handles {@link Exception}.
   *
   * @param e {@link Exception} that can be thrown
   * @return {@link ErrorResponse} with {@link HttpStatus#INTERNAL_SERVER_ERROR internal server
   *     error status}
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleInternalException(Exception e) {
    ErrorResponse errorResponse;
    if (e instanceof NullPointerException) {
      errorResponse = new ErrorResponse(BAD_REQUEST.value(), e.getMessage());
      return ResponseEntity.status(BAD_REQUEST).body(errorResponse);
    }
    errorResponse = new ErrorResponse(INTERNAL_SERVER_ERROR.value(), e.getMessage());
    return ResponseEntity.status(INTERNAL_SERVER_ERROR).body(errorResponse);
  }
}

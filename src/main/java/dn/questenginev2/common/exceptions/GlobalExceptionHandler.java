package dn.questenginev2.common.exceptions;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final String BASE_URI = "https://api.questenginev2.dn/problems";

  // ===== MethodArgumentNotValidException (ошибки @Valid в теле запроса) =====
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ProblemDetail handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex, WebRequest request) {

    List<ValidationError> errors =
        ex.getBindingResult().getFieldErrors().stream()
            .map(error -> new ValidationError(error.getField(), error.getDefaultMessage()))
            .collect(Collectors.toList());

    ProblemDetail problemDetail =
        createProblemDetail(
            ex,
            HttpStatus.BAD_REQUEST,
            request,
            "Validation Failed",
            "Request validation failed. Check the 'errors' field for details.");

    problemDetail.setProperty("errors", errors);
    log.error("Validation error: {}", errors);
    return problemDetail;
  }

  // ===== ConstraintViolationException (ошибки валидации параметров/путей) =====
  @ExceptionHandler(ConstraintViolationException.class)
  public ProblemDetail handleConstraintViolation(
      ConstraintViolationException ex, WebRequest request) {

    List<ValidationError> errors =
        ex.getConstraintViolations().stream()
            .map(
                violation ->
                    new ValidationError(
                        violation.getPropertyPath().toString(), violation.getMessage()))
            .collect(Collectors.toList());

    ProblemDetail problemDetail =
        createProblemDetail(
            ex,
            HttpStatus.BAD_REQUEST,
            request,
            "Constraint Violation",
            "Request constraint violation. Check the 'errors' field for details.");

    problemDetail.setProperty("errors", errors);
    log.error("Constraint violation: {}", errors);
    return problemDetail;
  }

  // ===== AccessDeniedException =====
  @ExceptionHandler(AccessDeniedException.class)
  public ProblemDetail handleAccessDenied(AccessDeniedException ex, WebRequest request) {
    ProblemDetail problemDetail =
        createProblemDetail(ex, HttpStatus.FORBIDDEN, request, "Access Denied", ex.getMessage());
    log.error("Access denied: {}", ex.getMessage());
    return problemDetail;
  }

  // ===== WrongPasswordException =====
  @ExceptionHandler(WrongPasswordException.class)
  public ProblemDetail handleWrongPasswordException(WrongPasswordException ex, WebRequest request) {
    ProblemDetail problemDetail =
        createProblemDetail(ex, HttpStatus.BAD_REQUEST, request, "Wrong Password", ex.getMessage());
    log.error("Wrong password attempt: {}", ex.getMessage());
    return problemDetail;
  }

  // ===== RequestAlreadyExistsException =====
  @ExceptionHandler(RequestAlreadyExistsException.class)
  public ProblemDetail handleRequestAlreadyExists(
      RequestAlreadyExistsException ex, WebRequest request) {
    ProblemDetail problemDetail =
        createProblemDetail(ex, HttpStatus.CONFLICT, request, "Conflict", ex.getMessage());
    log.error("Request already exists: {}", ex.getMessage());
    return problemDetail;
  }

  // ===== ForbiddenOperationException =====
  @ExceptionHandler(ForbiddenOperationException.class)
  public ProblemDetail handleForbiddenOperation(
      ForbiddenOperationException ex, WebRequest request) {
    ProblemDetail problemDetail =
        createProblemDetail(
            ex, HttpStatus.CONFLICT, request, "Forbidden Operation", ex.getMessage());
    log.error("Forbidden operation: {}", ex.getMessage());
    return problemDetail;
  }

  // ===== EntityNotFoundException =====
  @ExceptionHandler(EntityNotFoundException.class)
  public ProblemDetail handleEntityNotFound(EntityNotFoundException ex, WebRequest request) {
    ProblemDetail problemDetail =
        createProblemDetail(ex, HttpStatus.NOT_FOUND, request, "Entity Not Found", ex.getMessage());
    log.error("Entity not found: {}", ex.getMessage());
    return problemDetail;
  }

  // ===== UserNotFoundException =====
  @ExceptionHandler(UserNotFoundException.class)
  public ProblemDetail handleUserNotFound(UserNotFoundException ex, WebRequest request) {
    ProblemDetail problemDetail =
        createProblemDetail(ex, HttpStatus.NOT_FOUND, request, "User Not Found", ex.getMessage());
    log.error("User not found: {}", ex.getMessage());
    return problemDetail;
  }

  // ===== RequestNotFoundException =====
  @ExceptionHandler(RequestNotFoundException.class)
  public ProblemDetail handleRequestNotFound(RequestNotFoundException ex, WebRequest request) {
    ProblemDetail problemDetail =
        createProblemDetail(
            ex, HttpStatus.NOT_FOUND, request, "Request Not Found", ex.getMessage());
    log.error("Request not found: {}", ex.getMessage());
    return problemDetail;
  }

  // ===== TeamNotFoundException =====
  @ExceptionHandler(TeamNotFoundException.class)
  public ProblemDetail handleTeamNotFound(TeamNotFoundException ex, WebRequest request) {
    ProblemDetail problemDetail =
        createProblemDetail(ex, HttpStatus.NOT_FOUND, request, "Team Not Found", ex.getMessage());
    log.error("Team not found: {}", ex.getMessage());
    return problemDetail;
  }

  // ===== UserAlreadyExistsException =====
  @ExceptionHandler(UserAlreadyExistsException.class)
  public ProblemDetail handleUserAlreadyExists(UserAlreadyExistsException ex, WebRequest request) {
    ProblemDetail problemDetail =
        createProblemDetail(
            ex, HttpStatus.CONFLICT, request, "User Already Exists", ex.getMessage());
    log.error("User already exists: {}", ex.getMessage());
    return problemDetail;
  }

  // ===== UserAlreadyInTeamException =====
  @ExceptionHandler(UserAlreadyInTeamException.class)
  public ProblemDetail handleUserAlreadyInTeam(UserAlreadyInTeamException ex, WebRequest request) {
    ProblemDetail problemDetail =
        createProblemDetail(
            ex, HttpStatus.CONFLICT, request, "User Already In Team", ex.getMessage());
    log.error("User already in team: {}", ex.getMessage());
    return problemDetail;
  }

  // ===== TeamAlreadyExistsException =====
  @ExceptionHandler(TeamAlreadyExistsException.class)
  public ProblemDetail handleTeamAlreadyExists(TeamAlreadyExistsException ex, WebRequest request) {
    ProblemDetail problemDetail =
        createProblemDetail(
            ex, HttpStatus.CONFLICT, request, "Team Already Exists", ex.getMessage());
    log.error("Team already exists: {}", ex.getMessage());
    return problemDetail;
  }

  // ===== IllegalArgumentException / IllegalStateException =====
  @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
  public ProblemDetail handleConflict(RuntimeException ex, WebRequest request) {
    ProblemDetail problemDetail =
        createProblemDetail(ex, HttpStatus.CONFLICT, request, "Conflict", ex.getMessage());
    log.error("Conflict: {}", ex.getMessage());
    return problemDetail;
  }

  // ===== HttpMessageNotReadableException (невалидный JSON) =====
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ProblemDetail handleHttpMessageNotReadable(
      HttpMessageNotReadableException ex, WebRequest request) {
    ProblemDetail problemDetail =
        createProblemDetail(
            ex,
            HttpStatus.BAD_REQUEST,
            request,
            "Malformed JSON Request",
            "Request body is not valid JSON or has invalid structure.");
    log.error("Malformed JSON request: {}", ex.getMessage());
    return problemDetail;
  }

  // ===== MethodArgumentTypeMismatchException (неверный тип параметра) =====
  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ProblemDetail handleMethodArgumentTypeMismatch(
      MethodArgumentTypeMismatchException ex, WebRequest request) {
    String message =
        "Parameter '"
            + ex.getName()
            + "' should be of type "
            + ex.getRequiredType().getSimpleName();
    ProblemDetail problemDetail =
        createProblemDetail(ex, HttpStatus.BAD_REQUEST, request, "Type Mismatch", message);
    log.error("Type mismatch: {}", message);
    return problemDetail;
  }

  // ===== ResponseStatusException =====
  @ExceptionHandler(ResponseStatusException.class)
  public ProblemDetail handleResponseStatusException(
      ResponseStatusException ex, WebRequest request) {
    ProblemDetail problemDetail =
        createProblemDetail(
            ex,
            HttpStatus.valueOf(ex.getStatusCode().value()),
            request,
            ex.getReason() != null ? ex.getReason() : "Error",
            ex.getReason() != null ? ex.getReason() : "An error occurred");
    log.error("Response status exception: {}", ex.getMessage());
    return problemDetail;
  }

  // ===== NoResourceFoundException (ошибки неправильного роута) =====
  @ExceptionHandler(NoResourceFoundException.class)
  public ProblemDetail handleMethodArgumentNotValid(
      NoResourceFoundException ex, WebRequest request) {
    ProblemDetail problemDetail =
        createProblemDetail(
            ex, HttpStatus.NOT_FOUND, request, "Route not found error", ex.getBody().getTitle());
    log.error("Route not found status exception: {}", ex.getMessage());
    return problemDetail;
  }

  // ===== Все остальные исключения =====
  @ExceptionHandler(Exception.class)
  public ProblemDetail handleAll(Exception ex, WebRequest request) {
    log.error("Internal server error", ex);

    ProblemDetail problemDetail =
        createProblemDetail(
            ex,
            HttpStatus.INTERNAL_SERVER_ERROR,
            request,
            "Internal Server Error",
            "An unexpected error occurred. Please try again later.");

    problemDetail.setProperty("timestamp", LocalDateTime.now());
    return problemDetail;
  }

  private ProblemDetail createProblemDetail(
      Exception ex, HttpStatus status, WebRequest request, String title, String detail) {

    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
    problemDetail.setTitle(title);
    problemDetail.setType(URI.create(BASE_URI + "/" + getProblemType(ex)));
    problemDetail.setProperty("timestamp", LocalDateTime.now());

    if (request != null) {
      problemDetail.setInstance(URI.create(request.getDescription(false).replace("uri=", "")));
    }

    return problemDetail;
  }

  private String getProblemType(Exception ex) {
    if (ex instanceof MethodArgumentNotValidException) return "validation-error";
    if (ex instanceof ConstraintViolationException) return "constraint-violation";
    if (ex instanceof AccessDeniedException) return "access-denied";
    if (ex instanceof WrongPasswordException) return "wrong-password";
    if (ex instanceof RequestAlreadyExistsException) return "conflict";
    if (ex instanceof ForbiddenOperationException) return "forbidden-operation";
    if (ex instanceof EntityNotFoundException) return "entity-not-found";
    if (ex instanceof UserNotFoundException) return "user-not-found";
    if (ex instanceof RequestNotFoundException) return "request-not-found";
    if (ex instanceof TeamNotFoundException) return "team-not-found";
    if (ex instanceof UserAlreadyExistsException) return "user-already-exists";
    if (ex instanceof UserAlreadyInTeamException) return "user-already-in-team";
    if (ex instanceof TeamAlreadyExistsException) return "team-already-exists";
    if (ex instanceof IllegalArgumentException || ex instanceof IllegalStateException)
      return "conflict";
    if (ex instanceof HttpMessageNotReadableException) return "malformed-json";
    if (ex instanceof MethodArgumentTypeMismatchException) return "type-mismatch";
    if (ex instanceof ResponseStatusException) return "response-status";
    return "internal-error";
  }
}

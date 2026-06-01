package dn.questenginev2.common.exceptions;

import dn.questenginev2.QuestEngineV2Application;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Arrays;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private final static int STACK_TAIL = 5;
    private static final String PACKAGE_NAME = QuestEngineV2Application.class.getPackageName();

    // ===== RequestAlreadyExistsException =====
    @ExceptionHandler(RequestAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleRequestAlreadyExists(RequestAlreadyExistsException ex) {
        return buildResponseEntity(
                HttpStatus.NOT_FOUND,
                "Request Already Exists",
                ex.getMessage()
        );
    }

    // ===== UserNotFoundException =====
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFound(UserNotFoundException ex) {
        return buildResponseEntity(
                HttpStatus.NOT_FOUND,
                "User Was Not Found In DB",
                ex.getMessage()
        );
    }

    // ===== RequestNotFoundException =====
    @ExceptionHandler(RequestNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleRequestNotFound(RequestNotFoundException ex) {
        return buildResponseEntity(
                HttpStatus.NOT_FOUND,
                "Request Was Not Found In DB",
                ex.getMessage()
        );
    }

    // ===== TeamNotFoundException =====
    @ExceptionHandler(TeamNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTeamNotFound(TeamNotFoundException ex) {
        return buildResponseEntity(
                HttpStatus.NOT_FOUND,
                "Team Was Not Found In DB",
                ex.getMessage()
        );
    }

    // ===== UserAlreadyExistsException =====
    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleUserAlreadyExists(UserAlreadyExistsException ex) {
        return buildResponseEntity(
                HttpStatus.CONFLICT,
                "User Already Exists",
                ex.getMessage()
        );
    }

    // ===== UserAlreadyExistsException =====
    @ExceptionHandler(UserAlreadyInTeamException.class)
    public ResponseEntity<ErrorResponse> handleUserAlreadyExistsInTeam(UserAlreadyInTeamException ex) {
        return buildResponseEntity(
                HttpStatus.CONFLICT,
                "User Already In The Team",
                ex.getMessage()
        );
    }

    // ===== TeamAlreadyExistsException =====
    @ExceptionHandler(TeamAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleTeamAlreadyExists(TeamAlreadyExistsException ex) {
        return buildResponseEntity(
                HttpStatus.CONFLICT,
                "Team Already Exists",
                ex.getMessage()
        );
    }

    // ===== Конфликты, недопустимые состояния =====
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<ErrorResponse> handleConflict(RuntimeException ex) {
        return buildResponseEntity(
                HttpStatus.CONFLICT,
                "Conflict",
                ex.getMessage()
        );
    }

    // ===== Все остальные исключения =====
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAll(Exception ex) {
        StackTraceElement[] stackTraceElements = ex.getStackTrace();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder
                .append("Internal Server Error")
                .append("\n");

        Arrays.stream(stackTraceElements)
                .filter(stackTraceElement -> stackTraceElement
                        .getClassName().startsWith(PACKAGE_NAME))
                .skip(Math.max(0, stackTraceElements.length - STACK_TAIL))
                .forEach(stackTraceElement -> stringBuilder.append(stackTraceElement.toString()).append(" \n "));

        return buildResponseEntity(
                HttpStatus.INTERNAL_SERVER_ERROR,
                stringBuilder.toString(),
                ex.getMessage()
        );
    }

    private ResponseEntity<ErrorResponse> buildResponseEntity(HttpStatus status, String error, String message) {
        ErrorResponse body = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                error
        );
        log.error("Error log message: {}", message);

        return ResponseEntity.status(status).body(body);
    }
}

package dn.questenginev2.common.exceptions;

import dn.questenginev2.QuestEngineV2Application;
import org.springframework.boot.SpringApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private final static int STACK_TAIL = 5;
    private static final String PACKAGE_NAME = QuestEngineV2Application.class.getPackageName();

    // ===== UsernameNotFoundException =====
    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleEntityNotFound(UsernameNotFoundException ex) {
        return buildResponseEntity(
                HttpStatus.NOT_FOUND,
                "User Was Not Found In DB",
                ex.getMessage()
        );
    }

    // ===== UserAlreadyExistsException =====
    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleUserAlreadyExists(UserAlreadyExistsException ex) {
        return buildResponseEntity(
                HttpStatus.CONFLICT,
                "User Already Exists",
                ex.getMessage()
        );
    }

    // ===== TeamAlreadyExistsException =====
    @ExceptionHandler(TeamAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleTeamAlreadyExists(TeamAlreadyExistsException ex) {
        return buildResponseEntity(
                HttpStatus.CONFLICT,
                "Team Already Exists",
                ex.getMessage()
        );
    }

    // ===== Конфликты, недопустимые состояния =====
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<Map<String, Object>> handleConflict(RuntimeException ex) {
        return buildResponseEntity(
                HttpStatus.CONFLICT,
                "Conflict",
                ex.getMessage()
        );
    }

    // ===== Все остальные исключения =====
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleAll(Exception ex) {
        StackTraceElement[] stackTraceElements = ex.getStackTrace();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder
                .append("Internal Server Error, please check stack trace: ")
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

    private ResponseEntity<Map<String, Object>> buildResponseEntity(HttpStatus status, String error, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", error);
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
}

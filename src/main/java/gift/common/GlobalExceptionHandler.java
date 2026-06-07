package gift.common;

import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ConcurrencyFailureException.class)
    public ResponseEntity<String> handleConcurrencyFailure() {
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body("동시 요청으로 처리할 수 없습니다. 다시 시도해주세요.");
    }
}

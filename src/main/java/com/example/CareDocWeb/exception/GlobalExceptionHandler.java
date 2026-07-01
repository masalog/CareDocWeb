package com.example.CareDocWeb.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * グローバル例外ハンドラー。
 *
 * <p>コントローラー層で発生した例外をキャッチし、
 * 適切なHTTPステータスコードとエラーメッセージを返す。</p>
 *
 * <p>未ハンドルの {@link RuntimeException} は500 Internal Server Errorとして返却する。</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * RuntimeExceptionをハンドルし、500エラーとして返す。
     *
     * @param ex 発生した例外
     * @return 500ステータスとエラーメッセージ
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> handleRuntimeException(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ex.getMessage());
    }
}

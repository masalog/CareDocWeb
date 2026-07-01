package com.example.CareDocWeb.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

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

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

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

    /**
     * リクエストボディが不正な場合、400 Bad Requestを返す。
     *
     * @param ex 発生した例外
     * @return 400ステータスとエラーメッセージ
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<String> handleBadRequestBody(HttpMessageNotReadableException ex) {
        logger.warn("リクエストボディの読み取りに失敗: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("リクエストの形式が不正です");
    }

    /**
     * パスパラメータの型が不正な場合、400 Bad Requestを返す。
     *
     * @param ex 発生した例外
     * @return 400ステータスとエラーメッセージ
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<String> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        logger.warn("パラメータの型が不正: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("パラメータの形式が不正です");
    }

    /**
     * その他の予期しない例外をハンドルし、500エラーとして返す。
     *
     * <p>内部の例外メッセージはログに記録し、クライアントには汎用メッセージを返す。</p>
     *
     * @param ex 発生した例外
     * @return 500ステータスと汎用エラーメッセージ
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGenericException(Exception ex) {
        logger.error("予期しないエラーが発生しました", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("サーバー内部エラーが発生しました");
    }
}

package com.example.CareDocWeb.exception;

/**
 * リソースが見つからない場合にスローされる例外。
 *
 * <p>指定されたIDに対応するリソースがDB上に存在しない場合に使用する。
 * {@link GlobalExceptionHandler} によって404 Not Foundとして返却される。</p>
 */
public class ResourceNotFoundException extends RuntimeException {

    /**
     * エラーメッセージを指定してインスタンスを生成する。
     *
     * @param message エラーメッセージ
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }
}

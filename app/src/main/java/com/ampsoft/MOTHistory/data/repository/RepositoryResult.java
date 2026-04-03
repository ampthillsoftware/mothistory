package com.ampsoft.MOTHistory.data.repository;

public class RepositoryResult<T> {

    public enum Status {
        SUCCESS,
        ERROR,
        LOADING
    }

    private final Status status;
    private final T data;
    private final String message;
    private final int httpCode;

    private RepositoryResult(Status status, T data, String message, int httpCode) {
        this.status = status;
        this.data = data;
        this.message = message;
        this.httpCode = httpCode;
    }

    public static <T> RepositoryResult<T> success(T data) {
        return new RepositoryResult<>(Status.SUCCESS, data, null, 0);
    }

    public static <T> RepositoryResult<T> error(String message, int httpCode) {
        return new RepositoryResult<>(Status.ERROR, null, message, httpCode);
    }

    public static <T> RepositoryResult<T> loading() {
        return new RepositoryResult<>(Status.LOADING, null, null, 0);
    }

    public Status getStatus() {
        return status;
    }

    public T getData() {
        return data;
    }

    public String getMessage() {
        return message;
    }

    public int getHttpCode() {
        return httpCode;
    }
}

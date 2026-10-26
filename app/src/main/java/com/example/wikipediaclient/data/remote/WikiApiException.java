package com.example.wikipediaclient.data.remote;
import androidx.annotation.Nullable;
/** Single error type exposed to upper layers; Volley types never leak out of data/remote. */
public final class WikiApiException extends Exception {
    public enum Kind { NO_CONNECTION, TIMEOUT, SERVER, DATA, UNKNOWN }
    private final Kind kind;
    private final int statusCode;
    private final long retryAfterMs;
    public WikiApiException(Kind kind, String message, @Nullable Throwable cause, int statusCode, long retryAfterMs) {
        super(message, cause);
        this.kind = kind;
        this.statusCode = statusCode;
        this.retryAfterMs = retryAfterMs;
    }
    public WikiApiException(Kind kind, String message, @Nullable Throwable cause, int statusCode) {
        this(kind, message, cause, statusCode, -1);
    }
    public WikiApiException(Kind kind, String message, @Nullable Throwable cause) {
        this(kind, message, cause, -1);
    }
    public Kind getKind() { return kind; }
    /** HTTP status for {@link Kind#SERVER}, otherwise -1. */
    public int getStatusCode() { return statusCode; }
    /** Wait requested by the server's Retry-After header, or -1 when it sent none. */
    public long getRetryAfterMs() { return retryAfterMs; }
    /** 429 Too Many Requests or 503 Service Unavailable: the server asks us to back off. */
    public boolean isRateLimited() { return statusCode == 429 || statusCode == 503; }
}

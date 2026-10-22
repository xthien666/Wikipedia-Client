package com.example.wikipediaclient.data.remote;
import androidx.annotation.Nullable;
/** Single error type exposed to upper layers; Volley types never leak out of data/remote. */
public final class WikiApiException extends Exception {
    public enum Kind { NO_CONNECTION, TIMEOUT, SERVER, DATA, UNKNOWN }
    private final Kind kind;
    private final int statusCode;
    public WikiApiException(Kind kind, String message, @Nullable Throwable cause, int statusCode) {
        super(message, cause);
        this.kind = kind;
        this.statusCode = statusCode;
    }
    public WikiApiException(Kind kind, String message, @Nullable Throwable cause) {
        this(kind, message, cause, -1);
    }
    public Kind getKind() { return kind; }
    /** HTTP status for {@link Kind#SERVER}, otherwise -1. */
    public int getStatusCode() { return statusCode; }
}

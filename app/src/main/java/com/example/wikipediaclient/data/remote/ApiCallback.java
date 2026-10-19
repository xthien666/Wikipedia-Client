package com.example.wikipediaclient.data.remote;
/** Result callback, always delivered on the main thread. */
public interface ApiCallback<T> {
    void onSuccess(T result);
    void onError(WikiApiException error);
}

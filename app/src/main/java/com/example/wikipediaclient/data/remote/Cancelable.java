package com.example.wikipediaclient.data.remote;
/** Handle for an in-flight request. Cancelling guarantees the callback is never delivered. */
public interface Cancelable {
    void cancel();
}

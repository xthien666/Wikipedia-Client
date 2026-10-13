package com.example.wikipediaclient.data.local;

import android.content.Context;
import android.content.SharedPreferences;

/** Persistent reader preferences, shared by preview and article screens. */
public final class ReaderSettings {
    private final SharedPreferences preferences;
    public ReaderSettings(Context context) {
        preferences = context.getApplicationContext().getSharedPreferences("reader_settings", Context.MODE_PRIVATE);
    }
    public int getFontSize() {
        int value = preferences.getInt("font_size", 16);
        return isSupported(value) ? value : 16;
    }
    public void setFontSize(int size) {
        if (!isSupported(size)) throw new IllegalArgumentException("Unsupported font size");
        preferences.edit().putInt("font_size", size).apply();
    }
    private static boolean isSupported(int value) {
        return value == 14 || value == 16 || value == 18 || value == 22;
    }
}

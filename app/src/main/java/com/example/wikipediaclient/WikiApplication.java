package com.example.wikipediaclient;
import android.app.Application;
import com.example.wikipediaclient.data.AppContainer;
public final class WikiApplication extends Application {
    private AppContainer container;
    @Override public void onCreate() {
        super.onCreate();
        container = new AppContainer(this);
    }
    public AppContainer getContainer() { return container; }
}

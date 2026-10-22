package com.example.wikipediaclient;
import android.app.Application;
import com.example.wikipediaclient.data.AppContainer;
public class WikiApplication extends Application {
    private AppContainer container;
    @Override public void onCreate() {
        super.onCreate();
        container = createContainer();
    }
    protected AppContainer createContainer() { return new AppContainer(this); }
    public AppContainer getContainer() { return container; }
}

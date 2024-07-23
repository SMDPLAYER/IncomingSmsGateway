package tech.bogomolov.incomingsmsgateway.notification;
import android.app.Application;

public class App extends Application {
    private static App _instance;

    public static App getInstanceCurrentApp() {
        return _instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        _instance = this;
    }
}

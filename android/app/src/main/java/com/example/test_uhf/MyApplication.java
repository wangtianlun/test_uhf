package com.example.test_uhf;

import androidx.annotation.NonNull;

import io.sentry.Sentry;
import io.flutter.app.FlutterApplication;


public class MyApplication extends FlutterApplication  {
    @Override
    public void onCreate() {
        super.onCreate();
        Sentry.init(options -> {
          options.setDsn(""); // 替换为您的 DSN
        });
        Thread.setDefaultUncaughtExceptionHandler(new MyUncaughtExceptionHandler());
    }

    // 自定义异常处理程序类
    private static class MyUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {
      @Override
      public void uncaughtException(@NonNull Thread thread, @NonNull Throwable throwable) {
        // 在这里执行异常处理逻辑，例如将异常发送到 Sentry
        Sentry.captureException(throwable);
      }
    }
}




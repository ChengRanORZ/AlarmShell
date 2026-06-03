package top.weixiansen574.alarmshell.xposed;

import android.app.Activity;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;

import java.io.File;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Locale;

import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam;
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam;

public class XposedMain extends XposedModule {
    private static final String TAG = "AlarmShell";

    @Override
    public void onModuleLoaded(ModuleLoadedParam param) {
        log(Log.INFO, TAG, "Module loaded");
    }

    @Override
    public void onPackageReady(PackageReadyParam param) {
        if (!param.getPackageName().equals("com.android.deskclock")) {
            return;
        }

        ClassLoader classLoader = param.getClassLoader();
        try {
            Class<?> alarmAlertClass = classLoader.loadClass("com.android.deskclock.alarm.alert.AlarmAlertFullScreenActivity");
            hook(alarmAlertClass.getMethod("onCreate", Bundle.class)).intercept(chain -> {
                Object result = chain.proceed();

                Activity activity = (Activity) chain.getThisObject();
                Parcelable alarm = activity.getIntent().getParcelableExtra("intent.extra.alarm");
                if (alarm != null) {
                    try {
                        Field labelField = alarm.getClass().getField("label");
                        String label = (String) labelField.get(alarm);

                        File shellDir = new File("/data/user/0/com.android.deskclock/files/shell/");
                        if (!shellDir.exists()) {
                            shellDir.mkdirs();
                        }
                        if (label == null) {
                            return result;
                        }

                        File[] shFiles = shellDir.listFiles();
                        if (shFiles == null) {
                            return result;
                        }
                        for (File shFile : shFiles) {
                            String shFileName = shFile.getName();
                            if (shFileName.equals(label)) {
                                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                                String shFilePath = shFile.getAbsolutePath();
                                log(Log.INFO, TAG, sdf.format(System.currentTimeMillis()) + " 执行shell脚本:" + shFilePath);
                                Thread execThread = new ShellExecThread(shFilePath);
                                execThread.setName("AlarmShellThread");
                                execThread.start();
                                break;
                            } else {
                                log(Log.INFO, TAG, "闹钟已响，但是没有对应的sh文件：" + label);
                            }
                        }
                    } catch (Exception e) {
                        log(Log.ERROR, TAG, "Failed to get alarm label", e);
                    }
                }

                return result;
            });
        } catch (Throwable t) {
            log(Log.ERROR, TAG, "Failed to hook AlarmAlertFullScreenActivity: " + t.getMessage());
        }
    }
}

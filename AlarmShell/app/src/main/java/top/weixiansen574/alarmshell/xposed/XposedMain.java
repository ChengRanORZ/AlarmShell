package top.weixiansen574.alarmshell.xposed;

import android.app.Activity;
import android.os.Bundle;
import android.os.Parcelable;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Locale;

import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam;
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam;
import io.github.libxposed.api.hooks.Chain;
import io.github.libxposed.api.hooks.MethodHooker;

public class XposedMain extends XposedModule {

    @Override
    public void onModuleLoaded(ModuleLoadedParam param) {
        // Module loaded
    }

    @Override
    public void onPackageLoaded(PackageLoadedParam param) {
        if (!param.getPackageName().equals("com.android.deskclock")) {
            return;
        }

        ClassLoader classLoader = param.getClassLoader();
        try {
            Class<?> alarmAlertClass = classLoader.loadClass("com.android.deskclock.alarm.alert.AlarmAlertFullScreenActivity");
            hookMethod(alarmAlertClass.getMethod("onCreate", Bundle.class), new AlarmHooker());
        } catch (Throwable t) {
            log("Failed to hook AlarmAlertFullScreenActivity: " + t.getMessage());
        }
    }

    private class AlarmHooker implements MethodHooker {
        @Override
        public void after(Chain chain) throws Throwable {
            Activity activity = (Activity) chain.getThisObject();
            Parcelable alarm = activity.getIntent().getParcelableExtra("intent.extra.alarm");
            if (alarm != null) {
                String label = (String) getXposedInterface().getObjectField(alarm, "label");
                File shellDir = new File("/data/user/0/com.android.deskclock/files/shell/");
                if (!shellDir.exists()) {
                    shellDir.mkdirs();
                }
                if (label == null) {
                    return;
                }

                File[] shFiles = shellDir.listFiles();
                if (shFiles == null) {
                    return;
                }
                for (File shFile : shFiles) {
                    String shFileName = shFile.getName();
                    if (shFileName.equals(label)) {
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                        String shFilePath = shFile.getAbsolutePath();
                        log(sdf.format(System.currentTimeMillis()) + " 执行shell脚本:" + shFilePath);
                        Thread execThread = new ShellExecThread(shFilePath);
                        execThread.setName("AlarmShellThread");
                        execThread.start();
                        break;
                    } else {
                        log("闹钟已响，但是没有对应的sh文件：" + label);
                    }
                }
            }
            chain.proceed();
        }
    }
}

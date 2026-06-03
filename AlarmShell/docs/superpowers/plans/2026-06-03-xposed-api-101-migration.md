# Xposed API 101 迁移实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 AlarmShell 项目从 XposedBridge API 54 迁移到现代 Xposed API 101

**Architecture:** 使用新的 XposedModule 基类和拦截器链模型替代旧的 IXposedHookLoadPackage 接口

**Tech Stack:** Java, Xposed API 101, Android Gradle

---

### Task 1: 更新 Gradle 依赖配置

**Files:**
- Modify: `app/build.gradle:31-33`

- [ ] **Step 1: 添加 Maven 仓库**

在 `repositories` 块中添加 `mavenCentral()`:

```gradle
repositories {
    google()
    mavenCentral()
}
```

- [ ] **Step 2: 更新依赖项**

替换 Xposed API 依赖:

```gradle
dependencies {
    compileOnly 'io.github.libxposed:api:101.0.1'
}
```

- [ ] **Step 3: 验证编译**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/build.gradle
git commit -m "build: migrate to Xposed API 101 dependency"
```

---

### Task 2: 创建 Xposed 模块配置文件

**Files:**
- Create: `app/src/main/resources/META-INF/xposed/java_init.list`
- Create: `app/src/main/resources/META-INF/xposed/module.prop`
- Create: `app/src/main/resources/META-INF/xposed/scope.list`

- [ ] **Step 1: 创建 java_init.list**

```bash
mkdir -p app/src/main/resources/META-INF/xposed
```

Write file `app/src/main/resources/META-INF/xposed/java_init.list`:
```
top.weixiansen574.alarmshell.xposed.XposedMain
```

- [ ] **Step 2: 创建 module.prop**

Write file `app/src/main/resources/META-INF/xposed/module.prop`:
```properties
minApiVersion=101
targetApiVersion=101
```

- [ ] **Step 3: 创建 scope.list**

Write file `app/src/main/resources/META-INF/xposed/scope.list`:
```
com.android.deskclock
```

- [ ] **Step 4: 验证文件创建**

Run: `ls -la app/src/main/resources/META-INF/xposed/`
Expected: 3 files created

- [ ] **Step 5: Commit**

```bash
git add app/src/main/resources/META-INF/xposed/
git commit -m "config: add Xposed API 101 module configuration"
```

---

### Task 3: 重构主类为 XposedModule

**Files:**
- Modify: `app/src/main/java/top/weixiansen574/alarmshell/xposed/XposedMain.java`

- [ ] **Step 1: 更新 import 语句**

替换旧的 import 为新的 API import:

```java
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
import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api hookers.MethodHooker;
import io.github.libxposed.api.hooks.Chain;
```

- [ ] **Step 2: 修改类声明**

```java
public class XposedMain extends XposedModule {
```

- [ ] **Step 3: 实现构造函数和回调方法**

```java
    @Override
    public void onModuleLoaded(ModuleLoadedParam param) {
        // 模块加载完成
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
```

- [ ] **Step 4: 创建内部 Hooker 类**

```java
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
                        log(sdf.format(System.currentTimeMillis()) + " 正在执行shell脚本:" + shFilePath);
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
```

- [ ] **Step 5: 验证编译**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/top/weixiansen574/alarmshell/xposed/XposedMain.java
git commit -m "refactor: migrate XposedMain to Xposed API 101"
```

---

### Task 4: 清理旧配置文件

**Files:**
- Delete: `app/src/main/assets/xposed_init`
- Modify: `app/src/main/AndroidManifest.xml:15-26`

- [ ] **Step 1: 删除旧入口点文件**

```bash
rm app/src/main/assets/xposed_init
```

- [ ] **Step 2: 更新 AndroidManifest.xml**

移除 xposed 相关 metadata:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <application
        android:allowBackup="true"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        tools:targetApi="31">

    </application>

</manifest>
```

- [ ] **Step 3: 验证编译**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/assets/ app/src/main/AndroidManifest.xml
git commit -m "cleanup: remove legacy Xposed configuration"
```

---

### Task 5: 最终验证

**Files:**
- None

- [ ] **Step 1: 清理构建**

Run: `./gradlew clean`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: 完整构建**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 检查 APK 内容**

Run: `unzip -l app/build/outputs/apk/debug/app-debug.apk | grep META-INF`
Expected: 看到 META-INF/xposed/ 目录下的配置文件

- [ ] **Step 4: 最终 Commit**

```bash
git add -A
git commit -m "feat: complete Xposed API 101 migration"
```

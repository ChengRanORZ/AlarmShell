# Xposed API 101 迁移设计文档

## 概述

将 AlarmShell 项目从传统的 XposedBridge API (版本 54) 迁移到现代 Xposed API 101。

## 背景

- 当前项目使用 `XposedBridgeApi-54.jar` 和 `IXposedHookLoadPackage` 接口
- Xposed API 101 是全新设计的现代 API，采用 OkHttp 风格的拦截器链模型
- API 101 不再支持旧版 API，需要完全迁移

## 变更内容

### 1. 依赖项更新

**修改文件**: `app/build.gradle`

- 移除: `compileOnly files('libs/XposedBridgeApi-54.jar')`
- 添加: `compileOnly 'io.github.libxposed:api:101.0.1'`
- 添加 Maven 仓库: `mavenCentral()`

### 2. 入口点配置

**创建文件**: `app/src/main/resources/META-INF/xposed/java_init.list`

内容: `top.weixiansen574.alarmshell.xposed.XposedMain`

**删除文件**: `app/src/main/assets/xposed_init`

### 3. 模块元数据

**创建文件**: `app/src/main/resources/META-INF/xposed/module.prop`

```properties
minApiVersion=101
targetApiVersion=101
```

**创建文件**: `app/src/main/resources/META-INF/xposed/scope.list`

内容: `com.android.deskclock`

### 4. 主类重构

**修改文件**: `XposedMain.java`

- 从: `implements IXposedHookLoadPackage`
- 改为: `extends XposedModule`

主要变化:
- 移除 `handleLoadPackage` 方法
- 实现 `onPackageLoaded` 回调
- 使用新的 Hook API

### 5. Hook API 更新

**修改文件**: `XposedMain.java`

使用新的 `hookMethod` API 替代 `findAndHookMethod`:

```java
// 旧 API
XposedHelpers.findAndHookMethod(className, classLoader, "onCreate", Bundle.class, new XC_MethodHook() {...});

// 新 API
hookMethod(targetClass.getMethod("onCreate", Bundle.class), new MethodHooker() {
    @Override
    public void after(Chain chain) throws Throwable {
        // Hook 逻辑
    }
});
```

### 6. 清理旧文件

- 删除 `app/src/main/assets/xposed_init`
- 更新 `AndroidManifest.xml` 移除 xposed 相关 metadata:
  - `xposedmodule`
  - `xposeddescription`
  - `xposedminversion`
  - `xposedscope`

## 技术细节

### 新 API 关键类

- `io.github.libxposed.api.XposedModule`: 模块主类基类
- `io.github.libxposed.api.XposedInterface`: Xposed 接口
- `io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam`: 模块加载参数
- `io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam`: 包加载参数

### 回调生命周期

1. `onModuleLoaded()` - 模块加载时调用
2. `onPackageLoaded()` - 目标包加载时调用（默认类加载器就绪后）
3. `onPackageReady()` - 应用类加载器创建后调用

### Hook 模型

使用拦截器链模型:
- `Chain`: 拦截器链
- `MethodHooker`: Hook 处理器接口
- `intercept(Chain chain)`: 拦截方法

## 验证方法

1. 编译项目确保无语法错误
2. 安装到设备测试闹钟触发功能
3. 检查 Xposed 日志确认模块加载正常

## 风险评估

- **低风险**: 项目功能简单，只 Hook 一个方法
- **中风险**: API 变化较大，需要仔细处理回调参数
- **缓解措施**: 参考官方文档和示例代码

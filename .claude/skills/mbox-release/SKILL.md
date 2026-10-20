---
name: mbox-release
description: MBox(TVBox 二次开发版，包名 com.github.tvbox.osc)发版流水线：提交/push 代码 → Gradle 编译 release APK → 安装到 Windows WSA 并验证 FTY Guard 加密蜘蛛 → 原子上传 NAS(/volume1/Packages/tvbox_by_me.apk)。当用户说「发版 / 编译 apk / 装到 WSA / 上传 NAS / 走一遍发布流程」且项目是 D:\project\TVBoxMobileSync 时使用。
---

# MBox 发版流水线（push → 编译 → WSA 安装验证 → NAS）

项目根：`D:\project\TVBoxMobileSync`（Git Bash 下为 `/d/project/TVBoxMobileSync`）
产物：`app/build/outputs/apk/release/MBox_v<versionName>_release_<yyyyMMdd>.apk`，由仓库根 `key.jks` + `key.properties` 自动签名。

## 本机固定路径（不要重新探测）

- JDK 11（AGP 7.2.2 必需，JBR 21 不行）：`C:\Users\mayi\.jdks\jbr_dcevm-11.0.16`
- Gradle 7.4 缓存（wrapper 写的 7.3.3 本机无缓存，直接调缓存版）：
  `/c/Users/mayi/.gradle/wrapper/dists/gradle-7.4-bin/c0gwcg53nkjbqw7r0h0umtfvt/gradle-7.4/bin/gradle`
- adb：`/c/Users/mayi/AppData/Local/Android/Sdk/platform-tools/adb`
- WSA 序列号：**`127.0.0.1:58526`**（所有 adb 命令必须 `-s 127.0.0.1:58526`）
- NAS 上传脚本：`C:/Users/mayi/.ssh/rnas.py`（putroot 走 sudo tee；SFTP 禁用）
- NAS 目标：`/volume1/Packages/tvbox_by_me.apk`，HTTP 校验地址 `http://192.168.31.140:1379/tvbox_by_me.apk`

## 铁律

1. **真机 `872c0cf8` 一直连着**：install/uninstall 一律显式 `-s 127.0.0.1:58526`，绝不能裸跑 `adb install`。
2. **调用 `/mbox-release` 本身即视为当次授权 push**：步骤 0 commit 后直接 `git push origin my_main`，无需再问。流程之外的临时 push 仍需用户明确授权。HTTPS 直推失败先重试（透明代理偶发），再考虑 ssh.github.com:443 通道。
3. `app/src/main/java/com/github/tvbox/osc/ui/fragment/PlayFragment.java` 是用户自己的未提交改动（jp_url 调试），**不要替他提交**，commit 时只 add 本流程相关文件。
4. WSA 里 App 跑在 displayId=2 的 freeform 窗口：uiautomator 必须 `--display-id 2`，`input -d 2 tap`；dump 出的 bounds 是**全局屏幕坐标**（窗口有偏移），不是窗口内坐标。
5. 控制台是 GBK：脚本输出别 print emoji，必要时 `PYTHONUTF8=1` 或把输出写 UTF-8 文件再读。

## 步骤 0：提交并推送代码

```bash
cd /d/project/TVBoxMobileSync
git status --short
git log --oneline -3
```

只 stage 本次发版相关文件（典型：`app/build.gradle`、`app/libs/dlna-dmc.aar`、`app/libs/dlna-core.aar`、Guard 修复源码、本 skill），逐个 `git add <file>`，禁止 `git add -A`（工作区有大量无关未跟踪目录）。

commit message 中文描述 why，结尾加：

```
Co-Authored-By: Claude Sonnet 5 <noreply@anthropic.com>
```

个人 fork、已在 WSA 真机验证，commit 前自测门禁可豁免。commit 后直接推送（`/mbox-release` 调用即授权）：

```bash
git push origin my_main
```

## 步骤 1：编译 release APK

```bash
cd /d/project/TVBoxMobileSync
export JAVA_HOME='C:\Users\mayi\.jdks\jbr_dcevm-11.0.16'
GR='/c/Users/mayi/.gradle/wrapper/dists/gradle-7.4-bin/c0gwcg53nkjbqw7r0h0umtfvt/gradle-7.4/bin/gradle'
"$GR" :app:assembleRelease -Pandroid.builder.sdkDownload=false
```

成功后取最新产物：

```bash
ls -t app/build/outputs/apk/release/MBox_v*_release_*.apk | head -1
```

### 构建环境前提（已配好；换机器或报对应错误时对照）

- `C:\Users\mayi\.gradle\init.gradle`：阿里云镜像改写 + 针对 rootProject.name `TVBoxMobile` 的机器钉版本（buildToolsVersion 35.0.0、compileSdkVersion 33）+ Gradle 6.5 兼容的 `dependencyResolutionManagement` respondsTo 守卫。
- SDK：`platforms/android-33` 已装；`build-tools/35.0.0` 下有 dx 垫片（`dx.bat`=d8.bat 副本、`lib/dx.jar`=lib/d8.jar 副本），否则 AGP 报 "Build Tools revision 35.0.0 is corrupted"。
- JitPack `com.github.devin1014.DLNA-Cast:dlna-dmc:V1.0.0` 构件永久损坏（死 bintray 插件），`app/libs/dlna-dmc.aar`、`dlna-core.aar` 为 V1.0.0 源码本地构建后入库；`app/build.gradle` 用 flatDir + 显式 cling/jetty/logger 依赖。**这两个 aar 和 build.gradle 改动必须在仓库里**，否则别人 clone 无法构建。
- 若 Gradle 尝试下载 2.5 分钟远程 manifest 或 build-tools 30.0.3，说明 init.gradle 钉版本没生效，检查 rootProject.name 是否仍是 `TVBoxMobile`。

## 步骤 2：安装到 WSA

```bash
ADB='/c/Users/mayi/AppData/Local/Android/Sdk/platform-tools/adb'
"$ADB" connect 127.0.0.1:58526
APK=$(ls -t /d/project/TVBoxMobileSync/app/build/outputs/apk/release/MBox_v*_release_*.apk | head -1)
"$ADB" -s 127.0.0.1:58526 install -r "$APK"
```

报 `device not found` = WSA 关了：从桌面 MBox 快捷方式启动（或开始菜单搜 MBox），再 connect。

## 步骤 3：验证 Guard 加密蜘蛛（这是本 fork 的核心修复）

背景：`JarLoader.java` 给 `DexClassLoader` 第 3 参传 cacheDir 作为 native 搜索路径，修 WSA houdini 下 `UnsatisfiedLinkError: failed to create bridged namespace ... search_paths:`。

```bash
"$ADB" -s 127.0.0.1:58526 shell am force-stop com.github.tvbox.osc
"$ADB" -s 127.0.0.1:58526 logcat -c
"$ADB" -s 127.0.0.1:58526 shell monkey -p com.github.tvbox.osc 1 >/dev/null
sleep 20
"$ADB" -s 127.0.0.1:58526 logcat -d | grep -c "bridged namespace"   # 必须为 0
```

UI 验证（瓜子 Guard 源出片）：

```bash
"$ADB" -s 127.0.0.1:58526 shell uiautomator dump --display-id 2 /sdcard/ui.xml >/dev/null
"$ADB" -s 127.0.0.1:58526 pull /sdcard/ui.xml /tmp/mbox_ui.xml >/dev/null
grep -oE "瓜子[^\"<]*|电影|电视剧|动漫|综艺" /tmp/mbox_ui.xml | sort -u
```

看到「瓜子┃不卡」分类与真实片名即通过；点播放可进一步确认 `logcat -d | grep -E "AudioTrack.*state:started"` 非空。点击坐标用 dump bounds 的全局坐标：`input -d 2 tap <x> <y>`。

## 步骤 4：原子上传 NAS

```bash
export MSYS2_ARG_CONV_EXCL='*' MSYS_NO_PATHCONV=1   # 防止 MSYS 篡改路径/参数
cd /d/project/TVBoxMobileSync
APK=$(ls -t app/build/outputs/apk/release/MBox_v*_release_*.apk | head -1)
LOCAL_MD5=$(md5sum "$APK" | cut -d' ' -f1)
python C:/Users/mayi/.ssh/rnas.py putroot "$(cygpath -w "$APK")" /volume1/Packages/tvbox_by_me.apk.new
python C:/Users/mayi/.ssh/rnas.py run "echo $LOCAL_MD5  expected; md5sum /volume1/Packages/tvbox_by_me.apk.new && mv -f /volume1/Packages/tvbox_by_me.apk.new /volume1/Packages/tvbox_by_me.apk && ls -la /volume1/Packages/tvbox_by_me.apk" --root
```

远端 md5 与本地一致才算成功（不一致绝不 mv）。HTTP 复核：

```bash
curl -sI --max-time 20 "http://192.168.31.140:1379/tvbox_by_me.apk" | grep -iE "HTTP|Content-Length|Last-Modified"
```

Content-Length 须等于本地 APK 字节数。

## 失败回滚

NAS 历史备份：`/volume1/Packages/tvbox_by_me-20241119.apk.bak`（更早版本）。若新版有问题，`rnas.py run "cp <备份> /volume1/Packages/tvbox_by_me.apk" --root` 回滚；WSA 端 `install -r` 旧 APK 即可（同签名）。

## 速查：Guard 修复点

- 源码：`app/src/main/java/com/github/catvod/crawler/JarLoader.java` — DexClassLoader 第 3 参由 null 改为 `App.getInstance().getCacheDir().getAbsolutePath()`。
- 纯 smali 急救路线（无构建环境时）：baksmali 2.5.2（bitbucket 下载，GitHub release 无资产）拆 classes.dex，`JarLoader.smali` 中 DexClassLoader `<init>` 前把 `const/4 v4, 0x0` 改 `move-object v4, v1`，zip 替换 dex → 去 META-INF → zipalign → apksigner（JAVA_HOME 指 JBR）。

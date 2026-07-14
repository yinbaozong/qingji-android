# 技术状态 / Technical Status

Last reviewed: 2026-07-15

## What is stable now

| Area | Current baseline |
| --- | --- |
| Language / runtime | Kotlin 1.9.24, JDK 17 |
| Android build | Android Gradle Plugin 8.5.2, Gradle 8.9 |
| UI | Jetpack Compose, Compose BOM 2024.09.00 |
| App identity | `com.dreamjournal.app` |
| Verification | GitHub Actions runs unit tests, Android Lint, and `assembleDebug` on every push to `main` |

These versions are not presented as the newest Android toolchain. They are the tested baseline for the current release line. The project is a local-first Android app and can be built with JDK 17 using the included Gradle wrapper.

这些版本不是“2026 年最新”，而是当前发布线已经验证过的稳定基线。项目使用仓库内的 Gradle Wrapper，在 JDK 17 下构建；每次推送到 `main` 都会执行单元测试、Android Lint 与 Debug APK 构建。

## Why the package still says `dreamjournal`

The product is called **瞬记 / QingJi**. The original package was created when the app focused on dream notes. On Android, `applicationId` is more than a label: it is the app identity used for installation, local app storage, permissions, and upgrades.

Renaming it from `com.dreamjournal.app` would make a new application identity. A user installing that build would not receive it as an update and would not automatically see existing Room data, recordings, photos, or DataStore settings. For that reason, the name is intentionally retained until a real migration plan exists.

产品名称已经是 **瞬记 / QingJi**；`com.dreamjournal.app` 是早期定位留下的内部应用 ID。Android 会用它识别安装包、私有存储、权限和更新关系。直接改名会被系统认作一个新 App，原有 Room 数据、录音、图片和 DataStore 设置都不会自动迁移。因此，在有完整迁移方案前，这个内部名称会保持不变。

## Upgrade policy

Dependency upgrades are deliberately not bundled with product fixes. The Android Gradle Plugin, Kotlin, KSP, Compose compiler, and Compose BOM move together enough that a casual one-line bump can create build or runtime regressions.

When the project upgrades its toolchain, the work should happen in a dedicated pull request with these checks:

1. Upgrade a compatible set of Gradle, AGP, Kotlin, KSP, and Compose dependencies.
2. Build and test on a clean JDK 17 environment.
3. Run Android Lint and all unit tests.
4. Open the app on at least one physical device or emulator and verify recording, transcription, exports, image insertion, calendar navigation, and provider configuration.
5. Publish only after an upgrade install from the current released APK preserves local data and settings.

依赖升级会单独进行，不和功能修复混在一起。Gradle、AGP、Kotlin、KSP、Compose 编译器和 BOM 之间存在兼容关系，不能只为了“版本最新”而随手升级。升级 PR 至少需要完成上面的构建、功能和覆盖安装验证。

## Package migration policy

If a future release must move to a QingJi-specific application ID, it needs to be treated as a migration project rather than a rename:

1. Keep the existing package available during the transition.
2. Offer an explicit encrypted or user-visible export/import path for entries, audio, photos, to-dos, tags, and settings.
3. Document how users move their data and how long the old package remains supported.
4. Test migration with real-world data before announcing a new package.

Until then, the internal package name is a compatibility choice, not a public branding decision.

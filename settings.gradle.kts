pluginManagement {
    repositories {
        // ---- 清华 Gradle 插件镜像 ----
        maven { url = uri("https://mirrors.tuna.tsinghua.edu.cn/gradle/plugins/") }
        // ---- 腾讯云镜像（备用） ----
        maven { url = uri("https://mirrors.cloud.tencent.com/gradle/plugins/") }

        // ---- 官方源（兜底） ----
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // ---- Android / Google Maven 清华镜像 ----
        maven { url = uri("https://mirrors.tuna.tsinghua.edu.cn/Android/") }
        // ---- 腾讯云 Android 镜像（备用） ----
        maven { url = uri("https://mirrors.cloud.tencent.com/Android/") }

        // ---- 官方源（兜底） ----
        google()
        mavenCentral()
    }
}

rootProject.name = "game240"
include(":app")

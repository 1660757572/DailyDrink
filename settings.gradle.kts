pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // 金标联盟 SharedView Kit 只发布在联盟自己的 Nexus（内测阶段仍是 HTTP），限定分组避免影响其他依赖解析。
        maven {
            url = uri("http://nexus.itgsa.com:5566/repository/sharedViewKit/")
            isAllowInsecureProtocol = true
            content { includeGroup("gsai.sdk") }
        }
    }
}

rootProject.name = "AquaDay"
include(":app")

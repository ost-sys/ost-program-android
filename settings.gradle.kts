pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://maven.pkg.github.com/tribalfs/sesl-androidx")
            credentials {
                username = "ost-sys"
                password = "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"
            }
        }
        maven {
            url = uri("https://maven.pkg.github.com/tribalfs/sesl-material-components-android")
            credentials {
                username = "ost-sys"
                password = "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"
            }
        }
        maven { url = uri("https://jitpack.io")}
    }
}

include(":lib")

rootProject.name = "My Application"
include(":app")
 
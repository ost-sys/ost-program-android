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
    }
}

include(":lib")

rootProject.name = "My Application"
include(":app")
 
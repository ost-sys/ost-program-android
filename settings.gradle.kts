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
 
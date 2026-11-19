rootProject.name = "Alakastats"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        mavenLocal()
    }
}

include(":composeApp")
include(":androidApp")

// -PisolatedBuild: builds in build-isolated/ instead of build/, to run a build (e.g. the tests) next to a continuous one
// (e.g. the web dev server) without both writing the same files. To use with its own project cache:
// ./gradlew -PisolatedBuild --project-cache-dir .gradle-isolated <tasks>
if (providers.gradleProperty("isolatedBuild").isPresent) {
    gradle.beforeProject {
        layout.buildDirectory.set(layout.projectDirectory.dir("build-isolated"))
    }
}
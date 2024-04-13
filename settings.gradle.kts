import java.net.URI

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        //maven { url = URI("https://jitpack.io") }
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        //maven { url = URI("https://jitpack.io") }
        /*maven{
            url = uri("https://chaquo.com/maven")
        }*/

    }
}

rootProject.name = "FutureCampus"
include(":app")

include(":OpenCV490")

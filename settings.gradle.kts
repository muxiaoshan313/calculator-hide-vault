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

        flatDir { dirs("app/libs")}

        maven {
            credentials {
                username = "oCTwYT"
                password = "D0QXlbW7Pb"
            }
            url = uri("https://packages.aliyun.com/maven/repository/2366325-release-BS0F4j/")
        }

        maven {
            url = uri("https://jfrog.anythinktech.com/artifactory/overseas_sdk")
        }

        maven {
            url = uri("https://android-sdk.is.com/")
        }

        // pangle
        maven {
            url = uri("https://artifact.bytedance.com/repository/pangle")
        }

        // openmediation
        maven {
            url = uri("https://dl.openmediation.com/omcenter/")
        }

        // mintegral
        maven {
            url = uri("https://dl-maven-android.mintegral.com/repository/mbridge_android_sdk_oversea")
        }

        // Anythink(Core)
        maven {
            url = uri("https://anythink.jfrog.io/artifactory/android_sdk")
        }

        // Vungle
        maven {
            url = uri("https://s01.oss.sonatype.org/content/groups/staging/")
        }

        // Chartboost
        maven {
            url = uri("https://cboost.jfrog.io/artifactory/helium")
        }

        // Tapjoy
        maven {
            url = uri("https://sdk.tapjoy.com/")
        }

        maven {
            url = uri("https://developer.huawei.com/repo/")
        }

        maven {
            url = uri("https://maven.singular.net/")
        }

        // Chartboost 三星要加
        maven {
            url = uri("https://cboost.jfrog.io/artifactory/chartboost-ads")
        }
        maven {
            url = uri("https://cboost.jfrog.io/artifactory/chartboost-mediation")
        }

        maven {
            url = uri("https://verve.jfrog.io/artifactory/verve-gradle-release")
        }

        maven {
            url = uri("https://artifactory.bidmachine.io/bidmachine")
        }

        maven {
            url = uri("https://bitbucket.org/sdkcenter/sdkcenter/raw/release")
        }

    }
}

rootProject.name = "Private Space"
include(":app")
 
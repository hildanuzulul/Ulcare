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
        // (opsional) JitPack jarang dipakai untuk plugin, jadi boleh TIDAK ditambah di sini
         maven(url = "https://jitpack.io")
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // ⬇️ WAJIB untuk library ImagePicker
        maven(url = "https://jitpack.io")
    }
}

rootProject.name = "ULCARE"
include(":app")

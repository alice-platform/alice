val sub = arrayOf(
    "api",
    "module:discord",
    "module:twitch",
    "plugin:base",
    "plugin:gradle",
    "plugin:maven",
)

rootProject.name = "alice"

include(sub.map { ":$it" })

sub.forEach {
    project(":$it").apply {
        projectDir = rootDir.resolve(it.split(":").joinToString(File.separator))
        name = it.split(":").reversed().joinToString("-", prefix = rootProject.name + "-")
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
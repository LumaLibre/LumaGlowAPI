import org.apache.tools.ant.filters.ReplaceTokens

plugins {
    id("java")
    id("maven-publish")
    id("io.freefair.lombok") version "9.2.0"
    id("com.gradleup.shadow") version "9.3.1"
    id("xyz.jpenilla.run-paper") version "3.0.1"
}

group = "dev.lumas.glowapi"
version = commitHash()

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://repo.jsinco.dev/releases")
    maven("https://repo.okaeri.cloud/releases")
}

dependencies {
    implementation("net.megavex:scoreboard-library-api:2.8.1")
    runtimeOnly("net.megavex:scoreboard-library-implementation:2.8.1")
    implementation("eu.okaeri:okaeri-configs-yaml-snakeyaml:6.1.0-beta.1")
    // Resource pack generation for shader glow effects (dev.lumas.glowapi.pack.GlowPack)
    implementation("team.unnamed:creative-api:1.7.3")
    implementation("team.unnamed:creative-serializer-minecraft:1.7.3")


    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    compileOnly("me.clip:placeholderapi:2.11.6")
    compileOnly("dev.lumas.core:LumaCore:0383263")
    implementation("org.jetbrains:annotations:24.0.0")
}


java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
    withSourcesJar()
}

val startupTest = sourceSets.create("startupTest") {
    java.srcDir("src/test/startup")
    compileClasspath += sourceSets.main.get().output + configurations.compileClasspath.get()
    runtimeClasspath += output + compileClasspath
}

val startupRegression = tasks.register<JavaExec>("startupRegression") {
    dependsOn(tasks.named(startupTest.classesTaskName))
    classpath = startupTest.runtimeClasspath
    mainClass.set("dev.lumas.glowapi.StartupRegression")
    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(25))
    })
}

tasks.check {
    dependsOn(startupRegression)
}

tasks {
    processResources {
        inputs.property("version", project.version.toString())
        filteringCharset = "UTF-8"
        filter<ReplaceTokens>(
            "tokens" to mapOf("version" to project.version.toString()),
            "beginToken" to "\${",
            "endToken" to "}"
        )
    }

    shadowJar {
        val pack = "dev.lumas.glowapi.libs"
        relocate("net.megavex.scoreboardlibrary", "$pack.scoreboardlibrary")
        relocate("eu.okaeri", "$pack.okaeri.configs")
        relocate("team.unnamed.creative", "$pack.creative")

        archiveClassifier.set("")
        archiveVersion.set("")
        exclude("yaml/snakeyaml/**", "intellij/lang/annotations/**", "org/jetbrains/annotations/**")
    }

    jar {
        enabled = false
    }

    build {
        dependsOn(shadowJar)
    }

    runServer {
        minecraftVersion("26.2")
        systemProperty("com.mojang.eula.agree", true)
    }
}

publishing {
    repositories {
        maven {
            name = "jsinco-repo"
            url = uri("https://repo.jsinco.dev/releases")
            credentials(PasswordCredentials::class) {
                username = System.getenv("REPO_USERNAME")
                password = System.getenv("REPO_PASSWORD")
            }
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }
    publications {
        create<MavenPublication>("maven") {
            artifact(tasks.shadowJar)
            artifact(tasks.named("sourcesJar"))
            groupId = project.group.toString()
            artifactId = project.name
            version = project.version.toString()
        }
    }
}

fun commitHash(): String = runCatching {
    providers.exec {
        commandLine("git", "log", "-1", "--format=%h")
        isIgnoreExitValue = true
    }.standardOutput.asText.get().trim()
}.getOrDefault("").ifEmpty { "none" }

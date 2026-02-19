import org.apache.tools.ant.filters.ReplaceTokens
import java.io.ByteArrayOutputStream
import java.nio.charset.Charset


plugins {
    id("java")
    id("maven-publish")
    id("io.freefair.lombok") version "9.2.0"
    id("com.gradleup.shadow") version "9.3.1"
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
    val scoreboardLibraryVersion = "2.5.0"
    implementation("net.megavex:scoreboard-library-api:$scoreboardLibraryVersion")
    implementation("net.megavex:scoreboard-library-implementation:$scoreboardLibraryVersion")
    implementation("net.megavex:scoreboard-library-modern:$scoreboardLibraryVersion")
    implementation("eu.okaeri:okaeri-configs-yaml-snakeyaml:6.1.0-beta.1")


    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    compileOnly("me.clip:placeholderapi:2.11.6")
    compileOnly("dev.lumas.lumacore:LumaCore:d774bc6")
    implementation("org.jetbrains:annotations:24.0.0")
}


java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}

tasks {
    processResources {
        outputs.upToDateWhen { false }
        filter<ReplaceTokens>(mapOf(
            "tokens" to mapOf("version" to project.version.toString()),
            "beginToken" to "\${",
            "endToken" to "}"
        )).filteringCharset = "UTF-8"
    }

    shadowJar {
        val pack = "dev.lumas.glowapi.libs"
        relocate("net.megavex.scoreboardlibrary", "$pack.scoreboardlibrary")
        relocate("eu.okaeri", "$pack.okaeri.configs")

        archiveClassifier.set("")
        archiveVersion.set("")
    }

    jar {
        enabled = false
    }

    build {
        dependsOn(shadowJar)
    }
}

java {
    withSourcesJar()
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
            from(components["java"])
            groupId = project.group.toString()
            artifactId = project.name
            version = project.version.toString()
        }
    }
}

fun commitHash(): String = ByteArrayOutputStream().use { stream ->
    var branch = "none"
    try {
        project.exec {
            commandLine = listOf("git", "log", "-1", "--format=%h")
            standardOutput = stream
        }
    } catch (_: Exception) {
        return branch
    }

    if (stream.size() > 0) branch = stream.toString(Charset.defaultCharset().name()).trim()
    return branch
}
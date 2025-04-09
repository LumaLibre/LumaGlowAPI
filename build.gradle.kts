import org.apache.tools.ant.filters.ReplaceTokens
import java.io.ByteArrayOutputStream
import java.nio.charset.Charset


plugins {
    id("java")
    id("maven-publish")
}

group = "dev.jsinco.lumaglowapi"
version = getGitCommitHashShort()

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://oss.sonatype.org/content/groups/public/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://repo.jsinco.dev/releases")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    compileOnly("me.clip:placeholderapi:2.11.6")
    compileOnly("dev.jsinco.luma.lumacore:LumaCore:17f172b")
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
}

publishing {
    repositories {
        maven {
            name = "jsinco-repo"
            url = uri("https://repo.jsinco.dev/releases")
            credentials(PasswordCredentials::class) {
                // get from environment
                username = System.getenv("repo_username")
                password = System.getenv("repo_secret")
            }
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }
    publications {
        create<MavenPublication>("maven") {
            groupId = project.group.toString()
            artifactId = project.name
            version = project.version.toString()
            artifact(tasks.jar.get().archiveFile) {
                builtBy(tasks.jar)
            }
        }
    }
}

fun getGitCommitHashShort(): String = ByteArrayOutputStream().use { stream ->
    var branch = "none"
    try {
        project.exec {
            commandLine = listOf("git", "log", "-1", "--format=%h")
            standardOutput = stream
        }
    } catch (e: Exception) {
        println("Failed to get git commit hash! (No git repository found?)")
        return branch
    }

    if (stream.size() > 0) branch = stream.toString(Charset.defaultCharset().name()).trim()
    return branch
}
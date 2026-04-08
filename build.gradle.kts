plugins {
    kotlin("jvm") version "2.3.20-Beta1"
    id("com.gradleup.shadow") version "8.3.0"
    id("xyz.jpenilla.run-paper") version "2.3.1"
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.19"
}

group = "me.thatonedevil"
version = "1.0"

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc-repo"
    }
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/"){
        name = "placeholderapi-repo"
    }
}

dependencies {
    implementation(kotlin("reflect"))
    paperweight.paperDevBundle("1.21.11-R0.1-SNAPSHOT")
    compileOnly("me.clip:placeholderapi:2.11.7")
    compileOnly("me.thatonedevil:DevilLib:1.0")

    val cloudVersion = "2.0.0"
    compileOnly("org.incendo:cloud-core:$cloudVersion")
    compileOnly("org.incendo:cloud-annotations:$cloudVersion")
    compileOnly("org.incendo:cloud-kotlin-coroutines:$cloudVersion")
    compileOnly("org.incendo:cloud-kotlin-coroutines-annotations:$cloudVersion")
    compileOnly("org.incendo:cloud-paper:2.0.0-beta.14")
}

tasks {
    runServer {
        minecraftVersion("1.21.11")
        downloadPlugins {
            modrinth("placeholderapi", "2.12.2")
            modrinth("plugmanx", "3.0.3")
        }

    }
}

val targetJavaVersion = 25
kotlin {
    jvmToolchain(targetJavaVersion)
}

tasks.build {
    dependsOn("shadowJar")
}

tasks.processResources {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
}

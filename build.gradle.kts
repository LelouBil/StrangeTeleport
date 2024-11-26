
plugins {
    `java-library`
    eclipse
    idea
    `maven-publish`
    id("net.neoforged.gradle.userdev") version "7.0.171"
    id("org.jetbrains.kotlin.jvm") version "2.0.21"
}

version = project.property("mod_version") as String
group = project.property("mod_group_id") as String

repositories {
    mavenLocal()
    maven {
        name = "Kotlin for Forge"
        url = uri("https://thedarkcolour.github.io/KotlinForForge/")
        content {
            includeGroup("thedarkcolour")
        }
    }
    mavenCentral()
}

base {
    archivesName.set(project.property("mod_id") as String)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

kotlin {
    jvmToolchain(21)
}

subsystems {
    parchment {
        mappingsVersion.set(project.property("parchment_mappings_version") as String)
        minecraftVersion.set(project.property("parchment_minecraft_version") as String)
    }
}

val libraries: Configuration by configurations.creating

configurations {
    implementation {
        extendsFrom(libraries)
    }



}

runs {
    create("client") {
        systemProperty("neoforge.enabledGameTestNamespaces", project.property("mod_id") as String)
    }

    create("server") {
        arguments("-XX:+AllowEnhancedClassRedefinition", "--nogui")
        systemProperty("neoforge.enabledGameTestNamespaces", project.property("mod_id") as String)
    }

    create("gameTestServer") {
        systemProperty("neoforge.enabledGameTestNamespaces", project.property("mod_id") as String)
    }

    create("data") {
        arguments.addAll(
            "--mod", project.property("mod_id") as String,
            "--all",
            "--output", file("src/generated/resources/").absolutePath,
            "--existing", file("src/main/resources/").absolutePath
        )
    }

    configureEach {
        systemProperty("forge.logging.markers", "REGISTRIES")
//        logLevel = org.slf4j.event.Level.DEBUG
        dependencies {
            runtime(libraries)
            runtime("io.arrow-kt:arrow-core:1.2.4")
        }
    }
}






sourceSets {
    main {
        resources {
            srcDir("src/generated/resources")
        }
    }
}



jarJar.enable()

dependencies {
    implementation("thedarkcolour:kotlinforforge-neoforge:5.6.0:slim")
    libraries("org.jetbrains.kotlin","kotlin-stdlib","2.0.21")
    libraries("org.jetbrains.kotlin","kotlin-reflect","2.0.21")
    libraries("org.jetbrains.kotlinx","kotlinx-coroutines-core","1.9.0")
    jarJar("io.arrow-kt:arrow-core:1.2.4")
    libraries("io.arrow-kt:arrow-core:1.2.4")
    libraries("org.jetbrains:annotations:26.0.0")
    libraries("org.reflections:reflections:0.10.2")
    implementation("net.neoforged:neoforge:${project.property("neo_version")}")
}

tasks.withType<ProcessResources> {
    val replaceProperties = mapOf(
        "minecraft_version" to project.property("minecraft_version"),
        "minecraft_version_range" to project.property("minecraft_version_range"),
        "neo_version" to project.property("neo_version"),
        "neo_version_range" to project.property("neo_version_range"),
        "loader_version_range" to project.property("loader_version_range"),
        "mod_id" to project.property("mod_id"),
        "mod_name" to project.property("mod_name"),
        "mod_license" to project.property("mod_license"),
        "mod_version" to project.property("mod_version"),
        "mod_authors" to project.property("mod_authors"),
        "mod_description" to project.property("mod_description")
    )
    inputs.properties(replaceProperties)

    filesMatching("META-INF/neoforge.mods.toml") {
        expand(replaceProperties)
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
    repositories {
        maven {
            url = uri("file://${project.projectDir}/repo")
        }
    }
}

idea {
    module {
        isDownloadSources = true
        isDownloadJavadoc = true
    }
}

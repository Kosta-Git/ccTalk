plugins {
  kotlin("jvm") version "2.1.0"
  `maven-publish`
}

group = "cctalk"
version = "0.1.0"

repositories {
  mavenCentral()
}

dependencies {
  implementation("com.fazecast:jSerialComm:2.10.4")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
  implementation("io.arrow-kt:arrow-core:2.0.0")

  testImplementation(kotlin("test"))
  testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
  testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
  testImplementation("io.mockk:mockk:1.13.8")
}

tasks.test {
  useJUnitPlatform()
}

kotlin {
  jvmToolchain(21)
}

publishing {
  repositories {
    maven {
      name = "GitHubPackages"
      url = uri("https://maven.pkg.github.com/${System.getenv("GITHUB_REPOSITORY") ?: "Kosta-Git/ccTalk"}")
      credentials {
        username = System.getenv("GITHUB_ACTOR") ?: System.getenv("GITHUB_USERNAME")
        password = System.getenv("GITHUB_TOKEN")
      }
    }
  }
  publications {
    register<MavenPublication>("gpr") {
      from(components["java"])
      pom {
        name.set("ccTalk")
        description.set("ccTalk implementation for CCT910 in Kotlin")
        url.set("https://github.com/Kosta-Git/ccTalk")
      }
    }
  }
}
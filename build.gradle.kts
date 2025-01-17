plugins {
  kotlin("jvm") version "2.0.21"
}

group = "be.inotek"
version = "1.0-SNAPSHOT"

repositories {
  mavenCentral()
}

dependencies {
  implementation("com.fazecast:jSerialComm:2.10.4")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
  implementation("io.arrow-kt:arrow-core:1.2.4")

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
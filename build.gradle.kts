plugins {
    java
    application
    kotlin("jvm") version "2.1.10"
}

application.mainClass = "MainKt"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:+")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

sourceSets.main {
    kotlin.srcDir("src/main/kotlin")
}

java {
    sourceCompatibility = JavaVersion.VERSION_23
    targetCompatibility = JavaVersion.VERSION_23
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}


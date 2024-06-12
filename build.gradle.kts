buildscript {
    repositories {
        gradlePluginPortal()
        maven("https://packages.confluent.io/maven/")
    }
}

plugins {
    id("java")
    id("application")
}

repositories {
    mavenCentral()
    maven("https://packages.confluent.io/maven/")
}

application {
    mainClass.set("io.confluent.devx.Main")
}

java.sourceCompatibility = JavaVersion.VERSION_21
java.targetCompatibility = JavaVersion.VERSION_21

group = "io.confluent.devx"
version = "1.0-SNAPSHOT"

val junitJupiterVersion = "5.10.2"
val jqwikVersion = "1.8.4"

val jacksonVersion = "2.17.1"

val lombokVersion = "1.18.32"

tasks.compileTestJava {
    options.compilerArgs.add("-parameters")
}

dependencies {
    implementation("org.slf4j:slf4j-api:2.0.11")
    implementation("org.slf4j:slf4j-simple:2.0.11")
    implementation("ch.qos.logback:logback-core:1.4.14")

    implementation("org.apache.kafka:kafka-streams:3.6.0")
    implementation("org.apache.kafka:kafka-clients") {
        version {
            strictly("3.6.0")
        }
    }

    implementation("com.fasterxml.jackson.core:jackson-core:${jacksonVersion}")
    implementation("com.fasterxml.jackson.core:jackson-databind:${jacksonVersion}")
    implementation("com.fasterxml.jackson.core:jackson-annotations:${jacksonVersion}")

    implementation("org.projectlombok:lombok:${lombokVersion}")
    annotationProcessor("org.projectlombok:lombok:${lombokVersion}")

    testCompileOnly("org.projectlombok:lombok:${lombokVersion}")
    testAnnotationProcessor("org.projectlombok:lombok:${lombokVersion}")

    implementation("io.confluent:kafka-schema-rules:7.6.0")

    testImplementation("org.apache.kafka:kafka-streams-test-utils:3.6.0")
    // aggregate jqwik dependency
    testImplementation("net.jqwik:jqwik:$jqwikVersion")
    // Add if you also want to use the Jupiter engine or Assertions from it
    testImplementation("org.junit.jupiter:junit-jupiter:$junitJupiterVersion")
    // Add any other test library you need...
    testImplementation("org.assertj:assertj-core:3.23.1")
    testImplementation("org.mockito:mockito-core:3.+")
    // Optional but recommended to get annotation related API warnings
    compileOnly("org.jetbrains:annotations:23.0.0")
}

tasks.test {
    useJUnitPlatform()
}

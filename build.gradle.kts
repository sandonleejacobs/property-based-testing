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
val kafkaStreamsVersion = "3.7.0"
val logbackVersion = "1.4.14"
val lombokVersion = "1.18.32"
val slf4jVersion = "2.0.11"

tasks.compileTestJava {
    options.compilerArgs.add("-parameters")
}

dependencies {
    implementation("org.slf4j:slf4j-api:${slf4jVersion}")
    implementation("org.slf4j:slf4j-simple:${slf4jVersion}")
    implementation("ch.qos.logback:logback-core:${logbackVersion}")

    implementation("org.apache.kafka:kafka-streams:${kafkaStreamsVersion}")
    implementation("org.apache.kafka:kafka-clients") {
        version {
            strictly(kafkaStreamsVersion)
        }
    }

    implementation("com.fasterxml.jackson.core:jackson-core:${jacksonVersion}")
    implementation("com.fasterxml.jackson.core:jackson-databind:${jacksonVersion}")
    implementation("com.fasterxml.jackson.core:jackson-annotations:${jacksonVersion}")

    implementation("org.projectlombok:lombok:${lombokVersion}")
    annotationProcessor("org.projectlombok:lombok:${lombokVersion}")

    testCompileOnly("org.projectlombok:lombok:${lombokVersion}")
    testAnnotationProcessor("org.projectlombok:lombok:${lombokVersion}")

    testImplementation("org.apache.kafka:kafka-streams-test-utils:${kafkaStreamsVersion}")
    // aggregate jqwik dependency
    testImplementation("net.jqwik:jqwik:$jqwikVersion")
    // Add if you also want to use the Jupiter engine or Assertions from it
    testImplementation("org.junit.jupiter:junit-jupiter:$junitJupiterVersion")

}

tasks.test {
    useJUnitPlatform()
}

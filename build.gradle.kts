import com.google.protobuf.gradle.id

buildscript {
    repositories {
        gradlePluginPortal()
        maven("https://packages.confluent.io/maven/")
        maven("https://jitpack.io")
    }
}

plugins {
    id("java")
    id("application")
    id("com.google.protobuf") version "0.9.4"
    id("com.github.imflog.kafka-schema-registry-gradle-plugin") version "2.1.0"
}

repositories {
    mavenCentral()
    maven("https://packages.confluent.io/maven/")
    maven("https://jitpack.io")
}

application {
    mainClass.set("io.confluent.devx.Main")
}

java.sourceCompatibility = JavaVersion.VERSION_17
java.targetCompatibility = JavaVersion.VERSION_17

group = "io.confluent.devx"
version = "1.0-SNAPSHOT"

val junitJupiterVersion = "5.10.2"
val jqwikVersion = "1.8.4"

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
    implementation("io.grpc:grpc-stub:1.64.0")
    implementation("io.grpc:grpc-protobuf:1.64.0")
    implementation("com.google.protobuf:protobuf-java:3.22.2")
    implementation("io.confluent:kafka-protobuf-provider:7.6.0")
    implementation("io.confluent:kafka-protobuf-serializer:7.6.0")
    implementation("io.confluent:kafka-streams-protobuf-serde:7.6.0")

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


//sourceSets {
//    main {
//        java.srcDirs("src/main/java", "build/generated/source/proto/main/java")
//        proto {
//            srcDir("src/main/proto")
//        }
//    }
//}
val protocVersion = "3.14.0"
val protocGenRpcVersion = "1.15.1"

protobuf {
    protoc {
        if (osdetector.os == "osx") {
            artifact = "com.google.protobuf:protoc:$protocVersion:osx-x86_64"
        } else {
            artifact = "com.google.protobuf:protoc:$protocVersion"
        }
    }
    plugins {
        // Optional: an artifact spec for a protoc plugin, with "grpc" as
        // the identifier, which can be referred to in the "plugins"
        // container of the "generateProtoTasks" closure.
        id("grpc") {
            if (osdetector.os == "osx") {
                artifact = "io.grpc:protoc-gen-grpc-java:$protocGenRpcVersion:osx-x86_64"
            } else {
                artifact = "io.grpc:protoc-gen-grpc-java:$protocGenRpcVersion"
            }
        }
    }
    generateProtoTasks {
        ofSourceSet("main").forEach {
            it.plugins {
                // Apply the "grpc" plugin whose spec is defined above, without
                // options. Note the braces cannot be omitted, otherwise the
                // plugin will not be added. This is because of the implicit way
                // NamedDomainObjectContainer binds the methods.
                id("grpc") { }
            }
        }
    }
}

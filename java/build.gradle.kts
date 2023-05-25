import net.ltgt.gradle.errorprone.errorprone

plugins {
    java
    // id("java-library")
    id("net.ltgt.errorprone") version "3.1.0"
    id("com.diffplug.spotless") version "6.18.0"
    id("io.freefair.lombok") version "8.0.1"
}

repositories {
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
    // toolchain {
    //   languageVersion.set(JavaLanguageVersion.of(17))
    // }
}

spotless {
    java {
        importOrder()
        removeUnusedImports()

        cleanthat()
        googleJavaFormat()
        formatAnnotations()
        trimTrailingWhitespace()
    }
}

tasks {
    withType<JavaCompile> {
        // Work around a Lombok/ErrorProne bug:
        // https://github.com/projectlombok/lombok/issues/2730
        options.errorprone.disable("MissingSummary")
    }

    compileTestJava {
        options.compilerArgs.add("-Xlint:deprecation")
    }
}

dependencies {
    implementation("org.jetbrains:annotations:23.0.0")
    // implementation("org.projectlombok:lombok:1.18.22")
    errorprone("com.google.errorprone:error_prone_core:2.18.0")

    implementation("org.apache.commons:commons-math3:3.6.1")

    implementation("org.glassfish:javax.json:1.1.4")

    // Guava.
    implementation("com.google.guava:guava:31.1-jre")

    // Logging.
    implementation("org.slf4j:slf4j-simple:2.0.7")

    // Test frameworks.
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.assertj:assertj-core:3.24.2")
    testImplementation("org.mockito:mockito-core:5.3.1")

    implementation("com.fasterxml.jackson.core:jackson-core:2.15.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.0")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.15.0")

}


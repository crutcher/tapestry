import net.ltgt.gradle.errorprone.errorprone

plugins {
    java

    id("net.ltgt.errorprone") version "3.1.0"
    id("com.diffplug.spotless") version "6.18.0"
    id("io.freefair.lombok") version "8.0.1"
    id("me.champeau.jmh") version "0.7.1"


    jacoco
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
        options.errorprone.disableWarningsInGeneratedCode.set(true)

        options.compilerArgs.add("-Xlint:deprecation")
        options.compilerArgs.add("-Xlint:unchecked")
    }

    compileJava {
        options.compilerArgs.add("-Xlint:deprecation")
        options.compilerArgs.add("-Xlint:unchecked")
    }

    compileTestJava {
        options.compilerArgs.add("-Xlint:deprecation")
        options.compilerArgs.add("-Xlint:unchecked")
    }
}

tasks.test {
    finalizedBy(tasks.jacocoTestReport)
}
tasks.jacocoTestReport {
    dependsOn(tasks.test)
}

jmh {
    // See: https://github.com/melix/jmh-gradle-plugin

    // This dramatically reduces the value of these benchmarks, but it's
    // a nice fast iteration for now. We can remove the nerfing later,
    // and run stronger benchmarks.
    fork.set(1)
    iterations.set(1)
    timeOnIteration.set("2s")
    warmupIterations.set(1)
    warmupMode.set("BULK")
}

dependencies {
    implementation("javax.annotation:javax.annotation-api:1.3.2")

    implementation("guru.nidi:graphviz-java-all-j2v8:0.18.1")

    testImplementation("org.openjdk.jmh:jmh-core:1.36")
    testImplementation("org.openjdk.jmh:jmh-generator-annprocess:1.36")

    implementation("org.jetbrains:annotations:24.0.1")
    // implementation("org.projectlombok:lombok:1.18.22")
    errorprone("com.google.errorprone:error_prone_core:2.20.0")

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

    implementation("com.fasterxml.jackson.core:jackson-core:2.15.1")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.1")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.15.1")

}


plugins {
    java
    id("org.springframework.boot") version "3.5.14"
    id("io.spring.dependency-management") version "1.1.7"
    id("com.diffplug.spotless") version "6.25.0"
    jacoco
}

group = "dn"
version = "0.6.4"
description = "Platform of city-quests, real-time and online"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

springBoot {
    buildInfo {
        properties {
            version = project.version.toString()
        }
    }
}

tasks.processResources {
    filteringCharset = "UTF-8"
    filesMatching("**/application.yml") {
        expand(mapOf("version" to project.version, "description" to project.description))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot Starters
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // JWT
    implementation("io.jsonwebtoken:jjwt-api:0.12.5")
    implementation("io.jsonwebtoken:jjwt-impl:0.12.6")
    implementation("io.jsonwebtoken:jjwt-jackson:0.12.5")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.flywaydb:flyway-core:11.8.0")
    implementation("org.flywaydb:flyway-database-postgresql:11.8.0")

    // Database
    runtimeOnly("org.postgresql:postgresql")
    testRuntimeOnly("com.h2database:h2")

    compileOnly("org.projectlombok:lombok")
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    annotationProcessor("org.projectlombok:lombok")

    // For tests
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testCompileOnly("org.projectlombok:lombok")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testAnnotationProcessor("org.projectlombok:lombok")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")

    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.4")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// Only one jar for build — executable bootJar
tasks.named("jar") {
    enabled = false
}

jacoco {
    toolVersion = "0.8.13"
}

tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}
tasks.jacocoTestReport {
    dependsOn(tasks.test)

    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

spotless {
    java {
        googleJavaFormat("1.21.0")
            .reflowLongStrings()
            .skipJavadocFormatting()

        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()

        targetExclude("build/**", "**/generated/**")
    }
}
// Чтобы spotlessCheck запускался перед компиляцией
tasks.compileJava {
    dependsOn(tasks.spotlessCheck)
}

// ### SpotlessCheck ###
//# Проверка стиля
//        ./gradlew spotlessCheck
//
//# Авто-исправление всех ошибок стиля (магия! ✨)
//./gradlew spotlessApply
//
//# Только для Checkstyle
//    ./gradlew checkstyleMain
//    ./gradlew checkstyleTest
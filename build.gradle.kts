/*
 * Copyright 2024 Mark C. Chu-Carroll
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
val antlrVersion: String by project
val cliktVersion: String by project
val guavaVersion: String by project
val junitVersion: String by project
val kotlinVersion: String by project
val mockkVersion: String by project

plugins {
    kotlin("jvm")  version "2.0.0"
    antlr
    application
}

group = "org.goodmath"
version = "0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    antlr("org.antlr:antlr4:$antlrVersion") // use ANTLR version 4
    implementation("org.jcommander:jcommander:1.83")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
    // https://mvnrepository.com/artifact/org.antlr/antlr4
    implementation("org.antlr:antlr4:$antlrVersion") // use ANTLR version 4
    implementation("org.antlr:antlr4-runtime:$antlrVersion")
    implementation("com.github.ajalt.clikt:clikt:$cliktVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testImplementation(kotlin("test"))
    implementation("eu.mihosoft.vrl.jcsg:jcsg:0.5.7")
    implementation("com.github.ajalt.clikt:clikt:$cliktVersion")
}

application {
    // Define the main class for the application.
    mainClass.set("org.goodmath.simplex.Simplex")
}

tasks.compileKotlin {
    dependsOn(tasks.generateGrammarSource)
}

tasks.compileTestKotlin {
    dependsOn(tasks.generateTestGrammarSource)
}
tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

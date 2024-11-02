import java.net.URI
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
rootProject.name = "simplex"

sourceControl {
    gitRepository(URI("https://github.com/miho/JCSG.git")) {
        producesModule("eu.mihosoft.vrl.jcsg:jcsg")
    }
}

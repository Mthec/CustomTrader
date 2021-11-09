plugins {
    java
}

group = "mod.wurmunlimited.npcs.customtrader"
version = "0.3.6"
val shortName = "customtrader"
val wurmServerFolder = "F:/Steam/steamapps/common/Wurm Unlimited/WurmServerLauncher/"

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    testImplementation(project(":WurmTestingHelper"))
    implementation(project(":BMLBuilder"))
    implementation(project(":PlaceNpc"))
    implementation(project(":CreatureCustomiser"))
    implementation(project(":TradeLibrary"))
    implementation("com.wurmonline:server:1.9")
    implementation("org.gotti.wurmunlimited:server-modlauncher:0.45")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

tasks {
    jar {
        doLast {
            copy {
                from(jar)
                into(wurmServerFolder + "mods/" + shortName)
            }

            copy {
                from("src/main/resources/$shortName.properties")
                into(wurmServerFolder + "mods/")
            }
        }

        from(configurations.runtimeClasspath.get().filter { it.name.startsWith("BMLBuilder") && it.name.endsWith("jar") }.map { zipTree(it) })
        from(configurations.runtimeClasspath.get().filter { it.name.startsWith("PlaceNpc") && it.name.endsWith("jar") }.map { zipTree(it) })
        from(configurations.runtimeClasspath.get().filter { it.name.startsWith("CreatureCustomiser") && it.name.endsWith("jar") }.map { zipTree(it) })
        from(configurations.runtimeClasspath.get().filter { it.name.startsWith("TradeLibrary") && it.name.endsWith("jar") }.map { zipTree(it) })

        includeEmptyDirs = false
        archiveFileName.set("$shortName.jar")
        exclude("**/TradeHandler.class", "**/Trade.class", "**/TradingWindow.class", "**/CustomTraderTradeAction.class")

        manifest {
            attributes["Implementation-Version"] = archiveVersion.get()
        }
    }

    register<Zip>("zip") {
        into(shortName) {
            from(jar)
        }

        from("src/main/resources/$shortName.properties")
        archiveFileName.set("$shortName.zip")
    }
}
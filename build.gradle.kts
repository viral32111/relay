import net.fabricmc.loom.task.RemapJarTask

plugins {
	id( "fabric-loom" )
	id( "com.github.johnrengelman.shadow" ) version "8.1.1"
	kotlin( "jvm" ) version( System.getProperty( "kotlin_version" ) )
	kotlin( "plugin.serialization" ) version( System.getProperty( "kotlin_version" ) )
}

base {
	archivesName.set( project.extra[ "archives_base_name" ] as String )
}
version = project.extra[ "mod_version" ] as String
group = project.extra[ "maven_group" ] as String

repositories {
	maven {
		url = uri( "https://maven.pkg.github.com/viral32111/events" )
		credentials {
			username = project.findProperty( "gpr.user" ) as String? ?: System.getenv( "USER" )
			password = project.findProperty( "gpr.key" ) as String? ?: System.getenv( "TOKEN" )
		}
	}
}

val shadowInclude: Configuration by configurations.creating

configurations {
	implementation.get().extendsFrom( shadowInclude )
}

dependencies {

	// Minecraft
	minecraft( "com.mojang", "minecraft", project.extra[ "minecraft_version" ] as String )

	// Minecraft source mappings - https://github.com/FabricMC/yarn
	mappings( "net.fabricmc", "yarn", project.extra[ "yarn_mappings" ] as String, null, "v2" )

	// Fabric Loader - https://github.com/FabricMC/fabric-loader
	modImplementation( "net.fabricmc", "fabric-loader", project.extra[ "loader_version" ] as String )

	// Fabric API - https://github.com/FabricMC/fabric
	modImplementation( "net.fabricmc.fabric-api", "fabric-api", project.extra[ "fabric_version" ] as String )

	// Kotlin support for Fabric - https://github.com/FabricMC/fabric-language-kotlin
	modImplementation( "net.fabricmc", "fabric-language-kotlin", project.extra[ "fabric_language_kotlin_version" ] as String )

	// Kotlin JSON serialization
	implementation( "org.jetbrains.kotlinx", "kotlinx-serialization-json", project.extra[ "kotlinx_serialization_json_version" ] as String )

	// My callbacks - https://github.com/viral32111/events
	modImplementation( "com.viral32111", "events", project.extra[ "events_version" ] as String )

	// Ktor - https://ktor.io/docs/getting-started-ktor-client.html
	implementation("io.ktor", "ktor-client-core", project.extra[ "ktor_version" ] as String )
	implementation( "io.ktor", "ktor-client-cio", project.extra[ "ktor_version" ] as String )
	shadowInclude( "io.ktor", "ktor-client-core", project.extra[ "ktor_version" ] as String )
	shadowInclude( "io.ktor", "ktor-client-cio", project.extra[ "ktor_version" ] as String )

	// Ktor JSON serialization - https://ktor.io/docs/serialization-client.html
	implementation( "io.ktor", "ktor-client-content-negotiation", project.extra[ "ktor_version" ] as String )
	implementation( "io.ktor", "ktor-serialization-kotlinx-json", project.extra[ "ktor_version" ] as String )
	shadowInclude( "io.ktor", "ktor-client-content-negotiation", project.extra[ "ktor_version" ] as String )
	shadowInclude( "io.ktor", "ktor-serialization-kotlinx-json", project.extra[ "ktor_version" ] as String )

}

tasks.register( "remappedShadowJar", type = RemapJarTask::class ) {
	dependsOn( tasks.shadowJar )
	inputFile.set( tasks.shadowJar.get().archiveFile )
	addNestedDependencies.set( true )
}

tasks {
	val javaVersion = JavaVersion.toVersion( ( project.extra[ "java_version" ] as String ).toInt() )

	withType<JavaCompile> {
		options.encoding = "UTF-8"
		sourceCompatibility = javaVersion.toString()
		targetCompatibility = javaVersion.toString()
		options.release.set( javaVersion.toString().toInt() )
	}

	withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
		kotlinOptions {
			jvmTarget = javaVersion.toString()
		}
	}

	jar {
		from( "LICENSE.txt" ) {
			rename { "${ it }_${ base.archivesName.get() }.txt" }
		}
	}

	processResources {

		// Metadata
		filesMatching( "fabric.mod.json" ) {
			expand( mutableMapOf(
				"version" to project.extra[ "mod_version" ] as String,
				"java" to project.extra[ "java_version" ] as String,
				"minecraft" to project.extra[ "minecraft_version" ] as String,
				"fabricloader" to project.extra[ "loader_version" ] as String,
				"fabric_api" to project.extra[ "fabric_version" ] as String,
				"fabric_language_kotlin" to project.extra[ "fabric_language_kotlin_version" ] as String,
				"events" to project.extra[ "events_version" ] as String
			) )
		}

		// Mixins
		filesMatching( "*.mixins.json" ) {
			expand( mutableMapOf(
				"java" to project.extra[ "java_version" ] as String
			) )
		}

	}

	java {
		toolchain {
			languageVersion.set( JavaLanguageVersion.of( javaVersion.toString() ) )
		}

		sourceCompatibility = javaVersion
		targetCompatibility = javaVersion

		withSourcesJar()
	}

	shadowJar {
		configurations = listOf( shadowInclude )
	}

	test {
		useJUnitPlatform()
	}
}

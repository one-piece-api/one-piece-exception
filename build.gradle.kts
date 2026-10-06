plugins {
	`java-library`
	`maven-publish`
	id("io.spring.dependency-management") version "1.1.7"
	id("io.spring.javaformat") version "0.0.48"
	checkstyle
}

group = "dev.onepieceapi"
version = "0.5.0"
description = "One Piece API - shared application exception handling library"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(25)
	}
	withSourcesJar()
	withJavadocJar()
}

repositories {
	mavenCentral()
}

dependencyManagement {
	imports {
		mavenBom("org.springframework.boot:spring-boot-dependencies:4.1.0")
	}
}

dependencies {
	// Exposed to consumers: ProblemDetail, HttpStatus and the servlet-filter base class
	// (OncePerRequestFilter) are all part of this library's own public API.
	api("org.springframework:spring-web")
	// TraceIdFilter/ApplicationExceptionHandler log and populate MDC through the slf4j
	// facade directly - Spring's own logging bridge (spring-jcl) doesn't expose it.
	api("org.slf4j:slf4j-api")
	// Needed only at runtime, to back the Spring Boot auto-configuration entry point below -
	// consumers never reference its types directly, so "implementation" (not "api") is enough;
	// Gradle still puts it on their runtime classpath.
	implementation("org.springframework.boot:spring-boot-autoconfigure")
	compileOnly("jakarta.servlet:jakarta.servlet-api")
	// The routing failures (no resource, method not allowed) are Spring MVC's own exceptions;
	// every consumer is a Spring MVC service, which brings it.
	compileOnly("org.springframework:spring-webmvc")
	// Optional: ProblemDetailOpenApiAutoConfiguration only activates when the consuming
	// service already uses springdoc - never forced onto services that don't.
	compileOnly("org.springdoc:springdoc-openapi-starter-common:3.1.1")
	// Optional: ConcurrentModificationAutoConfiguration only activates when the consuming
	// service already has Spring's data access abstraction - never forced onto services that don't.
	compileOnly("org.springframework:spring-tx")
	compileOnly("org.projectlombok:lombok")
	annotationProcessor("org.projectlombok:lombok")
	testCompileOnly("org.projectlombok:lombok")
	testAnnotationProcessor("org.projectlombok:lombok")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc")
	testImplementation("org.springframework.boot:spring-boot-starter-validation")
	testImplementation("org.springdoc:springdoc-openapi-starter-common:3.1.1")
	testImplementation("org.springframework:spring-tx")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// Keeps parameter names in the bytecode, as Spring Boot's Gradle plugin does for the services
// using this library: the handler names a violated parameter after them.
tasks.withType<JavaCompile> {
	options.compilerArgs.add("-parameters")
}

tasks.withType<Test> {
	useJUnitPlatform()
}

checkstyle {
	toolVersion = "14.0.0"
}

publishing {
	publications {
		create<MavenPublication>("maven") {
			from(components["java"])
			// Dependency versions come from the Spring Boot BOM (io.spring.dependency-management),
			// not literal version strings on each `dependencies { }` entry - this tells Gradle to
			// publish the versions actually resolved on the build classpath instead of an
			// unresolved/absent version in the POM.
			versionMapping {
				usage("java-api") {
					fromResolutionResult()
				}
				usage("java-runtime") {
					fromResolutionResult()
				}
			}
			pom {
				name = "one-piece-exception"
				description = project.description
				url = "https://github.com/one-piece-api/one-piece-exception"
			}
		}
	}
	repositories {
		maven {
			name = "GitHubPackages"
			url = uri("https://maven.pkg.github.com/one-piece-api/one-piece-exception")
			credentials {
				username = System.getenv("GITHUB_ACTOR")
				password = System.getenv("GITHUB_TOKEN")
			}
		}
	}
}

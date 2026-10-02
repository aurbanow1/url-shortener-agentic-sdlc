plugins {
	java
	jacoco
	id("org.springframework.boot") version "4.1.1"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "dev.urlshort"
version = "0.1.0"
description = "urlshort — URL shortener built through a governed agentic SDLC"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
	implementation("org.springframework.boot:spring-boot-starter-flyway")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.1.1")
	runtimeOnly("com.h2database:h2")
}

jacoco {
	toolVersion = "0.8.15"
}

// Two JVM test suites: `test` = unit, `functionalTest` = HTTP journeys through
// the public surface against a temporary H2 database with Flyway applied.
testing {
	suites {
		getByName<JvmTestSuite>("test") {
			useJUnitJupiter()
			dependencies {
				implementation("org.springframework.boot:spring-boot-starter-actuator-test")
				implementation("org.springframework.boot:spring-boot-starter-data-jdbc-test")
				implementation("org.springframework.boot:spring-boot-starter-flyway-test")
				implementation("org.springframework.boot:spring-boot-starter-validation-test")
				implementation("org.springframework.boot:spring-boot-starter-webmvc-test")
				runtimeOnly("org.junit.platform:junit-platform-launcher")
			}
		}
		register<JvmTestSuite>("functionalTest") {
			useJUnitJupiter()
			dependencies {
				implementation(project())
				implementation("org.springframework.boot:spring-boot-starter-actuator-test")
				implementation("org.springframework.boot:spring-boot-starter-data-jdbc-test")
				implementation("org.springframework.boot:spring-boot-starter-flyway-test")
				implementation("org.springframework.boot:spring-boot-starter-validation-test")
				implementation("org.springframework.boot:spring-boot-starter-webmvc-test")
				runtimeOnly("org.junit.platform:junit-platform-launcher")
				runtimeOnly("com.h2database:h2")
			}
			targets {
				all {
					testTask.configure {
						shouldRunAfter(tasks.named("test"))
					}
				}
			}
		}
	}
}

// One JaCoCo report per suite plus a merged one; the gate verifies the merged
// execution data at 100% line and branch coverage.
val functionalTest = tasks.named<Test>("functionalTest")

tasks.jacocoTestReport {
	dependsOn(tasks.test)
	reports {
		xml.required = true
		csv.required = true
		html.required = true
	}
}

val jacocoFunctionalTestReport = tasks.register<JacocoReport>("jacocoFunctionalTestReport") {
	group = "verification"
	description = "Coverage report for the functionalTest suite."
	dependsOn(functionalTest)
	executionData(layout.buildDirectory.file("jacoco/functionalTest.exec"))
	sourceSets(sourceSets.main.get())
	reports {
		xml.required = true
		csv.required = true
		html.required = true
		html.outputLocation = layout.buildDirectory.dir("reports/jacoco/functionalTest/html")
		xml.outputLocation = layout.buildDirectory.file("reports/jacoco/functionalTest/jacocoTestReport.xml")
		csv.outputLocation = layout.buildDirectory.file("reports/jacoco/functionalTest/jacocoTestReport.csv")
	}
}

val jacocoAllReport = tasks.register<JacocoReport>("jacocoAllReport") {
	group = "verification"
	description = "Merged coverage report across both suites."
	dependsOn(tasks.test, functionalTest)
	executionData(fileTree(layout.buildDirectory.dir("jacoco")) { include("*.exec") })
	sourceSets(sourceSets.main.get())
	reports {
		xml.required = true
		csv.required = true
		html.required = true
		html.outputLocation = layout.buildDirectory.dir("reports/jacoco/all/html")
		xml.outputLocation = layout.buildDirectory.file("reports/jacoco/all/jacocoTestReport.xml")
		csv.outputLocation = layout.buildDirectory.file("reports/jacoco/all/jacocoTestReport.csv")
	}
}

tasks.jacocoTestCoverageVerification {
	dependsOn(tasks.test, functionalTest)
	executionData(fileTree(layout.buildDirectory.dir("jacoco")) { include("*.exec") })
	violationRules {
		rule {
			limit {
				counter = "LINE"
				value = "COVEREDRATIO"
				minimum = "1.0".toBigDecimal()
			}
			limit {
				counter = "BRANCH"
				value = "COVEREDRATIO"
				minimum = "1.0".toBigDecimal()
			}
		}
	}
}

tasks.check {
	dependsOn(functionalTest, tasks.jacocoTestReport, jacocoFunctionalTestReport, jacocoAllReport, tasks.jacocoTestCoverageVerification)
}

tasks.bootJar {
	archiveFileName = "urlshort.jar"
}

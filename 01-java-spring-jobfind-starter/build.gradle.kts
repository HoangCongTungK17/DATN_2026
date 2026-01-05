plugins {
	java
	id("org.springframework.boot") version "3.2.4"
	id("io.spring.dependency-management") version "1.1.4"
	id("io.freefair.lombok") version "8.6"
}

group = "vn.hoangtung"
version = "0.0.1-SNAPSHOT"

java {
	sourceCompatibility = JavaVersion.VERSION_17
}
springBoot {
    mainClass.set("vn.hoangtung.jobfind.JobFindApplication")
}

repositories {
	mavenCentral()
	maven { url = uri("https://repo.spring.io/milestone") }
}

val springAiVersion = "1.0.0-M1" // Phiên bản ổn định mới nhất

dependencyManagement {
	imports {
		mavenBom("org.springframework.ai:spring-ai-bom:$springAiVersion")
	}
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.thymeleaf.extras:thymeleaf-extras-springsecurity6")
	implementation("com.turkraft.springfilter:jpa:3.1.7")
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.5.0")
	implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
	implementation("org.springframework.boot:spring-boot-starter-mail")
	developmentOnly("org.springframework.boot:spring-boot-devtools")
	runtimeOnly("com.mysql:mysql-connector-j")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.security:spring-security-test")
	implementation("org.springframework.ai:spring-ai-openai-spring-boot-starter") // Dùng để gọi Groq
    implementation("org.springframework.ai:spring-ai-transformers-spring-boot-starter") // Tạo Embedding miễn phí (offline)
    implementation("org.springframework.ai:spring-ai-pinecone-store-spring-boot-starter") // Lưu Vector
}

tasks.withType<Test> {
	useJUnitPlatform()
}

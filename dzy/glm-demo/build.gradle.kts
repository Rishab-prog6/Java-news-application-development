plugins {
    id("java")
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")

    implementation("cn.bigmodel.openapi:oapi-java-sdk:release-V4-2.0.2")
}

tasks.test {
    useJUnitPlatform()
}
plugins {
    id("com.google.devtools.ksp")
    kotlin("jvm")
}

version = "1.0-SNAPSHOT"

dependencies {
    implementation(kotlin("stdlib"))
    implementation(project(":datainterface-processor"))
    ksp(project(":datainterface-processor"))
}

ksp {
    arg("option1", "value1")
    arg("option2", "value2")
}

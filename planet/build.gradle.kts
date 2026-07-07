plugins {
    id("com.utopia-rise.godot-kotlin-jvm") version "0.13.1-4.4.1"
}

repositories {
    mavenCentral()
}

dependencies {
    val godotKotlinVersion = "0.13.1-4.4.1"

    implementation("com.github.davidmoten:rtree-multi:0.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.19.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.19.2")

    // The Godot plugin adds these only to the main compile classpath, while its
    // KSP processor also generates a registration entry for the test source set.
    testImplementation("com.utopia-rise:common:$godotKotlinVersion")
    testImplementation("com.utopia-rise:godot-build-props:$godotKotlinVersion")
    testImplementation("com.utopia-rise:godot-core-library-debug:$godotKotlinVersion")
    testImplementation("com.utopia-rise:godot-api-library-debug:$godotKotlinVersion")
    testImplementation("com.utopia-rise:godot-extension-library-debug:$godotKotlinVersion")
    testRuntimeOnly("com.utopia-rise:godot-internal-library-debug:$godotKotlinVersion")
    testImplementation(kotlin("test-junit5"))
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}

godot {
    // ---------Setup-----------------

    // the script registration which you'll attach to nodes are generated into this directory
    registrationFileBaseDir.set(projectDir.resolve("gdj"))

	// Create .gdj files from all JVM scripts
	isRegistrationFileGenerationEnabled.set(true)

    // defines whether the script registration files should be generated hierarchically according to the classes package path or flattened into `registrationFileBaseDir`
    //isRegistrationFileHierarchyEnabled.set(true)

    // defines whether your scripts should be registered with their fqName or their simple name (can help with resolving script name conflicts)
    //isFqNameRegistrationEnabled.set(false)

    // ---------Android----------------

    // NOTE: Make sure you read: https://godot-kotl.in/en/stable/user-guide/exporting/#android as not all jvm libraries are compatible with android!
    // IMPORTANT: Android export should to be considered from the start of development!
    //isAndroidExportEnabled.set(ANDROID_ENABLED)
    //d8ToolPath.set(File("D8_TOOL_PATH"))
    //androidCompileSdkDir.set(File("ANDROID_COMPILE_SDK_DIR"))

    // --------IOS and Graal------------

    // NOTE: this is an advanced feature! Read: https://godot-kotl.in/en/stable/user-guide/advanced/graal-vm-native-image/
    // IMPORTANT: Graal Native Image needs to be considered from the start of development!
    //isGraalNativeImageExportEnabled.set(IS_GRAAL_VM_ENABLED)
    //graalVmDirectory.set(File("GRAAL_VM_DIR"))
    //windowsDeveloperVCVarsPath.set(File("WINDOWS_DEVELOPER_VS_VARS_PATH"))
    //isIOSExportEnabled.set(IS_IOS_ENABLED)

	// --------Library authors------------

	// library setup. See: https://godot-kotl.in/en/stable/develop-libraries/
    //classPrefix.set("MyCustomClassPrefix")
    //projectName.set("LibraryProjectName")
    //projectName.set("LibraryProjectName")
}

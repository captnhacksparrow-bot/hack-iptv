import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy
import java.util.Base64

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
  alias(libs.plugins.google.services)
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.aistudio.captnhackstreams.iptv"
    minSdk = 24
    targetSdk = 34
    versionCode = 16
    versionName = "16.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }

    // Load release signing properties from project properties or environment
    val ksStorePass = (project.findProperty("STORE_PASSWORD") as? String) ?: System.getenv("STORE_PASSWORD") ?: "changeit"
    val ksAlias = (project.findProperty("KEY_ALIAS") as? String) ?: System.getenv("KEY_ALIAS") ?: "upload"
    val ksKeyPass = (project.findProperty("KEY_PASSWORD") as? String) ?: System.getenv("KEY_PASSWORD") ?: "changeit"

    // Decode base64 keystore if present to prevent UTF-8 corruption from file sync daemon
    val b64File = file("${rootDir}/my-upload-key.jks.base64")
    val decodedKeystoreFile = if (b64File.exists()) {
      val tempKeystore = file("/tmp/my-upload-key-decoded.jks")
      val base64Bytes = b64File.readText().trim()
      val decodedBytes = Base64.getDecoder().decode(base64Bytes)
      tempKeystore.writeBytes(decodedBytes)
      tempKeystore
    } else {
      null
    }

    val ksPath = (project.findProperty("KEYSTORE_PATH") as? String) ?: System.getenv("KEYSTORE_PATH") ?: decodedKeystoreFile?.absolutePath ?: "my-upload-key.jks"

    create("releaseConfig") {
      val keystoreFile = if (ksPath.startsWith("/")) file(ksPath) else file("${rootDir}/$ksPath")
      if (keystoreFile.exists()) {
        storeFile = keystoreFile
        storePassword = ksStorePass
        keyAlias = ksAlias
        keyPassword = ksKeyPass
      } else {
        storeFile = file("${rootDir}/debug.keystore")
        storePassword = "android"
        keyAlias = "androiddebugkey"
        keyPassword = "android"
      }
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      isShrinkResources = false
      signingConfig = signingConfigs.getByName("releaseConfig")
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "${rootDir}/proguard-rules.pro")
    }
    debug {
      signingConfig = signingConfigs.getByName("debugConfig")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
}

googleServices {
  missingGoogleServicesStrategy = MissingGoogleServicesStrategy.WARN
}


// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
//  implementation(libs.libvlc)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.fragment)
  // implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.coil.compose)
  implementation(libs.media3.exoplayer)
  implementation(libs.media3.exoplayer.hls)
  implementation(libs.media3.ui)
  implementation(libs.converter.moshi)
  // implementation(libs.firebase.ai)
  // implementation(libs.firebase.appcheck.recaptcha)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}

tasks.register<Copy>("copyToVisibleFolder") {
    from(layout.buildDirectory.dir("outputs/apk/debug")) {
        include("app-debug.apk")
    }
    from(layout.buildDirectory.dir("outputs/bundle/debug")) {
        include("app-debug.aab")
    }
    from(layout.buildDirectory.dir("outputs/apk/release")) {
        include("app-release.apk")
    }
    from(layout.buildDirectory.dir("outputs/bundle/release")) {
        include("app-release.aab")
    }
    into(rootProject.layout.projectDirectory.dir("App_Build_Files"))
}

tasks.register<Copy>("copyToBuildOutputs") {
    from(layout.buildDirectory.dir("outputs/apk/debug")) {
        include("app-debug.apk")
    }
    from(layout.buildDirectory.dir("outputs/bundle/debug")) {
        include("app-debug.aab")
    }
    from(layout.buildDirectory.dir("outputs/apk/release")) {
        include("app-release.apk")
    }
    from(layout.buildDirectory.dir("outputs/bundle/release")) {
        include("app-release.aab")
    }
    into(rootProject.layout.projectDirectory.dir(".build-outputs"))
}

tasks.configureEach {
    if (name == "assembleDebug" || name == "bundleDebug" || name == "assembleRelease" || name == "bundleRelease") {
        finalizedBy("copyToVisibleFolder", "copyToBuildOutputs")
    }
}

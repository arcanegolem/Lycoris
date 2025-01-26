plugins {
   id("com.android.library")
   id("org.jetbrains.kotlin.android")
   id("maven-publish")
   id("org.jetbrains.kotlin.plugin.compose")
}

android {
   namespace = "ru.spektrit.pdfcompose"
   compileSdk = 34

   defaultConfig {
      minSdk = 24

      testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
      consumerProguardFiles("consumer-rules.pro")
   }

   buildFeatures {
      compose = true
   }

   buildTypes {
      release {
         isMinifyEnabled = false
         proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro"
         )
      }
   }

   compileOptions {
      sourceCompatibility = JavaVersion.VERSION_1_8
      targetCompatibility = JavaVersion.VERSION_1_8
   }
   kotlinOptions {
      jvmTarget = "1.8"
   }
   publishing {
      singleVariant("release") {
         withSourcesJar()
      }
   }
}

dependencies {

   implementation("androidx.core:core-ktx:1.15.0")
   implementation("androidx.appcompat:appcompat:1.7.0")
   implementation("com.google.android.material:material:1.12.0")
   testImplementation("junit:junit:4.13.2")
   androidTestImplementation("androidx.test.ext:junit:1.2.1")
   androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")

   implementation(platform("androidx.compose:compose-bom:2025.01.00"))
   implementation("androidx.compose.ui:ui")
   implementation("androidx.compose.ui:ui-graphics")
   implementation("androidx.compose.material3:material3")

   // Coil
   implementation("io.coil-kt:coil-compose:2.7.0")

   // Retrofit 2
   implementation("com.squareup.retrofit2:retrofit:2.9.0")
   implementation("com.squareup.okhttp3:okhttp:4.12.0")


   // Material Icons
   implementation("androidx.compose.material:material-icons-extended")
}

publishing {
   publications {
      register<MavenPublication>("release") {
         groupId = "ru.spektrit"
         artifactId = "lycoris"
         version = "0.1.0"

         afterEvaluate {
            from(components["release"])
         }
      }
   }
}

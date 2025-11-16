plugins {
    alias(libs.plugins.androidApplication)
}

android {
    namespace = "com.example.smartfirstaid"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.smartfirstaid"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildFeatures {
            buildConfig = true
        }
        // Inject API KEY from local.properties
//        buildConfigField(
//                "String",
//                "GOOGLE_API_KEY",
//                "\"${project.properties["GOOGLE_API_KEY"]}\""
//        )
        buildConfigField("String", "GROQ_API_KEY", "\"${project.properties["GROQ_API_KEY"]}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation("com.google.android.material:material:1.12.0")
    implementation("org.mongodb:mongodb-driver-sync:4.3.1")
    implementation("com.google.android.gms:play-services-location:21.2.0")
    implementation("com.squareup.okhttp3:okhttp:4.9.3")
    implementation("androidx.viewpager2:viewpager2:1.1.0")
    implementation("com.github.bumptech.glide:glide:4.15.1")
    annotationProcessor("com.github.bumptech.glide:compiler:4.15.1")
}
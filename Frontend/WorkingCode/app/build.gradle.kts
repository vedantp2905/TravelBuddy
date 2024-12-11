plugins {
    alias(libs.plugins.android.application)
    id("jacoco")
}

android {
    namespace = "com.example.finalapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.finalapp"
        minSdk = 33
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        getByName("debug") {
            isMinifyEnabled = false
            isShrinkResources = false
            enableUnitTestCoverage = true
            enableAndroidTestCoverage = true
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
        baseline = file("lint-baseline.xml")
        disable += listOf(
            "CheckResult",
            "ScrollViewSize",
            "ScopedStorage",
            "ObsoleteSdkInt",
            "UnusedResources",
            "WebViewLayout",
            "MissingInflatedId",
            "DefaultLocale",
            "GradleDependency",
            "SetJavaScriptEnabled",
            "DataExtractionRules"
        )
    }

    buildFeatures {
        buildConfig = true
    }

    configurations.all {
        resolutionStrategy {
            cacheChangingModulesFor(0, "seconds")
        }
    }

}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation("com.android.volley:volley:1.2.1")
    implementation(libs.converter.gson)
    implementation(libs.play.services.location)
    implementation(libs.swiperefreshlayout)
    implementation(libs.tools.core)

    // Test dependencies
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // Networking dependencies
    implementation("com.squareup.okhttp3:okhttp:4.9.3")
    implementation("com.squareup.okhttp3:logging-interceptor:4.9.3")
    implementation("com.stripe:stripe-android:20.25.1")

    // RecyclerView dependency (latest version)
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.recyclerview:recyclerview-selection:1.1.0")

    // WebSocket dependencies
    implementation("org.java-websocket:Java-WebSocket:1.5.1")
    implementation("com.google.code.gson:gson:2.8.9")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")

    // Retrofit for networking
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // Stomp Protocol and RxJava dependencies
    implementation("com.github.NaikSoftware:StompProtocolAndroid:1.6.6")
    implementation("io.reactivex.rxjava2:rxjava:2.2.21")
    implementation("io.reactivex.rxjava2:rxandroid:2.1.1")

    // AppCompat dependency
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")

    implementation("com.itextpdf:itextpdf:5.5.13.3")


    implementation("com.github.mhiew:android-pdf-viewer:3.2.0-beta.1")

    implementation("com.squareup.okhttp3:okhttp:4.9.1")
    implementation("com.squareup.okhttp3:okhttp-urlconnection:4.9.1")

    implementation("com.github.chrisbanes:PhotoView:2.3.0")

    // Add these new dependencies
    implementation("com.github.bumptech.glide:glide:4.12.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.12.0")
    implementation("commons-io:commons-io:2.11.0")
    implementation("com.github.Yalantis:uCrop:2.2.8-native")
    implementation("com.google.android.material:material:1.9.0")

    // for Android
    testImplementation ("junit:junit:4.13.2")
    androidTestImplementation ("com.android.support.test:rules:1.0.2")
    androidTestImplementation ("com.android.support.test:runner:1.0.2")
    androidTestImplementation ("androidx.test.ext:junit:1.1.5")
    androidTestImplementation ("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation ("androidx.test.espresso:espresso-contrib:3.4.0")
    androidTestImplementation ("androidx.test.espresso:espresso-intents:3.4.0")

    // AndroidX Test dependencies
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.test:rules:1.4.0")
    androidTestImplementation("androidx.test:runner:1.4.0")

    // Ensure you have the correct version of Jacoco
    testImplementation("org.jacoco:org.jacoco.agent:0.8.8")

    implementation ("com.google.android.gms:play-services-location:21.0.1")

}
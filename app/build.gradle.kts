plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")

    //id("com.chaquo.python")
}



android {
    namespace = "mo.edu.kaoyip.kyrobot.futurecampus"
    compileSdk = 34

    defaultConfig {
        applicationId = "mo.edu.kaoyip.kyrobot.futurecampus"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        externalNativeBuild {
            cmake {
                cppFlags("-std=c++17 -frtti -fexceptions")
//                abiFilters("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
                abiFilters("arm64-v8a")
                arguments("-DANDROID_STL=c++_shared")
            }
        }

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

    externalNativeBuild {
        cmake {
            version = "3.22.1"
             path = file("src/main/jniLibs/CMakeLists.txt")


        }
    }
    sourceSets {
        getByName("main") {
            jniLibs.srcDirs("src\\main\\jniLibs","src\\main\\cpp") // \\openCV


        }
    }

    packagingOptions {
        pickFirst("**/*.so")

    }




}

/*chaquopy{
    defaultConfig {
        buildPython("E:\\Python310\\python.exe")
        pyc {
            src = false
        }
        pip{
            install("numpy")
            install("pandas")
            install("opencv-python")
            install("torch")
            install("torchvision")
            // install("torchaudio")
            install("TensorFlow")
        }

    }
}*/


dependencies {

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation(project(":OpenCV490"))
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    implementation(fileTree(mapOf("include" to listOf("*.jar"), "dir" to "libs")))

    implementation("com.squareup.okhttp3:okhttp:4.11.0")

    // webview
     implementation("androidx.webkit:webkit:1.10.0")

}
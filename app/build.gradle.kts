plugins {
    id("com.android.application")
}

android {
    namespace = "maggi.privacy"
    compileSdk = 33
    
    defaultConfig {
        applicationId = "maggi.privacy"
        minSdk = 21
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"
        
        vectorDrawables { 
            useSupportLibrary = true
        }
    }
    
    compileOptions {
        // ESSENCIAL: Ativa o suporte a métodos de Java moderno no Android 9
        isCoreLibraryDesugaringEnabled = true 
        
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        viewBinding = true
    }

    packagingOptions {
        resources {
            excludes += "META-INF/proguard/androidx-annotations.pro"
            excludes += "META-INF/DEPENDENCIES"
        }
    }
}

configurations.all {
    exclude(group = "com.github.spotbugs", module = "spotbugs-annotations")
}

dependencies {
    // Biblioteca que contém a "tradução" dos métodos novos do Java para o Android antigo
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.3")

    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("com.google.android.material:material:1.9.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")
    implementation("androidx.recyclerview:recyclerview:1.3.0")
    
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    implementation("androidx.media3:media3-exoplayer:1.1.0")
    implementation("androidx.media3:media3-ui:1.1.0")
    implementation("androidx.media3:media3-session:1.1.0")
    
    implementation("com.github.TeamNewPipe:NewPipeExtractor:v0.24.3")
    implementation("com.github.TeamNewPipe:nanojson:1d9e1aea9049fc9f85e68b43ba39fe7be1c1f751")
    
    implementation("org.jsoup:jsoup:1.16.1")
    implementation("com.fasterxml.jackson.core:jackson-core:2.15.2")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.15.2")
}

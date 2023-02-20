plugins {
    id("com.av.android.library")
}

android {
    namespace = "com.protectednet.utilizr"

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            ndk {
                abiFilters += listOf("arm64-v8a", "armeabi-v7a")
            }
        }
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.corektx)
    implementation(libs.rxkotlin)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.recyclerview)

//    testImplementation 'junit:junit:4.13.2'
//    androidTestImplementation 'androidx.test:runner:1.4.0'
//    androidTestImplementation 'androidx.test.espresso:espresso-core:3.4.0'

}

import java.util.Properties
plugins { id("com.android.application"); id("org.jetbrains.kotlin.android"); id("org.jetbrains.kotlin.plugin.compose"); id("org.jetbrains.kotlin.plugin.serialization") }
val nativeLocal = Properties().apply { rootProject.file("local.properties").takeIf { it.isFile }?.inputStream()?.use { load(it) } }
val publishableKey = providers.environmentVariable("SUPABASE_PUBLISHABLE_KEY").orNull ?: nativeLocal.getProperty("supabase.publicKey", "")
require(publishableKey.isBlank() || Regex("sb_publishable_[A-Za-z0-9_-]+").matches(publishableKey)) { "Only a publishable client key is allowed" }
android {
 namespace = "com.msoumaya.androidcoran"
 compileSdk = 36
 defaultConfig { applicationId = "com.msoumaya.androidcoran"; minSdk = 26; targetSdk = 36; versionCode = 1; versionName = "0.1.0"; testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"; buildConfigField("String", "SUPABASE_PUBLIC_KEY", "\"$publishableKey\"") }
 buildFeatures { compose = true; buildConfig = true }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
 buildTypes { release { isMinifyEnabled = false; proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro") } }
}
kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }
dependencies {
 implementation(platform("androidx.compose:compose-bom:2026.05.01"))
 implementation("androidx.activity:activity-compose:1.12.4")
 implementation("androidx.compose.material3:material3")
 implementation("androidx.compose.material:material-icons-extended")
 implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
 implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
 implementation("androidx.datastore:datastore-preferences:1.2.1")
 implementation("androidx.work:work-runtime-ktx:2.11.1")
 implementation("androidx.media3:media3-exoplayer:1.9.3")
 implementation("androidx.media3:media3-session:1.9.3")
 implementation(platform("io.github.jan-tennert.supabase:bom:3.8.0"))
 implementation("io.github.jan-tennert.supabase:auth-kt")
 implementation("io.github.jan-tennert.supabase:postgrest-kt")
 implementation("io.github.jan-tennert.supabase:storage-kt")
 implementation("io.github.jan-tennert.supabase:realtime-kt")
 implementation("io.ktor:ktor-client-okhttp:3.4.1")
 implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.10.0")
 testImplementation("junit:junit:4.13.2")
 androidTestImplementation("androidx.test:runner:1.7.0")
 androidTestImplementation("androidx.test.ext:junit:1.3.0")
 androidTestImplementation(platform("androidx.compose:compose-bom:2026.05.01"))
 androidTestImplementation("androidx.compose.ui:ui-test-junit4")
 debugImplementation("androidx.compose.ui:ui-test-manifest")
}

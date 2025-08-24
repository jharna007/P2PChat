-keep class org.webrtc.** { *; }
-keep class com.google.firebase.** { *; }
-keep class com.google.gson.** { *; }
-keep class androidx.room.** { *; }
-keep class com.kaifcodec.p2pchat.models.** { *; }
-keep class com.kaifcodec.p2pchat.data.local.entities.** { *; }

# ZXing
-keep class com.google.zxing.** { *; }
-keep class com.journeyapps.barcodescanner.** { *; }

# WebRTC
-keep class org.webrtc.** { *; }
-dontwarn org.webrtc.**

# SQLCipher
-keep class net.sqlcipher.** { *; }
-dontwarn net.sqlcipher.**

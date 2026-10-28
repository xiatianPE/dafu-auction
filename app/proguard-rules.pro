# 保留 Koin / Room 生成的类
-keep class org.koin.** { *; }
-keep class com.dafu.auction.data.local.** { *; }
-keepclassmembers class * {
    @androidx.room.* <methods>;
}
-dontwarn org.koin.**

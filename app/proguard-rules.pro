# SQLCipher uses JNI; keep its classes so R8 does not strip or rename them.
-keep class net.zetetic.database.** { *; }
-keep class net.zetetic.database.sqlcipher.** { *; }

# kotlinx.serialization generated serializers.
-keepclassmembers class app.agentsetu.** {
    *** Companion;
}
-keepclasseswithmembers class app.agentsetu.** {
    kotlinx.serialization.KSerializer serializer(...);
}

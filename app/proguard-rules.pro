# Release shrinking rules. Add keep rules here only with a comment saying why.

# SQLCipher's native code calls back into these classes by name over JNI, so shrinking must not rename or remove them.
-keep class net.zetetic.database.** { *; }

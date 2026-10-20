# Credential Manager finds its Play services provider by class name, so release shrinking must keep it.
-if class androidx.credentials.CredentialManager
-keep class androidx.credentials.playservices.** { *; }

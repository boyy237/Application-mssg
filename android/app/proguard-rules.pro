# Règles ProGuard par défaut du projet CipherTalk.
# Ajoutez ici vos règles spécifiques si vous activez la minification (isMinifyEnabled = true).
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.ciphertalk.messenger.data.model.** { *; }

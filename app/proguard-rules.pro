# Add release-only keep rules when release shrinking is enabled.
# NewPipeExtractor uses Rhino for YouTube signature handling.
-keep class org.mozilla.javascript.** { *; }
-keep class org.mozilla.classfile.ClassFileWriter
-dontwarn org.mozilla.javascript.tools.**
# NewPipe uses Rhino's interpreted mode, not the desktop JSR-223/JDK linker.
# Aligned with TeamNewPipe/NewPipe app/proguard-rules.pro.
-dontwarn org.mozilla.javascript.JavaToJSONConverters
-dontwarn javax.script.**
-dontwarn jdk.dynalink.**
-keep class org.schabi.newpipe.extractor.timeago.patterns.** { *; }

# CHETO Authenticator release R8 rules.
# Keep only classes/members that are instantiated reflectively at runtime.

# WorkManager/Room: WorkManager creates its generated Room database implementation
# reflectively. AGP/R8 full mode can remove the no-arg constructor.
-keep class androidx.work.impl.WorkDatabase_Impl {
    <init>();
}

# WorkManager may instantiate workers and input mergers reflectively.
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class * extends androidx.work.InputMerger {
    public <init>();
}

# ML Kit component registrars are discovered and instantiated by class name.
-keep class com.google.mlkit.common.internal.CommonComponentRegistrar {
    public <init>();
}
-keep class com.google.mlkit.vision.barcode.internal.BarcodeRegistrar {
    public <init>();
}
-keep class com.google.mlkit.vision.common.internal.VisionCommonRegistrar {
    public <init>();
}

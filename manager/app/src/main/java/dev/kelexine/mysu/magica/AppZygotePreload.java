package dev.kelexine.mysu.magica;

import android.app.ZygotePreload;
import android.content.pm.ApplicationInfo;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.File;

public class AppZygotePreload implements ZygotePreload {
    public static final String TAG = "MySUMagica";

    private static native void forkDontCareAndExecMysud(String mysudPath, String packageName);

    @Override
    public void doPreload(@NonNull ApplicationInfo appInfo) {
        File f = new File(appInfo.nativeLibraryDir, "libmysud.so");
        try {
            System.loadLibrary("mysu");
            Log.d(TAG, "executing magica ...");
            forkDontCareAndExecMysud(f.getAbsolutePath(), appInfo.packageName);
        } catch (Throwable t) {
            Log.e(TAG, "failed to late load", t);
        }
    }
}

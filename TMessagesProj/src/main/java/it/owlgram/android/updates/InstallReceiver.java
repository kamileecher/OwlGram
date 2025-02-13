package it.owlgram.android.updates;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.RequiresApi;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class InstallReceiver extends BroadcastReceiver {
    private final Context context;
    private final String packageName;
    private final Runnable onSuccess;
    private final CountDownLatch latch = new CountDownLatch(1);
    private Intent intent = null;
    public int resultValue;

    InstallReceiver(Context context, String packageName, Runnable onSuccess) {
        this.context = context;
        this.packageName = packageName;
        this.onSuccess = onSuccess;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onReceive(Context c, Intent i) {
        if (Intent.ACTION_PACKAGE_ADDED.equals(i.getAction())) {
            Uri data = i.getData();
            if (data == null || onSuccess == null) return;
            String pkg = data.getSchemeSpecificPart();
            if (pkg.equals(packageName)) {
                onSuccess.run();
                context.unregisterReceiver(this);
            }
            return;
        }
        int status = i.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE_INVALID);
        resultValue = status;
        switch (status) {
            case PackageInstaller.STATUS_PENDING_USER_ACTION:
                intent = i.getParcelableExtra(Intent.EXTRA_INTENT);
                break;
            case PackageInstaller.STATUS_FAILURE:
            case PackageInstaller.STATUS_FAILURE_BLOCKED:
            case PackageInstaller.STATUS_FAILURE_CONFLICT:
            case PackageInstaller.STATUS_FAILURE_INCOMPATIBLE:
            case PackageInstaller.STATUS_FAILURE_INVALID:
            case PackageInstaller.STATUS_FAILURE_STORAGE:
            case PackageInstaller.STATUS_FAILURE_ABORTED:
            case PackageInstaller.STATUS_SUCCESS:
            default:
                if (onSuccess != null) {
                    onSuccess.run();
                }
                context.unregisterReceiver(this);
        }
        latch.countDown();
    }

    // @WorkerThread @Nullable
    public Intent waitIntent() {
        try {
            //noinspection ResultOfMethodCallIgnored
            latch.await(5, TimeUnit.SECONDS);
        } catch (Exception ignored) {
        }
        return intent;
    }
}
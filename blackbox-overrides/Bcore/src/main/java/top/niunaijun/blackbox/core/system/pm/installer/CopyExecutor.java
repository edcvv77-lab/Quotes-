package top.niunaijun.blackbox.core.system.pm.installer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import top.niunaijun.blackbox.core.env.BEnvironment;
import top.niunaijun.blackbox.core.system.pm.BPackageSettings;
import top.niunaijun.blackbox.entity.pm.InstallOption;
import top.niunaijun.blackbox.utils.FileUtils;
import top.niunaijun.blackbox.utils.NativeUtils;
import top.niunaijun.blackbox.utils.Slog;

/**
 * Quotes Android 11 overlay.
 *
 * Storage installs copy the base APK, every split APK and native libraries into
 * BlackBox private storage. Guests therefore do not depend on an APK path owned
 * by a normally-installed host package after import.
 */
public class CopyExecutor implements Executor {
    private static final String TAG = "CopyExecutor";

    @Override
    public int exec(BPackageSettings ps, InstallOption option, int userId) {
        try {
            if (!option.isFlag(InstallOption.FLAG_SYSTEM)) {
                NativeUtils.copyNativeLib(
                        new File(ps.pkg.baseCodePath),
                        BEnvironment.getAppLibDir(ps.pkg.packageName)
                );
                if (ps.pkg.applicationInfo != null
                        && ps.pkg.applicationInfo.splitSourceDirs != null) {
                    for (String split : ps.pkg.applicationInfo.splitSourceDirs) {
                        if (split == null || split.length() == 0) continue;
                        try {
                            NativeUtils.copyNativeLib(
                                    new File(split),
                                    BEnvironment.getAppLibDir(ps.pkg.packageName)
                            );
                        } catch (Throwable ignored) {
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }

        if (option.isFlag(InstallOption.FLAG_STORAGE)) {
            File origFile = new File(ps.pkg.baseCodePath);
            File newFile = BEnvironment.getBaseApkDir(ps.pkg.packageName);
            try {
                if (option.isFlag(InstallOption.FLAG_URI_FILE)) {
                    boolean moved = FileUtils.renameTo(origFile, newFile);
                    if (!moved) FileUtils.copyFile(origFile, newFile);
                } else {
                    FileUtils.copyFile(origFile, newFile);
                }
                newFile.setReadOnly();
                ps.pkg.baseCodePath = newFile.getAbsolutePath();
            } catch (IOException e) {
                e.printStackTrace();
                return -1;
            }

            if (ps.pkg.applicationInfo != null
                    && ps.pkg.applicationInfo.splitSourceDirs != null
                    && ps.pkg.applicationInfo.splitSourceDirs.length > 0) {
                File splitDir = BEnvironment.getSplitApkDir(ps.pkg.packageName);
                FileUtils.deleteDir(splitDir);
                FileUtils.mkdirs(splitDir);

                ArrayList<String> copiedPaths = new ArrayList<>();
                for (String split : ps.pkg.applicationInfo.splitSourceDirs) {
                    if (split == null || split.length() == 0) continue;

                    File splitFile = new File(split);
                    if (!splitFile.isFile()) {
                        Slog.w(TAG, "Split APK missing before private copy: " + split);
                        return -1;
                    }

                    File copied = new File(splitDir, splitFile.getName());
                    try {
                        FileUtils.copyFile(splitFile, copied);
                        copied.setReadOnly();
                        copiedPaths.add(copied.getAbsolutePath());
                    } catch (IOException e) {
                        e.printStackTrace();
                        return -1;
                    }
                }

                if (copiedPaths.size() != ps.pkg.applicationInfo.splitSourceDirs.length) {
                    Slog.w(TAG, "Not every split APK was copied for " + ps.pkg.packageName);
                    return -1;
                }

                ps.pkg.applicationInfo.splitSourceDirs =
                        copiedPaths.toArray(new String[copiedPaths.size()]);
                ps.pkg.applicationInfo.splitPublicSourceDirs =
                        ps.pkg.applicationInfo.splitSourceDirs;
                Slog.i(
                        TAG,
                        "Private split copy complete package=" + ps.pkg.packageName
                                + " count=" + copiedPaths.size()
                );
            }
        }
        return 0;
    }
}

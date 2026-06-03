package top.weixiansen574.alarmshell.xposed;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Locale;

import android.util.Log;

public class ShellExecThread extends Thread {
    private static final String TAG = "AlarmShell";
    private final String shFilePath;

    public ShellExecThread(String shFilePath) {
        this.shFilePath = shFilePath;
    }

    @Override
    public void run() {
        try {
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            DataInputStream is = new DataInputStream(process.getInputStream());
            os.write(("sh " + shFilePath + "\n").getBytes(StandardCharsets.UTF_8));
            os.writeBytes("exit\n");
            os.flush();
            String line = null;
            while ((line = is.readLine()) != null) {
                Log.i(TAG, line);
            }
            process.waitFor();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            Log.i(TAG, sdf.format(System.currentTimeMillis()) + " shell脚本执行结束：" + shFilePath);
        } catch (IOException | InterruptedException e) {
            Log.e(TAG, "Shell execution failed", e);
            throw new RuntimeException(e);
        }
    }
}

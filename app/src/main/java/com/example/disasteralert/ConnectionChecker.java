package com.example.disasteralert;

import android.content.Context;
import android.telephony.TelephonyManager;

import java.io.IOException;

public class ConnectionChecker {
    public ConnectionChecker() {
    }

    public boolean isOnline() {
        Runtime runtime = Runtime.getRuntime();
        try {
            Process ipProcess = runtime.exec("/system/bin/ping -c 1 8.8.8.8");
            int     exitValue = ipProcess.waitFor();
            return (exitValue == 0);
        }
        catch (IOException e)          { e.printStackTrace(); }
        catch (InterruptedException e) { e.printStackTrace(); }

        return false;
    }

    public boolean isMobileAvailable(Context appcontext) {
        TelephonyManager tel = (TelephonyManager) appcontext.getSystemService(Context.TELEPHONY_SERVICE);
        return ((tel.getNetworkOperator() != null && tel.getNetworkOperator().equals("")) ? false : true);
    }
}
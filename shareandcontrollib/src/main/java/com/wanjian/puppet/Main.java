package com.wanjian.puppet;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Point;
import android.hardware.input.InputManager;
import android.media.projection.MediaProjection;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.v4.view.InputDeviceCompat;
import android.util.Log;
import android.view.IWindowManager;
import android.view.InputEvent;
import android.view.KeyEvent;
import android.view.MotionEvent;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.Socket;

/**
 * Created by wanjian on 2017/4/4.
 */

public class Main {

    private static InputManager im;
    private static Method injectInputEventMethod;
    private static long downTime;

    private static IWindowManager wm;

    private static float scale = 1;

//    public static void main(String[] args) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException, IOException {
//
//        System.out.println("start!");
//        LocalServerSocket serverSocket = new LocalServerSocket("puppet-ver1");
//
//        init();
//
//        while (true) {
//            System.out.println("listen.....");
//            try {
//                LocalSocket socket = serverSocket.accept();
//                acceptConnect(socket);
//            } catch (Exception e) {
//                serverSocket = new LocalServerSocket("puppet-ver1");
//            }
//
//        }
//
//    }




    public static Bitmap screenshot() throws Exception {

        String surfaceClassName;
        Point size = SurfaceControlVirtualDisplayFactory.getCurrentDisplaySize(false);
        size.x *= scale;
        size.y *= scale;
        Bitmap b = null;
        if (Build.VERSION.SDK_INT <= 17) {
            surfaceClassName = "android.view.Surface";
        } else {
            surfaceClassName = "android.view.SurfaceControl";
            //  b = android.view.SurfaceControl.screenshot(size.x, size.y);
        }
        b = (Bitmap) Class.forName(surfaceClassName).getDeclaredMethod("screenshot", new Class[]{Integer.TYPE, Integer.TYPE}).invoke(null, new Object[]{Integer.valueOf(size.x), Integer.valueOf(size.y)});

        int rotation = wm.getDefaultDisplayRotation();

        if (rotation == 0) {
            return b;
        }
        Matrix m = new Matrix();
        if (rotation == 1) {
            m.postRotate(-90.0f);
        } else if (rotation == 2) {
            m.postRotate(-180.0f);
        } else if (rotation == 3) {
            m.postRotate(-270.0f);
        }
        return Bitmap.createBitmap(b, 0, 0, size.x, size.y, m, false);

    }


}

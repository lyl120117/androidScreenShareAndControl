package honeywell.com.myserver;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.hardware.input.InputManager;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.support.annotation.RequiresApi;
import android.support.v4.view.InputDeviceCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.IWindowManager;
import android.view.InputEvent;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import com.wanjian.puppet.Main;
import com.wanjian.puppet.SurfaceControlVirtualDisplayFactory;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.Enumeration;
import java.util.Vector;

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
public class ServerActivity extends AppCompatActivity {

    private static final int SCREEN_SHOT = 0;
    public static final String TAG = "ServerActivity";
    private static final String TAG_ERROR = "ERROR";

    MediaProjection mediaProjection;
    MediaProjectionManager projectionManager;
    int mResultCode;
    Intent mData;
    ImageReader imageReader;

    int width;
    int height;
    int dpi;

    String imageName;
    Bitmap bitmap;




    private static boolean isStarted = false;
    private static StringBuilder stringBuilder = new StringBuilder();
    private void sendMsg(String msg){
        stringBuilder.append(msg);
        stringBuilder.append("\n");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView tv = findViewById(R.id.msg);
                tv.setText(stringBuilder.toString());
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server);
        sendMsg(getHostIP());
        start();

        startHandler();

        DisplayMetrics metric = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metric);
//        width = metric.widthPixels;
//        height = metric.heightPixels;
        width = 540;
        height = 960;
        dpi = metric.densityDpi;

        projectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        StartRecorder(null);
    }


    public void acceptConnect(Socket socket) {
        System.out.println("accepted...");
        read(socket);
        write(socket);
    }

    BufferedOutputStream outputStream;
    private static final int MAX_SIZE = 3;
    Vector<Bitmap> vector = new Vector(MAX_SIZE);
    private HandlerThread handlerThread = new HandlerThread("socket_image");
    private Handler handler;
    private void startHandler(){
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper()){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                    if(vector.size() == 0){
                        return;
                    }
                    try{
                        long s1 = System.currentTimeMillis();
                        final int VERSION = 2;
                        Bitmap bitmap = vector.remove(0);
                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 10, byteArrayOutputStream);
                        long s2 = System.currentTimeMillis();

                        outputStream.write(VERSION);
                        writeInt(outputStream, byteArrayOutputStream.size());
                        outputStream.write(byteArrayOutputStream.toByteArray());
                        outputStream.flush();
                        long s3 = System.currentTimeMillis();
                        Log.w(TAG, "---write---     "+byteArrayOutputStream.size()+", s1="+(s2 - s1)+", s2="+(s3 - s2));
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                    handler.sendEmptyMessage(0);
            }
        };
    }

    private void write(final Socket socket) {
        new Thread() {
            @Override
            public void run() {
                super.run();
                try {
                    outputStream = new BufferedOutputStream(socket.getOutputStream());
                    while (true) {
                        long s1 = System.currentTimeMillis();
                        Bitmap bitmap = startCapture();
                        long s2 = System.currentTimeMillis();
                        if(bitmap == null){
                            Thread.sleep(5);
                            continue;
                        }
                        vector.add(bitmap);
                        if(vector.size() > MAX_SIZE){
                            vector.remove(0);
                        }

                        handler.sendEmptyMessage(0);


                        Log.w(TAG, "---capture---     "+vector.size()+", "
                                +bitmap.getWidth()+"/"+bitmap.getHeight()+", s1="+(s2 - s1));

//                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
//                        bitmap.compress(Bitmap.CompressFormat.JPEG, 10, byteArrayOutputStream);
//                        long s3 = System.currentTimeMillis();
//
//                        outputStream.write(VERSION);
//                        writeInt(outputStream, byteArrayOutputStream.size());
//                        outputStream.write(byteArrayOutputStream.toByteArray());
//                        outputStream.flush();
//                        long s4 = System.currentTimeMillis();
//                        Log.w(TAG, "---write---     "+byteArrayOutputStream.size()+", "+bitmap.getByteCount()
//                                +", "+bitmap.getWidth()+"/"+bitmap.getHeight()
//                                +", s1="+(s2 - s1)+", s2="+(s3 - s2)+", s3="+(s4 - s3));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    private static void writeInt(OutputStream outputStream, int v) throws IOException {
        outputStream.write(v >> 24);
        outputStream.write(v >> 16);
        outputStream.write(v >> 8);
        outputStream.write(v);
    }

    private void start(){
        if(isStarted){
           return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                isStarted = true;

                System.out.println("start!");
                sendMsg("start!");
                ServerSocket serverSocket = null;
                try {
                    serverSocket = new ServerSocket(8888);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                try {
                    init();

                    while (true) {
                        System.out.println("listen.....");
                        sendMsg("listen.....");
                        try {
                            Socket socket = serverSocket.accept();
                            acceptConnect(socket);
                            sendMsg("acceptConnect!");
                        } catch (Exception e) {

                            try {
                                Thread.sleep(1000);
                                serverSocket = new ServerSocket(8888);
                            } catch (Exception e1) {
                                e1.printStackTrace();
                            }
                        }

                    }
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
                isStarted = false;
                sendMsg("stop!");
                stringBuilder.delete(0, stringBuilder.length());
            }
        }).start();
    }


    /**
     * 获取ip地址
     * @return
     */
    public static String getHostIP() {

        String hostIp = null;
        try {
            Enumeration nis = NetworkInterface.getNetworkInterfaces();
            InetAddress ia = null;
            while (nis.hasMoreElements()) {
                NetworkInterface ni = (NetworkInterface) nis.nextElement();
                Enumeration<InetAddress> ias = ni.getInetAddresses();
                while (ias.hasMoreElements()) {
                    ia = ias.nextElement();
                    if (ia instanceof Inet6Address) {
                        continue;// skip ipv6
                    }
                    String ip = ia.getHostAddress();
                    if (!"127.0.0.1".equals(ip)) {
                        hostIp = ia.getHostAddress();
                        break;
                    }
                }
            }
        } catch (SocketException e) {
            Log.i("yao", "SocketException");
            e.printStackTrace();
        }
        return hostIp;

    }





    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == SCREEN_SHOT){
            if(resultCode == RESULT_OK){
                mResultCode = resultCode;
                mData = data;
                setUpMediaProjection();
                setUpVirtualDisplay();
            }
        }
    }

    private Bitmap startCapture() {
        imageName = System.currentTimeMillis() + ".png";
        Image image = imageReader.acquireLatestImage();
        if (image == null) {
            Log.e(TAG_ERROR, "image is null.");
            return null;
        }
        int width = image.getWidth();
        int height = image.getHeight();
        final Image.Plane[] planes = image.getPlanes();
        final ByteBuffer buffer = planes[0].getBuffer();
        int pixelStride = planes[0].getPixelStride();
        int rowStride = planes[0].getRowStride();
        int rowPadding = rowStride - pixelStride * width;
//        Log.w(TAG, "---startCapture---  "+planes.length+", width="+width+", height="+height
//            +", pixelStride="+pixelStride+", rowStride="+rowStride+", rowPadding="+rowPadding);
        bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_4444);
        bitmap.copyPixelsFromBuffer(buffer);
        image.close();
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height);
//        bitmap = resizeImage(bitmap, 540, 960);

        return bitmap;
    }

    public Bitmap resizeImage(Bitmap bitmap, int w, int h) {
        Bitmap BitmapOrg = bitmap;
        int width = BitmapOrg.getWidth();
        int height = BitmapOrg.getHeight();
        int newWidth = w;
        int newHeight = h;

        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;

        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        // if you want to rotate the Bitmap
        // matrix.postRotate(45);
        Bitmap resizedBitmap = Bitmap.createBitmap(BitmapOrg, 0, 0, width,
                height, matrix, true);
        return resizedBitmap;
    }


        private void setUpVirtualDisplay() {
        Log.w(TAG, "---setUpVirtualDisplay---");
        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 1);
        mediaProjection.createVirtualDisplay("ScreenShout",
                width,height,dpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.getSurface(),null,null);
    }

    private void setUpMediaProjection(){
        Log.w(TAG, "---setUpMediaProjection---");
        mediaProjection = projectionManager.getMediaProjection(mResultCode,mData);
    }

    public void StartRecorder(View view) {
        startActivityForResult(projectionManager.createScreenCaptureIntent(),
                SCREEN_SHOT);
    }




    public static void read(final Socket socket) {

        new Thread() {
            private final String DOWN = "DOWN";
            private final String MOVE = "MOVE";
            private final String UP = "UP";

            private final String MENU = "MENU";
            private final String HOME = "HOME";
            private final String BACK = "BACK";

            private final String DEGREE = "DEGREE";

            @Override
            public void run() {
                super.run();
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    while (true) {
                        String line;
                        try {
                            line = reader.readLine();
                            if (line == null) {
                                return;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            return;
                        }
//                        Log.w(TAG, "---read---   line="+line);
                        try {
                            if (line.startsWith(DOWN)) {
                                hanlerDown(line.substring(DOWN.length()));
                            } else if (line.startsWith(MOVE)) {
                                hanlerMove(line.substring(MOVE.length()));
                            } else if (line.startsWith(UP)) {
                                handlerUp(line.substring(UP.length()));
                            } else if (line.startsWith(MENU)) {
                                menu();
                            } else if (line.startsWith(HOME)) {
                                pressHome();
                            } else if (line.startsWith(BACK)) {
                                back();
                            } else if (line.startsWith(DEGREE)) {
                                scale = Float.parseFloat(line.substring(DEGREE.length())) / 100;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }


                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }.start();
    }

    private static void handlerUp(String line) {
        Point point = getXY(line);
        if (point != null) {
            try {
                touchUp(point.x, point.y);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static void hanlerMove(String line) {
        Point point = getXY(line);
        if (point != null) {
            try {
                touchMove(point.x, point.y);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static void hanlerDown(String line) {
        Point point = getXY(line);
        if (point != null) {
            try {
                touchDown(point.x, point.y);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    private static Point getXY(String nums) {
        try {
            Point point = SurfaceControlVirtualDisplayFactory.getCurrentDisplaySize(false);
            String[] s = nums.split("#");
            float scaleX = Float.parseFloat(s[0]);
            float scaleY = Float.parseFloat(s[1]);
            point.x *= scaleX;
            point.y *= scaleY;
            return point;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    private static InputManager im;
    private static Method injectInputEventMethod;
    private static long downTime;
    private static float scale = 1;

    private static IWindowManager wm;
    public static void init() throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        Method getServiceMethod = Class.forName("android.os.ServiceManager").getDeclaredMethod("getService", new Class[]{String.class});
        wm = IWindowManager.Stub.asInterface((IBinder) getServiceMethod.invoke(null, new Object[]{"window"}));

        im = (InputManager) InputManager.class.getDeclaredMethod("getInstance", new Class[0]).invoke(null, new Object[0]);
        MotionEvent.class.getDeclaredMethod("obtain", new Class[0]).setAccessible(true);
        injectInputEventMethod = InputManager.class.getMethod("injectInputEvent", new Class[]{InputEvent.class, Integer.TYPE});

    }

    private static void menu() throws InvocationTargetException, IllegalAccessException {
        sendKeyEvent(im, injectInputEventMethod, InputDeviceCompat.SOURCE_KEYBOARD, KeyEvent.KEYCODE_MENU, false);
    }

    private static void back() throws InvocationTargetException, IllegalAccessException {
        sendKeyEvent(im, injectInputEventMethod, InputDeviceCompat.SOURCE_KEYBOARD, 4, false);
    }


    private static void touchUp(float clientX, float clientY) throws InvocationTargetException, IllegalAccessException {
        injectMotionEvent(im, injectInputEventMethod, InputDeviceCompat.SOURCE_TOUCHSCREEN, 1, downTime, SystemClock.uptimeMillis(), clientX, clientY, 1.0f);
    }

    private static void touchMove(float clientX, float clientY) throws InvocationTargetException, IllegalAccessException {
        injectMotionEvent(im, injectInputEventMethod, InputDeviceCompat.SOURCE_TOUCHSCREEN, 2, downTime, SystemClock.uptimeMillis(), clientX, clientY, 1.0f);
    }

    private static void touchDown(float clientX, float clientY) throws InvocationTargetException, IllegalAccessException {
        downTime = SystemClock.uptimeMillis();
        injectMotionEvent(im, injectInputEventMethod, InputDeviceCompat.SOURCE_TOUCHSCREEN, 0, downTime, downTime, clientX, clientY, 1.0f);

    }


    private static void pressHome() throws InvocationTargetException, IllegalAccessException {
        sendKeyEvent(im, injectInputEventMethod, InputDeviceCompat.SOURCE_KEYBOARD, 3, false);
    }


    private static void injectMotionEvent(InputManager im, Method injectInputEventMethod, int inputSource, int action, long downTime, long eventTime, float x, float y, float pressure) throws InvocationTargetException, IllegalAccessException {
        MotionEvent event = MotionEvent.obtain(downTime, eventTime, action, x, y, pressure, 1.0f, 0, 1.0f, 1.0f, 0, 0);
        event.setSource(inputSource);
        injectInputEventMethod.invoke(im, new Object[]{event, Integer.valueOf(0)});
    }

    private static void injectKeyEvent(InputManager im, Method injectInputEventMethod, KeyEvent event) throws InvocationTargetException, IllegalAccessException {
        injectInputEventMethod.invoke(im, new Object[]{event, Integer.valueOf(0)});
    }


    private static void sendKeyEvent(InputManager im, Method injectInputEventMethod, int inputSource, int keyCode, boolean shift) throws InvocationTargetException, IllegalAccessException {
        long now = SystemClock.uptimeMillis();
        int meta = shift ? 1 : 0;
        injectKeyEvent(im, injectInputEventMethod, new KeyEvent(now, now, 0, keyCode, 0, meta, -1, 0, 0, inputSource));
        injectKeyEvent(im, injectInputEventMethod, new KeyEvent(now, now, 1, keyCode, 0, meta, -1, 0, 0, inputSource));
    }


}

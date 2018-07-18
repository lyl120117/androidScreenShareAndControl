package honeywell.com.myserver;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.wanjian.puppet.Main;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.Enumeration;

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



        DisplayMetrics metric = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metric);
        width = metric.widthPixels;
        height = metric.heightPixels;
        dpi = metric.densityDpi;

        projectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        StartRecorder(null);
    }


    public void acceptConnect(Socket socket) {
        System.out.println("accepted...");
        Main.read(socket);
        write(socket);
    }

    private void write(final Socket socket) {
        new Thread() {
            @Override
            public void run() {
                super.run();
                try {
                    final int VERSION = 2;
                    BufferedOutputStream outputStream = new BufferedOutputStream(socket.getOutputStream());
                    while (true) {
                        Bitmap bitmap = startCapture();

                        if(bitmap == null){
                            Thread.sleep(10);
                            continue;
                        }
                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 60, byteArrayOutputStream);

                        outputStream.write(VERSION);
                        Log.w(TAG, "---write---     "+byteArrayOutputStream.size()
                                +", "+bitmap.getWidth()+"/"+bitmap.getHeight());
                        writeInt(outputStream, byteArrayOutputStream.size());
                        outputStream.write(byteArrayOutputStream.toByteArray());
                        outputStream.flush();
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
                    Main.init();

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
        Log.w(TAG, "---startCapture---  "+planes.length+", width="+width+", height="+height
            +", pixelStride="+pixelStride+", rowStride="+rowStride+", rowPadding="+rowPadding);
        bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_4444);
        bitmap.copyPixelsFromBuffer(buffer);
        image.close();
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height);

        return bitmap;
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



}

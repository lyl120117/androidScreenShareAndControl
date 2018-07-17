package honeywell.com.myserver;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.os.Build;
import android.os.Environment;
import android.os.SystemClock;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.wanjian.puppet.Main;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
public class MainActivity extends AppCompatActivity {

    private static final int SCREEN_SHOT = 0;
    public static final String TAG = "MainActivity";
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
        setContentView(R.layout.activity_main);
        start();



        DisplayMetrics metric = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metric);
        width = metric.widthPixels;
        height = metric.heightPixels;
        dpi = metric.densityDpi;

        projectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        StartRecorder(null);
    }


    public void acceptConnect(LocalSocket socket) {
        System.out.println("accepted...");
        Main.read(socket);
        write(socket);
    }

    private void write(final LocalSocket socket) {
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

                        outputStream.write(2);
                        Log.w(TAG, "---write---     "+byteArrayOutputStream.size());
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
                LocalServerSocket serverSocket = null;
                try {
                    serverSocket = new LocalServerSocket("puppet-ver1");
                } catch (IOException e) {
                    e.printStackTrace();
                }

                try {
                    Main.init();

                    while (true) {
                        System.out.println("listen.....");
                        sendMsg("listen.....");
                        try {
                            LocalSocket socket = serverSocket.accept();
                            acceptConnect(socket);
                            sendMsg("acceptConnect!");
                        } catch (Exception e) {
                            try {
                                serverSocket = new LocalServerSocket("puppet-ver1");
                            } catch (IOException e1) {
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
        Image image = imageReader.acquireNextImage();
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
        bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(buffer);
        image.close();

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

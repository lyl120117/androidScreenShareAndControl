package honeywell.com.myserver;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
public class ClientActivity extends AppCompatActivity {
    public static final String TAG = ClientActivity.class.getSimpleName();
    private boolean started;
    private MyImage myImage;
    HandlerThread handlerThread = new HandlerThread("touch");
    private Handler handler;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);// 设置全屏
        setContentView(R.layout.activity_client);
        setToolsVisible(true);
        setupView();
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
    }

    private void setupView(){
        myImage = findViewById(R.id.img);
    }

    private void setToolsVisible(boolean visible){
        findViewById(R.id.tools).setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        started = false;
    }

    private void setImage(final Bitmap b){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                myImage.setBackground(new BitmapDrawable(b));
//                iv.setImageBitmap(b);
            }
        });
    }


    public void onConnect(View view) {
        EditText et = findViewById(R.id.ip);
        String ip = et.getText().toString();
        et = findViewById(R.id.port);
        String port = et.getText().toString();
        setToolsVisible(false);
        try {
            read(ip, port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    BufferedWriter writer;

    private void read(final String ip, final String port) throws IOException {
        new Thread() {
            @Override
            public void run() {
                super.run();
                try {
                    Socket socket = new Socket(ip, Integer.parseInt(port));
                    BufferedInputStream inputStream = new BufferedInputStream(socket.getInputStream());
                    writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                    myImage.writer = writer;
                    myImage.handler = handler;
                    started = true;
                    byte[] bytes = null;
                    while (started) {
                        long s1 = System.currentTimeMillis();
                        int version = inputStream.read();
                        if (version == -1) {
                            return;
                        }
                        int length = readInt(inputStream);
                        if (bytes == null) {
                            bytes = new byte[length];
                        }
                        if (bytes.length < length) {
                            bytes = new byte[length];
                        }
                        int read = 0;
                        while ((read < length)) {
                            read += inputStream.read(bytes, read, length - read);
                        }
                        InputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
                        long s2 = System.currentTimeMillis();
                        Bitmap bitmap = BitmapFactory.decodeStream(byteArrayInputStream);
                        setImage(bitmap);
                        long s3 = System.currentTimeMillis();

                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();

    }


    private int readInt(InputStream inputStream) throws IOException {
        int b1 = inputStream.read();
        int b2 = inputStream.read();
        int b3 = inputStream.read();
        int b4 = inputStream.read();

        return (b1 << 24) | (b2 << 16) | (b3 << 8) | b4;
    }

    public void onBack(View view) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    writer.write("BACK");
                    writer.newLine();
                    writer.flush();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void onHome(View view) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    writer.write("HOME");
                    writer.newLine();
                    writer.flush();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void onSwitch(View view) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    writer.write("MENU");
                    writer.newLine();
                    writer.flush();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
}

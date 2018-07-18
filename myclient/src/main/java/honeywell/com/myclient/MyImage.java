package honeywell.com.myclient;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.ImageView;

import java.io.BufferedWriter;

@SuppressLint("AppCompatCustomView")
public class MyImage extends ImageView {
    BufferedWriter writer;
    Handler handler;
    private boolean isMove;
    public MyImage(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

    }

    @Override
    public boolean onTouchEvent(final MotionEvent event) {
        if(writer != null){
//            float x = event.getX();
//            float y = event.getY();
//            Log.w(ClientActivity.TAG, "onTouchEvent   x="+x+", y="+y+", "+getWidth()+"/"+getHeight());
//            try {
//                writer.write("DOWN" + (x * 1.0f / getWidth()) + "#" + (y * 1.0f / getHeight()));
//                writer.newLine();
//                writer.write("UP" + (x * 1.0f / getWidth()) + "#" + (y * 1.0f / getHeight()));
//                writer.newLine();
//                writer.flush();
//            } catch (Exception e) {
//
//            }

            final float x = event.getX();
            final float y = event.getY();
//            Log.w(ClientActivity.TAG, "onTouchEvent   x="+x+", y="+y+", "+getWidth()+"/"+getHeight());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        switch (event.getAction()){
                            case MotionEvent.ACTION_DOWN:
                                writer.write("DOWN" + (x * 1.0f / getWidth()) + "#" + (y * 1.0f / getHeight()));
                                break;
                            case MotionEvent.ACTION_MOVE:
                                writer.write("MOVE" + (x * 1.0f / getWidth()) + "#" + (y * 1.0f / getHeight()));
                                break;
                            case MotionEvent.ACTION_UP:
                                writer.write("UP" + (x * 1.0f / getWidth()) + "#" + (y * 1.0f / getHeight()));
                                break;
                        }
                        writer.newLine();
                        writer.flush();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
        return true;
    }
}

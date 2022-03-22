package com.example.recorddemo;

import static android.content.pm.PackageManager.PERMISSION_DENIED;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.recorddemo.utils.AppUtils;
import com.example.recorddemo.utils.ScreenUtils;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private TextView mTvStart;
    private TextView mTvEnd;
    private TextView mTvFloat;

    private TextView mTvTime;

    private TextView mTvFloatTime;
    private ImageView mIvFloatPlay;

    private int REQUEST_CODE = 1;
    private int REQUEST_CODE_FLOAT = 2;

    private WindowManager windowManager;
    private WindowManager.LayoutParams layoutParams;
    private View floatView;
    private ScaleAnimation buttonScale;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        CommonUtil.init(this);
        PermissionUtils.checkPermission(this);
        mTvStart = findViewById(R.id.tv_start);
        mTvStart.setOnClickListener(this);

        mTvTime = findViewById(R.id.tv_record_time);

        mTvEnd = findViewById(R.id.tv_end);
        mTvEnd.setOnClickListener(this);
        mTvFloat = findViewById(R.id.tv_float);
        mTvFloat.setOnClickListener(this);

        startScreenRecordService();

    }

    private ServiceConnection mServiceConnection;

    /**
     * 开启录制 Service
     */
    private void startScreenRecordService(){

        mServiceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                ScreenRecordService.RecordBinder recordBinder = (ScreenRecordService.RecordBinder) service;
                ScreenRecordService screenRecordService = recordBinder.getRecordService();
                screenRecordService.setNotificationEngine(notificationEngine);
                ScreenUtil.setScreenService(screenRecordService);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {

            }
        };

        Intent intent = new Intent(this, ScreenRecordService.class);
        bindService(intent, mServiceConnection, BIND_AUTO_CREATE);

        ScreenUtil.addRecordListener(recordListener);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        for (int temp : grantResults) {
            if (temp == PERMISSION_DENIED) {
                AlertDialog dialog = new AlertDialog.Builder(this).setTitle("申请权限").setMessage("这些权限很重要").setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(MainActivity.this,"cancel",Toast.LENGTH_LONG).show();
                    }
                }).setPositiveButton("设置", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent();
                        intent.setAction(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        intent.setData(Uri.parse("package:" + MainActivity.this.getPackageName()));
                        MainActivity.this.startActivity(intent);
                    }
                }).create();
                dialog.show();
                break;
            }
        }
    }

    private ScreenUtil.RecordListener recordListener = new ScreenUtil.RecordListener() {
        @Override
        public void onStartRecord() {
            if(mIvFloatPlay!=null){

                mIvFloatPlay.setImageResource(R.drawable.icon_play_pause);
            }
        }

        @Override
        public void onPauseRecord() {
            if(mIvFloatPlay!=null){

                mIvFloatPlay.setImageResource(R.drawable.icon_play_play);
            }
        }

        @Override
        public void onResumeRecord() {
            if(mIvFloatPlay!=null){

                mIvFloatPlay.setImageResource(R.drawable.icon_play_pause);
            }
        }

        @Override
        public void onStopRecord(String stopTip) {
            Toast.makeText(MainActivity.this,stopTip,Toast.LENGTH_LONG).show();
            if(mIvFloatPlay!=null){
                mIvFloatPlay.setImageResource(R.drawable.icon_play_play);
            }
            if(mTvFloatTime!=null){
                mTvFloatTime.setText(R.string.start);
            }
        }

        @Override
        public void onRecording(String timeTip) {
            mTvTime.setText(timeTip);
            updateFloat(timeTip);
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK){
            try {
                ScreenUtil.setUpData(resultCode,data);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }else if(requestCode == REQUEST_CODE_FLOAT&& resultCode == Activity.RESULT_OK){
            addFloat();
        } else {
            Toast.makeText(MainActivity.this,"拒绝录屏",Toast.LENGTH_LONG).show();
        }
    }


    @Override
    public void onClick(View v) {

        switch (v.getId()){
            case R.id.tv_start:{
                ScreenUtil.startScreenRecord(this,REQUEST_CODE);
                break;
            }
            case R.id.tv_end:{
                ScreenUtil.stopScreenRecord(this);
                break;
            }
            case R.id.tv_float:{
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (!Settings.canDrawOverlays(this)) {
                        startActivityForResult(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())), REQUEST_CODE_FLOAT);
                    } else {
                        addFloat();
                    }
                }
                break;
            }
        }

    }
    private MediaProjectionNotificationEngine notificationEngine=new MediaProjectionNotificationEngine() {
        @Override
        public Notification getNotification() {
            String title = "启动媒体投影服务";
            return NotificationHelper.getInstance().createSystem()
                    .setOngoing(true)// 常驻通知栏
                    .setTicker(title)
                    .setContentText(title)
                    .setDefaults(Notification.DEFAULT_ALL)
                    .build();
        }
    };

    private  void addFloat(){
        if(windowManager!=null&&floatView!=null){
            return;
        }
        //加一点简单的动画 让效果更漂亮点
        buttonScale = (ScaleAnimation) AnimationUtils.loadAnimation(this, R.anim.anim_float);
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        layoutParams = new WindowManager.LayoutParams();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        }
        layoutParams.format = PixelFormat.RGBA_8888;
        layoutParams.gravity = Gravity.LEFT | Gravity.TOP;
        layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        layoutParams.width = ScreenUtils.dp2px(100);
        layoutParams.height = ScreenUtils.dp2px(50);
        layoutParams.x = ScreenUtils.getRealWidth() - ScreenUtils.dp2px(60);
        layoutParams.y = ScreenUtils.deviceHeight() * 2 / 3;

        LayoutInflater layoutInflater = LayoutInflater.from(this);
        floatView = layoutInflater.inflate(R.layout.view_float, null);
        LinearLayout rlFloatParent = floatView.findViewById(R.id.float_root);
        rlFloatParent.startAnimation(buttonScale);
        mTvFloatTime = floatView.findViewById(R.id.tv_float_time);
        mIvFloatPlay=floatView.findViewById(R.id.iv_play);
        floatView.findViewById(R.id.iv_close_float).setOnClickListener(v -> removeFloat());
        floatView.setOnTouchListener(new FloatingOnTouchListener());
        windowManager.addView(floatView, layoutParams);
        mIvFloatPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });
    }

    private void removeFloat(){
        if(windowManager!=null&&floatView!=null){
            windowManager.removeView(floatView);
            floatView=null;
        }
    }

    private void updateFloat(String timeTip){
        if(floatView==null||mTvFloatTime==null){
            return;
        }

        mTvFloatTime.setText(timeTip);
    }

    private class FloatingOnTouchListener implements View.OnTouchListener {
        private int x;
        private int y;
        private long downTime;

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View view, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    downTime = System.currentTimeMillis();
                    x = (int) event.getRawX();
                    y = (int) event.getRawY();
                    break;
                case MotionEvent.ACTION_MOVE:
                    int nowX = (int) event.getRawX();
                    int nowY = (int) event.getRawY();
                    int movedX = nowX - x;
                    int movedY = nowY - y;
                    x = nowX;
                    y = nowY;
                    layoutParams.x = layoutParams.x + movedX;
                    layoutParams.y = layoutParams.y + movedY;
                    windowManager.updateViewLayout(view, layoutParams);
                    break;
                case MotionEvent.ACTION_UP:
                    /* *
                     * 这里根据手指按下和抬起的时间差来判断点击事件还是滑动事件
                     * */
                    if ((System.currentTimeMillis() - downTime) < 200) {
                        if (AppUtils.isAppIsInBackground()) {
                            AppUtils.moveToFront(MainActivity.class);
                        }
                    }
                    break;
                default:
                    break;
            }
            return false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        removeFloat();
    }
}
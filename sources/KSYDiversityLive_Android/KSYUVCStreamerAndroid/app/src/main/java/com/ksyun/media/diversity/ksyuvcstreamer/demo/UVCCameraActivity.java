package com.ksyun.media.diversity.ksyuvcstreamer.demo;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.SensorManager;
import android.hardware.usb.UsbDevice;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.GsonBuilder;
import com.ksyun.media.diversity.ksyuvcstreamer.R;
import com.ksyun.media.diversity.ksyuvcstreamer.kit.KSYUVCStreamer;
import com.ksyun.media.streamer.encoder.VideoEncodeFormat;
import com.ksyun.media.streamer.filter.imgtex.ImgTexFilterBase;
import com.ksyun.media.streamer.filter.imgtex.ImgTexFilterMgt;
import com.ksyun.media.streamer.kit.KSYStreamer;
import com.ksyun.media.streamer.kit.StreamerConstants;
import com.serenegiant.usb.DeviceFilter;
import com.serenegiant.usb.USBMonitor;

import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Base streaming activity.
 */

public class UVCCameraActivity extends Activity implements
        ActivityCompat.OnRequestPermissionsResultCallback {
    public static final String TAG = "UVCCameraActivity";

    protected static final int PERMISSION_REQUEST_START_STREAM = 1;
    protected static final String START_STREAM = "????????????";
    protected static final String STOP_STREAM = "????????????";

    @BindView(R.id.gl_surface_view)
    protected GLSurfaceView mGLSurfaceView;
    @BindView(R.id.chronometer)
    protected Chronometer mChronometer;
    @BindView(R.id.url)
    protected TextView mUrlTextView;
    @BindView(R.id.start_stream_tv)
    protected TextView mStreamingText;
    @BindView(R.id.debug_info)
    protected TextView mDebugInfoTextView;

    protected BaseStreamConfig mConfig;
    protected boolean mIsLandscape;
    protected boolean mStreaming;
    protected boolean mIsChronometerStarted;
    protected String mDebugInfo = "";
    protected boolean mHWEncoderUnsupported;
    protected boolean mSWEncoderUnsupported;

    protected USBMonitor mUSBMonitor;
    protected KSYUVCStreamer mStreamer;
    protected Handler mMainHandler;
    protected Timer mTimer;

    protected int mLastRotation;
    protected OrientationEventListener mOrientationEventListener;

    protected String mSdcardPath = Environment.getExternalStorageDirectory().getPath();
    protected String mLogoPath = "file://" + mSdcardPath + "/test.png";
    protected String mBgImagePath = "assets://bg.jpg";

    public static class BaseStreamConfig {
        public String mUrl;
        public float mFrameRate;
        public int mVideoKBitrate;
        public int mAudioKBitrate;
        public int mTargetResolution;
        public int mOrientation;
        public int mEncodeMethod;
        public boolean mAutoStart;
        public boolean mShowDebugInfo;

        public BaseStreamConfig fromJson(String json) {
            return new GsonBuilder().create().fromJson(json, this.getClass());
        }

        public String toJson() {
            return new GsonBuilder().create().toJson(this);
        }
    }

    public static void startActivity(Context context, BaseStreamConfig config, Class<?> cls) {
        Intent intent = new Intent(context, cls);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("config", config.toJson());
        context.startActivity(intent);
    }

    protected BaseStreamConfig getConfig(Bundle bundle) {
        return new BaseStreamConfig().fromJson(bundle.getString("config"));
    }

    protected int getLayoutId() {
        return R.layout.uvc_camera_activity;
    }

    protected void setDisplayPreview() {
        mStreamer.setDisplayPreview(mGLSurfaceView);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(getLayoutId());
        ButterKnife.bind(this);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mMainHandler = new Handler();
        mUSBMonitor = new USBMonitor(this, mOnDeviceConnectListener);
        mUSBMonitor.register();
        mStreamer = new KSYUVCStreamer(this);
        mConfig = getConfig(getIntent().getExtras());
        initUI();
        config();
        enableBeautyFilter();
        showWaterMark();

        // ????????????????????????
        if (mConfig.mAutoStart) {
            startStreamWithPermCheck();
        }
        // ????????????????????????
        if (mConfig.mShowDebugInfo) {
            startDebugInfoTimer();
        }
    }

    protected void initUI() {
        // empty here
    }

    protected void config() {
        // ????????????URL??????
        if (!TextUtils.isEmpty(mConfig.mUrl)) {
            mUrlTextView.setText(mConfig.mUrl);
            mStreamer.setUrl(mConfig.mUrl);
        }

        // ?????????????????????
        mStreamer.setCameraCaptureResolution(mConfig.mTargetResolution);
        mStreamer.setPreviewResolution(mConfig.mTargetResolution);
        mStreamer.setTargetResolution(mConfig.mTargetResolution);

        // ???????????????????????????????????????
        mStreamer.setEncodeMethod(mConfig.mEncodeMethod);
        // ??????????????????????????????????????????(high profile)
        if (mConfig.mEncodeMethod == StreamerConstants.ENCODE_METHOD_HARDWARE) {
            mStreamer.setVideoEncodeProfile(VideoEncodeFormat.ENCODE_PROFILE_HIGH_PERFORMANCE);
        }

        // ??????????????????
        if (mConfig.mFrameRate > 0) {
            mStreamer.setPreviewFps(mConfig.mFrameRate);
            mStreamer.setTargetFps(mConfig.mFrameRate);
        }

        // ??????????????????????????????????????????????????????????????????????????????????????????
        int videoBitrate = mConfig.mVideoKBitrate;
        if (videoBitrate > 0) {
            mStreamer.setVideoKBitrate(videoBitrate * 3 / 4, videoBitrate, videoBitrate / 4);
        }

        // ??????????????????
        if (mConfig.mAudioKBitrate > 0) {
            mStreamer.setAudioKBitrate(mConfig.mAudioKBitrate);
        }

        // ????????????????????????????????????????????????
        if (mConfig.mOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            mIsLandscape = true;
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            mStreamer.setRotateDegrees(90);
        } else if (mConfig.mOrientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
            mIsLandscape = false;
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            mStreamer.setRotateDegrees(0);
        } else if (mConfig.mOrientation == ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR) {
            mOrientationEventListener = new StdOrientationEventListener(this,
                    SensorManager.SENSOR_DELAY_NORMAL);
        }

        // ????????????View
        setDisplayPreview();
        // ????????????????????????
        mStreamer.setOnInfoListener(mOnInfoListener);
        mStreamer.setOnErrorListener(mOnErrorListener);
        // ???????????????????????????????????????????????????????????????????????????????????????????????????????????????
        mStreamer.setEnableRepeatLastFrame(false);
    }

    protected void handleOnResume() {
        // ????????????????????????
        if (mOrientationEventListener != null) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
            if (mOrientationEventListener.canDetectOrientation()) {
                mOrientationEventListener.enable();
            }
            int rotation = getDisplayRotation();
            mIsLandscape = (rotation % 180) != 0;
            mStreamer.setRotateDegrees(rotation);
            mLastRotation = rotation;
        }

        // ??????KSYStreamer???onResume??????
        mStreamer.onResume();
        // ?????????????????????
        mStreamer.stopImageCapture();
        // ?????????????????????
        //startCameraPreviewWithPermCheck();
        // ??????onPause????????????DummyAudio???????????????????????????
        mStreamer.setUseDummyAudioCapture(false);
    }

    protected void handleOnPause() {
        // ????????????????????????
        if (mOrientationEventListener != null) {
            mOrientationEventListener.disable();
        }

        // ??????KSYStreamer???onPause??????
        mStreamer.onPause();
        // ??????????????????????????????????????????????????????????????????????????????????????????
        mStreamer.stopCameraPreview();
        mStreamer.startImageCapture(mBgImagePath);
        // ????????????App?????????????????????????????????????????????????????????????????????DummyAudio?????????
        // ??????????????????mic??????????????????????????????????????????????????????mic??????
        mStreamer.setUseDummyAudioCapture(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        handleOnResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        handleOnPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // ??????????????????
        mUSBMonitor.unregister();
        if (mMainHandler != null) {
            mMainHandler.removeCallbacksAndMessages(null);
            mMainHandler = null;
        }
        if (mTimer != null) {
            mTimer.cancel();
        }
        mStreamer.release();
    }

    //start streaming
    protected void startStream() {
        mStreamer.startStream();
        mStreamingText.setText(STOP_STREAM);
        mStreamingText.postInvalidate();
        mStreaming = true;
    }

    // stop streaming
    protected void stopStream() {
        mStreamer.stopStream();
        mStreamingText.setText(START_STREAM);
        mStreamingText.postInvalidate();
        mStreaming = false;
        stopChronometer();
    }

    protected void showWaterMark() {
        if (!mIsLandscape) {
            mStreamer.showWaterMarkLogo(mLogoPath, 0.08f, 0.04f, 0.20f, 0, 0.8f);
            mStreamer.showWaterMarkTime(0.03f, 0.01f, 0.35f, Color.WHITE, 1.0f);
        } else {
            mStreamer.showWaterMarkLogo(mLogoPath, 0.05f, 0.09f, 0, 0.20f, 0.8f);
            mStreamer.showWaterMarkTime(0.01f, 0.03f, 0.22f, Color.WHITE, 1.0f);
        }
    }

    protected void hideWaterMark() {
        mStreamer.hideWaterMarkLogo();
        mStreamer.hideWaterMarkTime();
    }

    protected void enableBeautyFilter() {
        // ?????????????????????????????????????????????????????????????????????????????????
        mStreamer.getImgTexFilterMgt().setOnErrorListener(new ImgTexFilterBase.OnErrorListener() {
            @Override
            public void onError(ImgTexFilterBase filter, int errno) {
                Toast.makeText(UVCCameraActivity.this, "??????????????????????????????",
                        Toast.LENGTH_SHORT).show();
                mStreamer.getImgTexFilterMgt().setFilter(mStreamer.getGLRender(),
                        ImgTexFilterMgt.KSY_FILTER_BEAUTY_DISABLE);
            }
        });
        // ??????????????????????????????????????????????????????????????????????????????????????????demo
        mStreamer.getImgTexFilterMgt().setFilter(mStreamer.getGLRender(),
                ImgTexFilterMgt.KSY_FILTER_BEAUTY_PRO3);
    }

    private void startDebugInfoTimer() {
        if (mTimer == null) {
            mTimer = new Timer();
            mTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    updateDebugInfo();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mDebugInfoTextView.setText(mDebugInfo);
                        }
                    });
                }
            }, 1000, 1000);
        }
    }

    // update debug info
    private void updateDebugInfo() {
        if (mStreamer == null) return;
        String encodeMethod;
        switch (mStreamer.getVideoEncodeMethod()) {
            case StreamerConstants.ENCODE_METHOD_HARDWARE: encodeMethod = "HW"; break;
            case StreamerConstants.ENCODE_METHOD_SOFTWARE: encodeMethod = "SW"; break;
            default: encodeMethod = "SW1"; break;
        }
        mDebugInfo = String.format(Locale.getDefault(), " " +
                        "EncodeMethod=%s PreviewFps=%.2f \n " +
                        "RtmpHostIP()=%s DroppedFrameCount()=%d \n " +
                        "ConnectTime()=%dms DnsParseTime()=%dms \n " +
                        "UploadedKB()=%d EncodedFrames()=%d \n " +
                        "CurrentKBitrate=%d Version()=%s",
                encodeMethod, mStreamer.getCurrentPreviewFps(),
                mStreamer.getRtmpHostIP(), mStreamer.getDroppedFrameCount(),
                mStreamer.getConnectTime(), mStreamer.getDnsParseTime(),
                mStreamer.getUploadedKBytes(), mStreamer.getEncodedFrames(),
                mStreamer.getCurrentUploadKBitrate(), KSYStreamer.getVersion());
    }

    @OnClick(R.id.start_stream_tv)
    protected void onStartStreamClick() {
        if (mStreaming) {
            stopStream();
        } else {
            startStreamWithPermCheck();
        }
    }

    protected void startChronometer() {
        mChronometer.setBase(SystemClock.elapsedRealtime());
        mChronometer.start();
        mIsChronometerStarted = true;
    }

    protected void stopChronometer() {
        mChronometer.setBase(SystemClock.elapsedRealtime());
        mChronometer.stop();
        mIsChronometerStarted = false;
    }

    protected void onStreamerInfo(int what, int msg1, int msg2) {
        Log.d(TAG, "OnInfo: " + what + " msg1: " + msg1 + " msg2: " + msg2);
        switch (what) {
            case StreamerConstants.KSY_STREAMER_CAMERA_INIT_DONE:
                Log.d(TAG, "KSY_STREAMER_CAMERA_INIT_DONE");
                break;
            case StreamerConstants.KSY_STREAMER_CAMERA_FACING_CHANGED:
                Log.d(TAG, "KSY_STREAMER_CAMERA_FACING_CHANGED");
                // check is flash torch mode supported
                //mFlashView.setEnabled(mStreamer.getCameraCapture().isTorchSupported());
                break;
            case StreamerConstants.KSY_STREAMER_OPEN_STREAM_SUCCESS:
                Log.d(TAG, "KSY_STREAMER_OPEN_STREAM_SUCCESS");
                mStreamingText.setText(STOP_STREAM);
                startChronometer();
                break;
            case StreamerConstants.KSY_STREAMER_FRAME_SEND_SLOW:
                Log.d(TAG, "KSY_STREAMER_FRAME_SEND_SLOW " + msg1 + "ms");
                Toast.makeText(UVCCameraActivity.this, "Network not good!",
                        Toast.LENGTH_SHORT).show();
                break;
            case StreamerConstants.KSY_STREAMER_EST_BW_RAISE:
                Log.d(TAG, "BW raise to " + msg1 / 1000 + "kbps");
                break;
            case StreamerConstants.KSY_STREAMER_EST_BW_DROP:
                Log.d(TAG, "BW drop to " + msg1 / 1000 + "kpbs");
                break;
            default:
                break;
        }
    }

    protected void onStreamerError(int what, int msg1, int msg2) {
        Log.e(TAG, "streaming error: what=" + what + " msg1=" + msg1 + " msg2=" + msg2);
        switch (what) {
            case StreamerConstants.KSY_STREAMER_AUDIO_RECORDER_ERROR_START_FAILED:
            case StreamerConstants.KSY_STREAMER_AUDIO_RECORDER_ERROR_UNKNOWN:
                break;
            case StreamerConstants.KSY_STREAMER_CAMERA_ERROR_UNKNOWN:
            case StreamerConstants.KSY_STREAMER_CAMERA_ERROR_START_FAILED:
            case StreamerConstants.KSY_STREAMER_CAMERA_ERROR_EVICTED:
            case StreamerConstants.KSY_STREAMER_CAMERA_ERROR_SERVER_DIED:
                mStreamer.stopCameraPreview();
                break;
            case StreamerConstants.KSY_STREAMER_VIDEO_ENCODER_ERROR_UNSUPPORTED:
            case StreamerConstants.KSY_STREAMER_VIDEO_ENCODER_ERROR_UNKNOWN:
                handleEncodeError();
            default:
                reStreaming(what);
                break;
        }
    }

    protected void reStreaming(int err) {
        stopStream();
        mMainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                startStream();
            }
        }, 3000);
    }

    protected void handleEncodeError() {
        int encodeMethod = mStreamer.getVideoEncodeMethod();
        if (encodeMethod == StreamerConstants.ENCODE_METHOD_HARDWARE) {
            mHWEncoderUnsupported = true;
            if (mSWEncoderUnsupported) {
                mStreamer.setEncodeMethod(
                        StreamerConstants.ENCODE_METHOD_SOFTWARE_COMPAT);
                Log.e(TAG, "Got HW encoder error, switch to SOFTWARE_COMPAT mode");
            } else {
                mStreamer.setEncodeMethod(StreamerConstants.ENCODE_METHOD_SOFTWARE);
                Log.e(TAG, "Got HW encoder error, switch to SOFTWARE mode");
            }
        } else if (encodeMethod == StreamerConstants.ENCODE_METHOD_SOFTWARE) {
            mSWEncoderUnsupported = true;
            mStreamer.setEncodeMethod(StreamerConstants.ENCODE_METHOD_SOFTWARE_COMPAT);
            Log.e(TAG, "Got SW encoder error, switch to SOFTWARE_COMPAT mode");
        }
    }

    private KSYStreamer.OnInfoListener mOnInfoListener = new KSYStreamer.OnInfoListener() {
        @Override
        public void onInfo(int what, int msg1, int msg2) {
            onStreamerInfo(what, msg1, msg2);
        }
    };

    private KSYStreamer.OnErrorListener mOnErrorListener = new KSYStreamer.OnErrorListener() {
        @Override
        public void onError(int what, int msg1, int msg2) {
            onStreamerError(what, msg1, msg2);
        }
    };

    protected void startCameraPreviewWithPermCheck() {
        final List<DeviceFilter> filter = DeviceFilter.getDeviceFilters(this, R.xml.device_filter);
        final List<UsbDevice> deviceList = mUSBMonitor.getDeviceList(filter.get(0));
        if (deviceList != null && deviceList.size() > 0) {
            mUSBMonitor.requestPermission(deviceList.get(0));
        }
    }

    protected void startStreamWithPermCheck() {
        int audioPerm = ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);
        if (audioPerm != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                Log.e(TAG, "No CAMERA or AudioRecord permission, please check");
                Toast.makeText(this, "No CAMERA or AudioRecord permission, please check",
                        Toast.LENGTH_LONG).show();
            } else {
                String[] permissions = {Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.READ_PHONE_STATE};
                ActivityCompat.requestPermissions(this, permissions,
                        PERMISSION_REQUEST_START_STREAM);
            }
        } else {
            startStream();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_START_STREAM: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mStreamer.startStream();
                } else {
                    Log.e(TAG, "No CAMERA or AudioRecord permission");
                    Toast.makeText(this, "No CAMERA or AudioRecord permission",
                            Toast.LENGTH_LONG).show();
                }
                break;
            }
        }
    }

    protected int getDisplayRotation() {
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        switch (rotation) {
            case Surface.ROTATION_0:
                return 0;
            case Surface.ROTATION_90:
                return 90;
            case Surface.ROTATION_180:
                return 180;
            case Surface.ROTATION_270:
                return 270;
        }
        return 0;
    }

    protected void onRotationChanged(int rotation) {
        hideWaterMark();
        showWaterMark();
    }

    private class StdOrientationEventListener extends OrientationEventListener {
        StdOrientationEventListener(Context context, int rate) {
            super(context, rate);
        }

        @Override
        public void onOrientationChanged(int orientation) {
            int rotation = getDisplayRotation();
            if (rotation != mLastRotation) {
                Log.d(TAG, "Rotation changed " + mLastRotation + "->" + rotation);
                mIsLandscape = (rotation % 180) != 0;
                mStreamer.setRotateDegrees(rotation);
                onRotationChanged(rotation);
                mLastRotation = rotation;
            }
        }
    }

    private final USBMonitor.OnDeviceConnectListener mOnDeviceConnectListener =
            new USBMonitor.OnDeviceConnectListener() {
        @Override
        public void onAttach(UsbDevice device) {
            Log.d(TAG, "onAttach");
            Toast.makeText(UVCCameraActivity.this, "USB_DEVICE_ATTACHED", Toast.LENGTH_SHORT).show();
            startCameraPreviewWithPermCheck();
        }

        @Override
        public void onDettach(UsbDevice device) {
            Log.d(TAG, "onDettach");
            Toast.makeText(UVCCameraActivity.this, "USB_DEVICE_DETACHED", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onConnect(UsbDevice device, final USBMonitor.UsbControlBlock ctrlBlock, boolean createNew) {
            Log.d(TAG, "onConnect");
            mStreamer.getUVCCameraCapture().open(ctrlBlock);
            mStreamer.startCameraPreview();
        }

        @Override
        public void onDisconnect(UsbDevice device, USBMonitor.UsbControlBlock ctrlBlock) {
            Log.d(TAG, "onDisconnect");
            mStreamer.getUVCCameraCapture().close();
        }

        @Override
        public void onCancel(UsbDevice device) {
            Log.d(TAG, "onCancel");
        }
    };
}

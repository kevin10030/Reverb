package com.ksyun.media.diversity.eyemorestreamer.demo;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Camera;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.Chronometer;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import com.ksyun.media.streamer.filter.audio.AudioFilterBase;
import com.ksyun.media.streamer.filter.audio.AudioReverbFilter;
import com.ksyun.media.streamer.filter.imgtex.ImgTexFilter;
import com.ksyun.media.streamer.filter.imgtex.ImgTexFilterBase;
import com.ksyun.media.streamer.filter.imgtex.ImgTexFilterMgt;
import com.ksyun.media.diversity.eyemorestreamer.kit.KSYEyemoreStreamer;
import com.ksyun.media.streamer.kit.OnAudioRawDataListener;
import com.ksyun.media.streamer.kit.OnPreviewFrameListener;
import com.ksyun.media.streamer.kit.StreamerConstants;
import com.ksyun.media.streamer.logstats.StatsLogReport;
import com.ksyun.media.streamer.util.audio.KSYBgmPlayer;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class EyemoreActivity extends Activity implements
        ActivityCompat.OnRequestPermissionsResultCallback {

    private static final String TAG = "EyemoreActivity";

    private GLSurfaceView mCameraPreviewView;
    private Chronometer mChronometer;
    private View mDeleteView;
    private TextView mShootingText;
    private CheckBox mWaterMarkCheckBox;
    private CheckBox mBeautyCheckBox;
    private CheckBox mReverbCheckBox;
    private CheckBox mAudioPreviewCheckBox;
    private CheckBox mBgmCheckBox;
    private CheckBox mMuteCheckBox;
    private CheckBox mAudioOnlyCheckBox;
    private CheckBox mFrontMirrorCheckBox;
    private TextView mUrlTextView;
    private TextView mDebugInfoTextView;

    private ButtonObserver mObserverButton;
    private CheckBoxObserver mCheckBoxObserver;

    private KSYEyemoreStreamer mStreamer;
    private Handler mMainHandler;
    private Timer mTimer;

    private boolean mAutoStart;
    private boolean mIsLandscape;
    private boolean mPrintDebugInfo = false;
    private boolean mRecording = false;
    private String mUrl;
    private String mDebugInfo = "";
    private String mBgmPath = "/sdcard/test.mp3";
    private String mLogoPath = "file:///sdcard/test.png";

    private final static int PERMISSION_REQUEST_AUDIOREC = 1;
    private static final String START_STRING = "????????????";
    private static final String STOP_STRING = "????????????";

    public final static String URL = "url";
    public final static String FRAME_RATE = "framerate";
    public final static String VIDEO_BITRATE = "video_bitrate";
    public final static String AUDIO_BITRATE = "audio_bitrate";
    public final static String VIDEO_RESOLUTION = "video_resolution";
    public final static String LANDSCAPE = "landscape";
    public final static String ENCDODE_METHOD = "encode_method";
    public final static String START_ATUO = "start_auto";
    public static final String SHOW_DEBUGINFO = "show_debuginfo";

    private PendingIntent mPermissionIntent;
    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";

    private String msg;
    private UsbManager mUsbManager;
    private UsbDeviceConnection muc;
    private UsbDevice device = null;
    private String strUsbFilPath = null;

    public static void startActivity(Context context, int fromType,
                                     String rtmpUrl, int frameRate,
                                     int videoBitrate, int audioBitrate,
                                     int videoResolution, boolean isLandscape,
                                     int encodeMethod, boolean startAuto,
                                     boolean showDebugInfo) {
        Intent intent = new Intent(context, EyemoreActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("type", fromType);
        intent.putExtra(URL, rtmpUrl);
        intent.putExtra(FRAME_RATE, frameRate);
        intent.putExtra(VIDEO_BITRATE, videoBitrate);
        intent.putExtra(AUDIO_BITRATE, audioBitrate);
        intent.putExtra(VIDEO_RESOLUTION, videoResolution);
        intent.putExtra(LANDSCAPE, isLandscape);
        intent.putExtra(ENCDODE_METHOD, encodeMethod);
        intent.putExtra(START_ATUO, startAuto);
        intent.putExtra(SHOW_DEBUGINFO, showDebugInfo);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.camera_activity);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mCameraPreviewView = (GLSurfaceView) findViewById(R.id.camera_preview);
        mUrlTextView = (TextView) findViewById(R.id.url);
        mChronometer = (Chronometer) findViewById(R.id.chronometer);
        mDebugInfoTextView = (TextView) findViewById(R.id.debuginfo);

        mObserverButton = new ButtonObserver();
        mShootingText = (TextView) findViewById(R.id.click_to_shoot);
        mShootingText.setOnClickListener(mObserverButton);
        mDeleteView = findViewById(R.id.backoff);
        mDeleteView.setOnClickListener(mObserverButton);

        mCheckBoxObserver = new CheckBoxObserver();
        mBeautyCheckBox = (CheckBox) findViewById(R.id.click_to_switch_beauty);
        mBeautyCheckBox.setOnCheckedChangeListener(mCheckBoxObserver);
        mReverbCheckBox = (CheckBox) findViewById(R.id.click_to_select_audio_filter);
        mReverbCheckBox.setOnCheckedChangeListener(mCheckBoxObserver);
        mBgmCheckBox = (CheckBox) findViewById(R.id.bgm);
        mBgmCheckBox.setOnCheckedChangeListener(mCheckBoxObserver);
        mAudioPreviewCheckBox = (CheckBox) findViewById(R.id.ear_mirror);
        mAudioPreviewCheckBox.setOnCheckedChangeListener(mCheckBoxObserver);
        mMuteCheckBox = (CheckBox) findViewById(R.id.mute);
        mMuteCheckBox.setOnCheckedChangeListener(mCheckBoxObserver);
        mWaterMarkCheckBox = (CheckBox) findViewById(R.id.watermark);
        mWaterMarkCheckBox.setOnCheckedChangeListener(mCheckBoxObserver);
        mFrontMirrorCheckBox = (CheckBox) findViewById(R.id.front_camera_mirror);
        mFrontMirrorCheckBox.setOnCheckedChangeListener(mCheckBoxObserver);
        mAudioOnlyCheckBox = (CheckBox) findViewById(R.id.audio_only);
        mAudioOnlyCheckBox.setOnCheckedChangeListener(mCheckBoxObserver);

        mMainHandler = new Handler();
        mStreamer = new KSYEyemoreStreamer(this);
        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            String url = bundle.getString(URL);
            if (!TextUtils.isEmpty(url)) {
                mUrl = url;
                mUrlTextView.setText(mUrl);
                mStreamer.setUrl(url);
            }

            int frameRate = bundle.getInt(FRAME_RATE, 0);
            if (frameRate > 0) {
                mStreamer.setPreviewFps(frameRate);
                mStreamer.setTargetFps(frameRate);
            }

            int videoBitrate = bundle.getInt(VIDEO_BITRATE, 0);
            if (videoBitrate > 0) {
                mStreamer.setVideoKBitrate(videoBitrate * 3 / 4, videoBitrate, videoBitrate / 4);
            }

            int audioBitrate = bundle.getInt(AUDIO_BITRATE, 0);
            if (audioBitrate > 0) {
                mStreamer.setAudioKBitrate(audioBitrate);
            }

            int videoResolution = bundle.getInt(VIDEO_RESOLUTION, 0);
            mStreamer.setPreviewResolution(videoResolution);
            mStreamer.setTargetResolution(videoResolution);
            
            int encode_method = bundle.getInt(ENCDODE_METHOD);
            mStreamer.setEncodeMethod(encode_method);

            mIsLandscape = bundle.getBoolean(LANDSCAPE, false);
            mStreamer.setRotateDegrees(mIsLandscape ? 90 : 0);
            if (mIsLandscape) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            } else {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }

            mAutoStart = bundle.getBoolean(START_ATUO, false);
            mPrintDebugInfo = bundle.getBoolean(SHOW_DEBUGINFO, false);
        }
        mStreamer.setDisplayPreview(mCameraPreviewView);
        mStreamer.setEnableStreamStatModule(true);
        mStreamer.enableDebugLog(true);
        mStreamer.setFrontCameraMirror(mFrontMirrorCheckBox.isChecked());
        mStreamer.setMuteAudio(mMuteCheckBox.isChecked());
        mStreamer.setEnableAudioPreview(mAudioPreviewCheckBox.isChecked());
        mStreamer.setOnInfoListener(mOnInfoListener);
        mStreamer.setOnErrorListener(mOnErrorListener);
        mStreamer.setOnLogEventListener(mOnLogEventListener);
        //mStreamer.setOnAudioRawDataListener(mOnAudioRawDataListener);
        //mStreamer.setOnPreviewFrameListener(mOnPreviewFrameListener);
        mStreamer.getImgTexFilterMgt().setFilter(mStreamer.getGLRender(),
                ImgTexFilterMgt.KSY_FILTER_BEAUTY_DENOISE);
        mStreamer.setEnableImgBufBeauty(true);
        mStreamer.getImgTexFilterMgt().setOnErrorListener(new ImgTexFilterBase.OnErrorListener() {
            @Override
            public void onError(ImgTexFilterBase filter, int errno) {
                Toast.makeText(EyemoreActivity.this, "??????????????????????????????",
                        Toast.LENGTH_SHORT).show();
                mStreamer.getImgTexFilterMgt().setFilter(mStreamer.getGLRender(),
                        ImgTexFilterMgt.KSY_FILTER_BEAUTY_DISABLE);
            }
        });

        startAudioPreviewWithPermCheck();
        startUsbPreviewWithPermCheck();
    }

    @Override
    public void onResume() {
        super.onResume();
        mStreamer.startCameraPreview();
        mStreamer.onResume();
        if (mStreamer.isRecording() && !mAudioOnlyCheckBox.isChecked()) {
            mStreamer.setAudioOnly(false);
        }
        if (mWaterMarkCheckBox.isChecked()) {
            showWaterMark();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
		mStreamer.onPause();
        mStreamer.stopCameraPreview();
        if (mStreamer.isRecording() && !mAudioOnlyCheckBox.isChecked()) {
            mStreamer.setAudioOnly(true);
        }
		hideWaterMark();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mMainHandler != null) {
            mMainHandler.removeCallbacksAndMessages(null);
            mMainHandler = null;
        }
        if (mTimer != null) {
            mTimer.cancel();
        }
        mStreamer.destroy_USB();

        mStreamer.release();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                onBackoffClick();
                break;
            default:
                break;
        }
        return true;
    }

    private void startStream() {
        mStreamer.startStream();
        mShootingText.setText(STOP_STRING);
        mShootingText.postInvalidate();
        mRecording = true;
    }

    private void stopStream() {
        mStreamer.stopStream();
        mChronometer.stop();
        mShootingText.setText(START_STRING);
        mShootingText.postInvalidate();
        mRecording = false;
    }

    private void beginInfoUploadTimer() {
        if (mPrintDebugInfo && mTimer == null) {
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
            }, 100, 1000);
        }
    }

    //update debug info
    private void updateDebugInfo() {
        if (mStreamer == null) return;
        mDebugInfo = String.format(Locale.getDefault(),
                "RtmpHostIP()=%s DroppedFrameCount()=%d \n " +
                        "ConnectTime()=%d DnsParseTime()=%d \n " +
                        "UploadedKB()=%d EncodedFrames()=%d \n" +
                        "CurrentKBitrate=%d Version()=%s",
                mStreamer.getRtmpHostIP(), mStreamer.getDroppedFrameCount(),
                mStreamer.getConnectTime(), mStreamer.getDnsParseTime(),
                mStreamer.getUploadedKBytes(), mStreamer.getEncodedFrames(),
                mStreamer.getCurrentUploadKBitrate(), mStreamer.getVersion());
    }

    //show watermark in specific location
    private void showWaterMark() {
        if (!mIsLandscape) {
            mStreamer.showWaterMarkLogo(mLogoPath, 0.08f, 0.04f, 0.20f, 0, 0.8f);
            mStreamer.showWaterMarkTime(0.03f, 0.01f, 0.35f, Color.RED, 1.0f);
        } else {
            mStreamer.showWaterMarkLogo(mLogoPath, 0.05f, 0.09f, 0, 0.20f, 0.8f);
            mStreamer.showWaterMarkTime(0.01f, 0.03f, 0.22f, Color.RED, 1.0f);
        }
    }

    private void hideWaterMark() {
        mStreamer.hideWaterMarkLogo();
        mStreamer.hideWaterMarkTime();
    }

    private KSYEyemoreStreamer.OnInfoListener mOnInfoListener = new KSYEyemoreStreamer.OnInfoListener() {
        @Override
        public void onInfo(int what, int msg1, int msg2) {
            switch (what) {
                case StreamerConstants.KSY_STREAMER_CAMERA_INIT_DONE:
                    Log.d(TAG, "KSY_STREAMER_CAMERA_INIT_DONE");
                    if (mAutoStart) {
                        startStream();
                    }
                    break;
                case StreamerConstants.KSY_STREAMER_OPEN_STREAM_SUCCESS:
                    Log.d(TAG, "KSY_STREAMER_OPEN_STREAM_SUCCESS");
                    mChronometer.setBase(SystemClock.elapsedRealtime());
                    mChronometer.start();
                    beginInfoUploadTimer();
                    break;
                case StreamerConstants.KSY_STREAMER_FRAME_SEND_SLOW:
                    Log.d(TAG, "KSY_STREAMER_FRAME_SEND_SLOW " + msg1 + "ms");
                    Toast.makeText(EyemoreActivity.this, "Network not good!",
                            Toast.LENGTH_SHORT).show();
                    break;
                case StreamerConstants.KSY_STREAMER_EST_BW_RAISE:
                    Log.d(TAG, "BW raise to " + msg1 / 1000 + "kbps");
                    break;
                case StreamerConstants.KSY_STREAMER_EST_BW_DROP:
                    Log.d(TAG, "BW drop to " + msg1 / 1000 + "kpbs");
                    break;
                default:
                    Log.d(TAG, "OnInfo: " + what + " msg1: " + msg1 + " msg2: " + msg2);
                    break;
            }
        }
    };

    private KSYEyemoreStreamer.OnErrorListener mOnErrorListener = new KSYEyemoreStreamer.OnErrorListener() {
        @Override
        public void onError(int what, int msg1, int msg2) {
            switch (what) {
                case StreamerConstants.KSY_STREAMER_ERROR_DNS_PARSE_FAILED:
                    Log.d(TAG, "KSY_STREAMER_ERROR_DNS_PARSE_FAILED");
                    break;
                case StreamerConstants.KSY_STREAMER_ERROR_CONNECT_FAILED:
                    Log.d(TAG, "KSY_STREAMER_ERROR_CONNECT_FAILED");
                    break;
                case StreamerConstants.KSY_STREAMER_ERROR_PUBLISH_FAILED:
                    Log.d(TAG, "KSY_STREAMER_ERROR_PUBLISH_FAILED");
                    break;
                case StreamerConstants.KSY_STREAMER_ERROR_CONNECT_BREAKED:
                    Log.d(TAG, "KSY_STREAMER_ERROR_CONNECT_BREAKED");
                    break;
                case StreamerConstants.KSY_STREAMER_ERROR_AV_ASYNC:
                    Log.d(TAG, "KSY_STREAMER_ERROR_AV_ASYNC " + msg1 + "ms");
                    break;
                case StreamerConstants.KSY_STREAMER_VIDEO_ENCODER_ERROR_UNSUPPORTED:
                    Log.d(TAG, "KSY_STREAMER_VIDEO_ENCODER_ERROR_UNSUPPORTED");
                    break;
                case StreamerConstants.KSY_STREAMER_VIDEO_ENCODER_ERROR_UNKNOWN:
                    Log.d(TAG, "KSY_STREAMER_VIDEO_ENCODER_ERROR_UNKNOWN");
                    break;
                case StreamerConstants.KSY_STREAMER_AUDIO_ENCODER_ERROR_UNSUPPORTED:
                    Log.d(TAG, "KSY_STREAMER_AUDIO_ENCODER_ERROR_UNSUPPORTED");
                    break;
                case StreamerConstants.KSY_STREAMER_AUDIO_ENCODER_ERROR_UNKNOWN:
                    Log.d(TAG, "KSY_STREAMER_AUDIO_ENCODER_ERROR_UNKNOWN");
                    break;
                case StreamerConstants.KSY_STREAMER_AUDIO_RECORDER_ERROR_START_FAILED:
                    Log.d(TAG, "KSY_STREAMER_AUDIO_RECORDER_ERROR_START_FAILED");
                    break;
                case StreamerConstants.KSY_STREAMER_AUDIO_RECORDER_ERROR_UNKNOWN:
                    Log.d(TAG, "KSY_STREAMER_AUDIO_RECORDER_ERROR_UNKNOWN");
                    break;
                case StreamerConstants.KSY_STREAMER_CAMERA_ERROR_UNKNOWN:
                    Log.d(TAG, "KSY_STREAMER_CAMERA_ERROR_UNKNOWN");
                    break;
                case StreamerConstants.KSY_STREAMER_CAMERA_ERROR_START_FAILED:
                    Log.d(TAG, "KSY_STREAMER_CAMERA_ERROR_START_FAILED");
                    break;
                case StreamerConstants.KSY_STREAMER_CAMERA_ERROR_SERVER_DIED:
                    Log.d(TAG, "KSY_STREAMER_CAMERA_ERROR_SERVER_DIED");
                    break;
                default:
                    Log.d(TAG, "what=" + what + " msg1=" + msg1 + " msg2=" + msg2);
                    break;
            }
            switch (what) {
                case StreamerConstants.KSY_STREAMER_CAMERA_ERROR_UNKNOWN:
                case StreamerConstants.KSY_STREAMER_CAMERA_ERROR_START_FAILED:
                case StreamerConstants.KSY_STREAMER_AUDIO_RECORDER_ERROR_START_FAILED:
                case StreamerConstants.KSY_STREAMER_AUDIO_RECORDER_ERROR_UNKNOWN:
                    break;
                case StreamerConstants.KSY_STREAMER_CAMERA_ERROR_SERVER_DIED:
                    mStreamer.stopCameraPreview();
                    mMainHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mStreamer.startCameraPreview();
                        }
                    }, 5000);
                    break;
                default:
                    stopStream();
                    mMainHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            startStream();
                        }
                    }, 3000);
                    break;
            }
        }
    };

    private StatsLogReport.OnLogEventListener mOnLogEventListener =
            new StatsLogReport.OnLogEventListener() {
                @Override
                public void onLogEvent(StringBuilder singleLogContent) {
                    Log.i(TAG, "***onLogEvent : " + singleLogContent.toString());
                }
            };

    private OnAudioRawDataListener mOnAudioRawDataListener = new OnAudioRawDataListener() {
        @Override
        public short[] OnAudioRawData(short[] data, int count) {
            Log.d(TAG, "OnAudioRawData data.length=" + data.length + " count=" + count);
            //audio pcm data
            return data;
        }
    };

    private OnPreviewFrameListener mOnPreviewFrameListener = new OnPreviewFrameListener() {
        @Override
        public void onPreviewFrame(byte[] data, int width, int height, boolean isRecording) {
            Log.d(TAG, "onPreviewFrame data.length=" + data.length + " " +
                    width + "x" + height + " isRecording=" + isRecording);
        }
    };

    private void onBackoffClick() {
        new AlertDialog.Builder(EyemoreActivity.this).setCancelable(true)
                .setTitle("?????????????")
                .setNegativeButton("??????", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {

                    }
                })
                .setPositiveButton("??????", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        mChronometer.stop();
                        mRecording = false;
                        EyemoreActivity.this.finish();
                    }
                }).show();
    }

    private void onShootClick() {
        if (mRecording) {
            stopStream();
        } else {
            startStream();
        }
    }

    private void showChooseFilter() {
        AlertDialog alertDialog;
        alertDialog = new AlertDialog.Builder(this)
                .setTitle("?????????????????????")
                .setSingleChoiceItems(
                        new String[]{"BEAUTY_SOFT", "SKIN_WHITEN", "BEAUTY_ILLUSION", "DENOISE",
                                "BEAUTY_SMOOTH", "DEMOFILTER", "GROUP_FILTER"}, -1,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (which < 5) {
                                    mStreamer.getImgTexFilterMgt().setFilter(
                                            mStreamer.getGLRender(), which + 16);
                                } else if (which == 5) {
                                    mStreamer.getImgTexFilterMgt().setFilter(
                                            new DemoFilter(mStreamer.getGLRender()));
                                } else if (which == 6) {
                                    List<ImgTexFilter> groupFilter = new LinkedList<>();
                                    groupFilter.add(new DemoFilter2(mStreamer.getGLRender()));
                                    groupFilter.add(new DemoFilter3(mStreamer.getGLRender()));
                                    groupFilter.add(new DemoFilter4(mStreamer.getGLRender()));
                                    mStreamer.getImgTexFilterMgt().setFilter(groupFilter);
                                }
                                dialog.dismiss();
                            }
                        })
                .create();
        alertDialog.setCancelable(false);
        alertDialog.show();
    }

    boolean[] mChooseFilter = {false, false};

    private void showChooseAudioFilter() {
        AlertDialog alertDialog;
        alertDialog = new AlertDialog.Builder(this)
                .setTitle("?????????????????????")
                .setMultiChoiceItems(
                        new String[]{"REVERB", "DEMO",}, mChooseFilter,
                        new DialogInterface.OnMultiChoiceClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                                if (isChecked) {
                                    mChooseFilter[which] = true;
                                }
                            }
                        }
                ).setPositiveButton("??????", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (mChooseFilter[0] && mChooseFilter[1]) {
                            List<AudioFilterBase> filters = new LinkedList<>();
                            AudioReverbFilter reverbFilter = new AudioReverbFilter();
                            DemoAudioFilter demofilter = new DemoAudioFilter();
                            filters.add(reverbFilter);
                            filters.add(demofilter);
                            mStreamer.getAudioFilterMgt().setFilter(filters);
                        } else if (mChooseFilter[0]) {
                            AudioReverbFilter reverbFilter = new AudioReverbFilter();
                            mStreamer.getAudioFilterMgt().setFilter(reverbFilter);
                        } else if (mChooseFilter[1]) {
                            DemoAudioFilter demofilter = new DemoAudioFilter();
                            mStreamer.getAudioFilterMgt().setFilter(demofilter);
                        } else {
                            mStreamer.getAudioFilterMgt().setFilter((AudioFilterBase) null);
                        }
                        dialog.dismiss();
                    }
                })
                .create();
        alertDialog.setCancelable(false);
        alertDialog.show();
    }

    private void onBeautyChecked(boolean isChecked) {
        if (isChecked) {
            if (mStreamer.getVideoEncodeMethod() ==
                    StreamerConstants.ENCODE_METHOD_SOFTWARE_COMPAT) {
                mStreamer.getImgTexFilterMgt().setFilter(mStreamer.getGLRender(),
                        ImgTexFilterMgt.KSY_FILTER_BEAUTY_DENOISE);
                mStreamer.setEnableImgBufBeauty(true);
            } else {
                showChooseFilter();
            }
        } else {
            mStreamer.getImgTexFilterMgt().setFilter(mStreamer.getGLRender(),
                    ImgTexFilterMgt.KSY_FILTER_BEAUTY_DISABLE);
            mStreamer.setEnableImgBufBeauty(false);
        }
    }

    private void onAudioFilterChecked(boolean isChecked) {
        showChooseAudioFilter();
    }

    private void onBgmChecked(boolean isChecked) {
        if (isChecked) {
            mStreamer.getAudioPlayerCapture().getBgmPlayer()
                    .setOnCompletionListener(new KSYBgmPlayer.OnCompletionListener() {
                        @Override
                        public void onCompletion(KSYBgmPlayer bgmPlayer) {
                            Log.d(TAG, "End of the currently playing music");
                        }
                    });
            mStreamer.getAudioPlayerCapture().getBgmPlayer()
                    .setOnErrorListener(new KSYBgmPlayer.OnErrorListener() {
                        @Override
                        public void onError(KSYBgmPlayer bgmPlayer, int what, int extra) {
                            Log.e(TAG, "onBgmError: " + what);
                        }
                    });
            mStreamer.getAudioPlayerCapture().getBgmPlayer().setVolume(1.0f);
            mStreamer.getAudioPlayerCapture().getBgmPlayer().setMute(false);
            mStreamer.startBgm(mBgmPath, true);
            mStreamer.setHeadsetPlugged(true);
        } else {
            mStreamer.stopBgm();
        }
    }

    private void onAudioPreviewChecked(boolean isChecked) {
        mStreamer.setEnableAudioPreview(isChecked);
    }

    private void onMuteChecked(boolean isChecked) {
        mStreamer.setMuteAudio(isChecked);
    }

    private void onWaterMarkChecked(boolean isChecked) {
        if (isChecked)
            showWaterMark();
        else
            hideWaterMark();
    }

    private void onFrontMirrorChecked(boolean isChecked) {
        mStreamer.setFrontCameraMirror(isChecked);
    }

    private void onAudioOnlyChecked(boolean isChecked) {
        mStreamer.setAudioOnly(isChecked);
    }

    private class ButtonObserver implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.backoff:
                    onBackoffClick();
                    break;
                case R.id.click_to_shoot:
                    onShootClick();
                    break;
                default:
                    break;
            }
        }
    }

    private class CheckBoxObserver implements CompoundButton.OnCheckedChangeListener {

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            switch (buttonView.getId()) {
                case R.id.click_to_switch_beauty:
                    onBeautyChecked(isChecked);
                    break;
                case R.id.click_to_select_audio_filter:
                    onAudioFilterChecked(isChecked);
                    break;
                case R.id.bgm:
                    onBgmChecked(isChecked);
                    break;
                case R.id.ear_mirror:
                    onAudioPreviewChecked(isChecked);
                    break;
                case R.id.mute:
                    onMuteChecked(isChecked);
                    break;
                case R.id.watermark:
                    onWaterMarkChecked(isChecked);
                    break;
                case R.id.front_camera_mirror:
                    onFrontMirrorChecked(isChecked);
                    break;
                case R.id.audio_only:
                    onAudioOnlyChecked(isChecked);
                    break;
                default:
                    break;
            }
        }
    }

    private void startAudioPreviewWithPermCheck() {
        int audioPerm = ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);
        if (audioPerm != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                Log.e(TAG, "No AudioRecord permission, please check");
                Toast.makeText(this, "No AudioRecord permission, please check",
                        Toast.LENGTH_LONG).show();
            } else {
                String[] permissions = {Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.READ_EXTERNAL_STORAGE};
                ActivityCompat.requestPermissions(this, permissions,
                        PERMISSION_REQUEST_AUDIOREC);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_AUDIOREC: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mStreamer.startCameraPreview();
                } else {
                    Log.e(TAG, "No AudioRecord permission");
                    Toast.makeText(this, "No AudioRecord permission",
                            Toast.LENGTH_LONG).show();
                }
                break;
            }
        }
    }
    private void startUsbPreviewWithPermCheck(){
        mUsbManager = (UsbManager)getSystemService(Context.USB_SERVICE);
        mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(
                ACTION_USB_PERMISSION), 0);

        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        registerReceiver(mUsbReceiver, filter);

        IntentFilter filter_state = new IntentFilter();
        filter_state.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter_state.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(mUsbReceiver_state, filter_state);

        HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();

        for (String s : deviceList.keySet()) {
            device = deviceList.get(s);
            if (device != null)
            {
                if (device.getProductId() == 0xf1 && device.getVendorId() == 0x4b4) {
                    strUsbFilPath = device.getDeviceName();
                    OpenUsbDev();
                }
            }
        }
    }

    void OpenUsbDev()
    {
        if(!mUsbManager.hasPermission(device))
        {
            mUsbManager.requestPermission(device, mPermissionIntent);
        }
        if(mUsbManager.hasPermission(device))
        {
            muc = mUsbManager.openDevice(device);

            int fd = muc.getFileDescriptor();

            msg = "b info: usb device path:" + strUsbFilPath
                    + "dev fd:" + fd;

            int ret = mStreamer.init_USB(fd, strUsbFilPath);
        }
    }

    BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.i(TAG, action);

            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = (UsbDevice) intent
                            .getParcelableExtra(UsbManager.EXTRA_DEVICE);

                    if (intent.getBooleanExtra(
                            UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            String strUsbFilPath = device.getDeviceName();

                            muc = mUsbManager.openDevice(device);

                            int fd = muc.getFileDescriptor();

                            msg = "mUsbReceiver b info: usb device path:" + strUsbFilPath
                                    + "dev fd:" + fd;
                            Log.i(TAG, msg);

                            int ret = mStreamer.init_USB(fd, strUsbFilPath);
                        }
                    } else {
                        Log.d(TAG, "permission denied for device " + device);
                    }
                }
            }
        }
    };

    BroadcastReceiver mUsbReceiver_state = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.i(TAG, action);

            if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (device != null) {
                    OpenUsbDev();
                }

            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                UsbDevice device = intent
                        .getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (device.getProductId() == 0xf1
                        && device.getVendorId() == 0x4b4) {
                    String strUsbFilPath = device.getDeviceName();
                    mStreamer.destroy_USB();
                    msg = "usb device " + strUsbFilPath + " removed";
                }
            }
        }
    };

}


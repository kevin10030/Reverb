package com.ksyun.media.streamer.rtspdemo;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.ksyun.media.streamer.encoder.VideoEncodeFormat;
import com.ksyun.media.streamer.framework.AVConst;
import com.ksyun.media.streamer.kit.StreamerConstants;
import com.ksyun.media.streamer.util.device.DeviceInfo;
import com.ksyun.media.streamer.util.device.DeviceInfoTools;

public class DemoActivity extends Activity
        implements OnClickListener, RadioGroup.OnCheckedChangeListener{
    private static final String TAG = DemoActivity.class.getSimpleName();
    protected Button mConnectButton;
    protected EditText mUrlEditText;
    private EditText mFrameRateEditText;
    private EditText mVideoBitRateEditText;
    private EditText mAudioBitRateEditText;
    private RadioButton mRes360Button;
    private RadioButton mRes480Button;
    private RadioButton mRes540Button;
    private RadioButton mRes720Button;

    private RadioButton mLandscapeButton;
    private RadioButton mPortraitButton;

    private RadioGroup mEncodeGroup;
    private RadioButton mSWButton;
    private RadioButton mHWButton;
    private RadioButton mSW1Button;

    private RadioGroup mEncodeTypeGroup;
    private RadioButton mEncodeWithH264;
    private RadioButton mEncodeWithH265;

    private RadioGroup mSceneGroup;
    private RadioButton mSceneDefaultButton;
    private RadioButton mSceneShowSelfButton;
    private RadioGroup mProfileGroup;
    private RadioButton mProfileLowPowerButton;
    private RadioButton mProfileBalanceButton;
    private RadioButton mProfileHighPerfButton;

    private RadioGroup mAACProfileGroup;
    private RadioButton mAACLCButton;
    private RadioButton mAACHEButton;
    private RadioButton mAACHEv2Button;
    private RadioGroup mAudioChannelGroup;
    private RadioButton mAudioMonoButton;
    private RadioButton mAudioStereoButton;

    private CheckBox mAutoStartCheckBox;
    private CheckBox mShowDebugInfoCheckBox;

    private DeviceInfo mDeviceInfo;

    boolean isSecondScreen = false;
    private boolean mShowDeviceToast = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.demo_activity);

        mConnectButton = (Button) findViewById(R.id.connectBT);
        mConnectButton.setOnClickListener(this);

        mUrlEditText = (EditText) findViewById(R.id.rtmpUrl);
        mFrameRateEditText = (EditText) findViewById(R.id.frameRatePicker);
        mFrameRateEditText.setInputType(InputType.TYPE_CLASS_NUMBER);
        mVideoBitRateEditText = (EditText) findViewById(R.id.videoBitratePicker);
        mVideoBitRateEditText.setInputType(InputType.TYPE_CLASS_NUMBER);
        mAudioBitRateEditText = (EditText) findViewById(R.id.audioBitratePicker);
        mAudioBitRateEditText.setInputType(InputType.TYPE_CLASS_NUMBER);
        mRes360Button = (RadioButton) findViewById(R.id.radiobutton1);
        mRes480Button = (RadioButton) findViewById(R.id.radiobutton2);
        mRes540Button = (RadioButton) findViewById(R.id.radiobutton3);
        mRes720Button = (RadioButton) findViewById(R.id.radiobutton4);
        mLandscapeButton = (RadioButton) findViewById(R.id.orientationbutton1);
        mPortraitButton = (RadioButton) findViewById(R.id.orientationbutton2);
        mEncodeGroup = (RadioGroup) findViewById(R.id.encode_group);
        mSWButton = (RadioButton) findViewById(R.id.encode_sw);
        mHWButton = (RadioButton) findViewById(R.id.encode_hw);
        mSW1Button = (RadioButton) findViewById(R.id.encode_sw1);
        mEncodeTypeGroup = (RadioGroup) findViewById(R.id.encode_type);
        mEncodeWithH264 = (RadioButton) findViewById(R.id.encode_h264);
        mEncodeWithH265 = (RadioButton) findViewById(R.id.encode_h265);
        mSceneGroup = (RadioGroup) findViewById(R.id.encode_scene);
        mSceneDefaultButton = (RadioButton) findViewById(R.id.encode_scene_default);
        mSceneShowSelfButton = (RadioButton) findViewById(R.id.encode_scene_show_self);
        mProfileGroup = (RadioGroup) findViewById(R.id.encode_profile);
        mProfileLowPowerButton = (RadioButton) findViewById(R.id.encode_profile_low_power);
        mProfileBalanceButton = (RadioButton) findViewById(R.id.encode_profile_balance);
        mProfileHighPerfButton = (RadioButton) findViewById(R.id.encode_profile_high_perf);
        mAACProfileGroup = (RadioGroup) findViewById(R.id.aac_profile);
        mAACLCButton = (RadioButton) findViewById(R.id.aac_lc);
        mAACHEButton = (RadioButton) findViewById(R.id.aac_he);
        mAACHEv2Button = (RadioButton) findViewById(R.id.aac_he_v2);
        mAudioChannelGroup = (RadioGroup) findViewById(R.id.audio_channel);
        mAudioMonoButton = (RadioButton) findViewById(R.id.mono);
        mAudioStereoButton = (RadioButton) findViewById(R.id.stereo);

        mAutoStartCheckBox = (CheckBox) findViewById(R.id.autoStart);
        mShowDebugInfoCheckBox = (CheckBox) findViewById(R.id.print_debug_info);

        updateUI();
        mEncodeTypeGroup.setOnCheckedChangeListener(this);
        mEncodeGroup.setOnCheckedChangeListener(this);
        mAudioChannelGroup.setOnCheckedChangeListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        //init encode info
        //?????????????????????????????????????????????????????????????????????????????????
        DeviceInfo lastDeviceInfo = mDeviceInfo;
        mDeviceInfo = DeviceInfoTools.getInstance().getDeviceInfo();
        if(mDeviceInfo != null) {
            Log.i(TAG, "deviceInfo:" + mDeviceInfo.printDeviceInfo());
            if (!mShowDeviceToast || (lastDeviceInfo != null && !mDeviceInfo.compareDeviceInfo
                    (lastDeviceInfo))) {
                if (mDeviceInfo.encode_h264 == DeviceInfo.ENCODE_HW_SUPPORT) {
                    //?????????????????????????????????
                    mHWButton.setChecked(true);
                    Toast.makeText(this, "???????????????h264??????????????????????????????", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "???????????????????????????????????????\n?????????????????????\n???????????????????????????" +
                            "\n????????????????????????????????????????????????", Toast.LENGTH_SHORT).show();
                }
                mShowDeviceToast = true;
            }
        }
    }
    
    private void setEnableRadioGroup(RadioGroup radioGroup, boolean enable) {
        for (int i=0; i<radioGroup.getChildCount(); i++) {
            radioGroup.getChildAt(i).setEnabled(enable);
        }
    }

    private void updateUI() {
        if (mHWButton.isChecked() || mEncodeWithH265.isChecked()) {
            setEnableRadioGroup(mSceneGroup, false);
            setEnableRadioGroup(mProfileGroup, false);
        } else {
            setEnableRadioGroup(mSceneGroup, true);
            setEnableRadioGroup(mProfileGroup, true);
        }

        if (mAudioMonoButton.isChecked()) {
            mAACHEv2Button.setEnabled(false);
        } else {
            mAACHEv2Button.setEnabled(true);
        }
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        updateUI();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.connectBT:
                int frameRate = 0;
                int videoBitRate = 0;
                int audioBitRate = 0;
                int videoResolution;
                int encodeType;
                int encodeMethod;
                int encodeScene;
                int encodeProfile;
                int orientation;
                int audioChannel = 1;
                int audioProfile;
                boolean startAuto;
                boolean showDebugInfo;

                if (!TextUtils.isEmpty(mUrlEditText.getText())
					&& mUrlEditText.getText().toString().startsWith("rtmp")) {
                    if (!TextUtils.isEmpty(mFrameRateEditText.getText().toString())) {
                        frameRate = Integer.parseInt(mFrameRateEditText.getText()
                                .toString());
                    }

                    if (!TextUtils.isEmpty(mVideoBitRateEditText.getText().toString())) {
                        videoBitRate = Integer.parseInt(mVideoBitRateEditText.getText()
                                .toString());
                    }

                    if (!TextUtils.isEmpty(mAudioBitRateEditText.getText().toString())) {
                        audioBitRate = Integer.parseInt(mAudioBitRateEditText.getText()
                                .toString());
                    }

                    if (mRes360Button.isChecked()) {
                        videoResolution = StreamerConstants.VIDEO_RESOLUTION_360P;
                    } else if (mRes480Button.isChecked()) {
                        videoResolution = StreamerConstants.VIDEO_RESOLUTION_480P;
                    } else if (mRes540Button.isChecked()) {
                        videoResolution = StreamerConstants.VIDEO_RESOLUTION_540P;
                    } else {
                        videoResolution = StreamerConstants.VIDEO_RESOLUTION_720P;
                    }

                    if (mEncodeWithH265.isChecked()) {
                        encodeType = AVConst.CODEC_ID_HEVC;
                    } else {
                        encodeType = AVConst.CODEC_ID_AVC;
                    }

                    if (mHWButton.isChecked()) {
                        encodeMethod = StreamerConstants.ENCODE_METHOD_HARDWARE;
                        mSceneGroup.setClickable(false);
                    } else if (mSWButton.isChecked()) {
                        mSceneGroup.setClickable(true);
                        encodeMethod = StreamerConstants.ENCODE_METHOD_SOFTWARE;
                    } else {
                        mSceneGroup.setClickable(true);
                        encodeMethod = StreamerConstants.ENCODE_METHOD_SOFTWARE_COMPAT;
                    }

                    //TODO
                    if (mSceneDefaultButton.isChecked()) {
                        encodeScene = VideoEncodeFormat.ENCODE_SCENE_DEFAULT;
                    } else {
                        encodeScene = VideoEncodeFormat.ENCODE_SCENE_SHOWSELF;
                    }

                    if (mProfileLowPowerButton.isChecked()) {
                        encodeProfile = VideoEncodeFormat.ENCODE_PROFILE_LOW_POWER;
                    } else if (mProfileBalanceButton.isChecked()) {
                        encodeProfile = VideoEncodeFormat.ENCODE_PROFILE_BALANCE;
                    } else {
                        encodeProfile = VideoEncodeFormat.ENCODE_PROFILE_HIGH_PERFORMANCE;
                    }


                    if (mAudioMonoButton.isChecked()) {
                        audioChannel = 1;
                    } else if (mAudioStereoButton.isChecked()){
                        audioChannel = 2;
                    }

                    if (mAACHEButton.isChecked()) {
                        audioProfile = AVConst.PROFILE_AAC_HE;
                    } else if (mAACHEv2Button.isChecked()) {
                        audioProfile = AVConst.PROFILE_AAC_HE_V2;
                    } else {
                        audioProfile = AVConst.PROFILE_AAC_LOW;
                    }

                    if (mLandscapeButton.isChecked()) {
                        orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                    } else if (mPortraitButton.isChecked()) {
                        orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                    } else {
                        orientation = ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR;
                    }

                    startAuto = mAutoStartCheckBox.isChecked();
                    showDebugInfo = mShowDebugInfoCheckBox.isChecked();

                    CameraActivity.startActivity(getApplicationContext(), 0,
                            mUrlEditText.getText().toString(), frameRate, videoBitRate,
                            audioBitRate, videoResolution, orientation, encodeType,  encodeMethod,
                            encodeScene, encodeProfile, audioProfile, audioChannel,
                            startAuto, showDebugInfo, isSecondScreen);
                }
                break;
            default:
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}

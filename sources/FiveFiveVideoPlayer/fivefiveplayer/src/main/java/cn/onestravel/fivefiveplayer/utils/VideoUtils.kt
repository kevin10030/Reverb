package cn.onestravel.fivefiveplayer.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.ContextWrapper
import android.media.MediaDataSource
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import java.util.*


/**
 * @author onestravel
 * @createTime 2020-02-11
 * @description TODO
 */
object VideoUtils {
    private var mSystemUiVisibilityPortrait: Int = -1
    private var mActionBarIsShown: Boolean = false

    /**
     * Get activity from context object
     *
     * @param context something
     * @return object of Activity or null if it is not Activity
     */
    fun scanForActivity(context: Context?): Activity? {
        if (context == null) return null
        if (context is Activity) {
            return context
        } else if (context is ContextWrapper) {
            return scanForActivity(context.baseContext)
        }
        return null
    }

    /**
     * Get AppCompatActivity from context
     *
     * @param context
     * @return AppCompatActivity if it's not null
     */
    private fun getAppCompActivity(context: Context?): AppCompatActivity? {
        if (context == null) return null
        if (context is AppCompatActivity) {
            return context
        } else if (context is ContextThemeWrapper) {
            return getAppCompActivity(context.baseContext)
        }
        return null
    }

    @SuppressLint("RestrictedApi")
    fun showActionBarAndStatusBar(context: Context?) {
        showActionBar(context)
        scanForActivity(context)?.let {
            it.window
                .clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }

    }


    @SuppressLint("RestrictedApi")
    fun hideActionBarAndStatusBar(context: Context?) {
        hideActionBar(context)
        scanForActivity(context)?.let {
            it.window
                .setFlags(
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN
                )
        }

    }

    @SuppressLint("RestrictedApi")
    fun showActionBar(context: Context?) {
        val ab =
            getAppCompActivity(context)!!.supportActionBar
        if (ab != null && mActionBarIsShown) {
            ab.setShowHideAnimationEnabled(false)
            ab.show()
        }

    }

    @SuppressLint("RestrictedApi")
    fun hideActionBar(context: Context?) {
        val ab =
            getAppCompActivity(context)!!.supportActionBar
        if (ab != null) {
            ab.setShowHideAnimationEnabled(false)
            mActionBarIsShown = ab.isShowing
            ab.hide()
        }
    }


    fun showBottomUIMenu(context: Context?) {
        //?????????????????????????????????
        if (Build.VERSION.SDK_INT in 12..18) { // lower api
            scanForActivity(context)?.let {
                val v: View = it.window.decorView
                v.systemUiVisibility = View.VISIBLE
            }
        } else if (Build.VERSION.SDK_INT >= 19) {
            //for new api versions.
            scanForActivity(context)?.let {
                // ??????????????????SystemUiVisibility
                val decorView: View = it.window.decorView
                if (mSystemUiVisibilityPortrait != -1) {
                    decorView.systemUiVisibility = mSystemUiVisibilityPortrait
                }
            }
        }
    }

    fun hideBottomUIMenu(context: Context?) {
        //?????????????????????????????????
        if (Build.VERSION.SDK_INT in 12..18) { // lower api
            scanForActivity(context)?.let {
                val v: View = it.window.decorView
                v.systemUiVisibility = View.GONE
            }

        } else if (Build.VERSION.SDK_INT >= 19) {
            //for new api versions.
            scanForActivity(context)?.let {
                val decorView: View = it.window.decorView
                mSystemUiVisibilityPortrait = decorView.systemUiVisibility
                val uiOptions: Int = (View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_FULLSCREEN)
                decorView.systemUiVisibility = uiOptions
            }
        }
    }


    fun hideBottomUIMenuAlways(context: Context?) {
        //?????????????????????????????????
        if (Build.VERSION.SDK_INT in 12..18) { // lower api
            scanForActivity(context)?.let {
                val v: View = it.window.decorView
                v.systemUiVisibility = View.GONE
            }
        } else if (Build.VERSION.SDK_INT >= 19) {
            scanForActivity(context)?.let {
                val _window: Window = it.window
                val params: WindowManager.LayoutParams = _window.attributes
                params.systemUiVisibility =
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE
                _window.attributes = params
            }
        }
    }

    /**
     * dialog ??????????????????????????????clearFocusNotAle() ????????????
     * ???show ?????????  focusNotAle   show?????????clearFocusNotAle
     * @param window
     */
    fun focusNotAle(context: Context?) {
        scanForActivity(context)?.let {
            val window: Window = it.window
            window.setFlags(
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            )
        }
    }

    /**
     * dialog ???????????????????????????focusNotAle() ????????????
     * ???show ?????????  focusNotAle   show?????????clearFocusNotAle
     * @param window
     */
    fun clearFocusNotAle(context: Context?) {
        scanForActivity(context)?.let {
            val window: Window = it.window
            window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
        }

    }

    fun setWindowStatusBarColor(activity: Activity, colorResId: Int) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val window = activity.window
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                window.statusBarColor = activity.resources.getColor(colorResId)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setWindowStatusBarColor(dialog: Dialog, colorResId: Int) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val window: Window? = dialog.getWindow()
                window?.let {
                    it.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                    it.statusBarColor = dialog.getContext().getResources().getColor(colorResId)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    fun setNavigationBarColor(activity: Activity, colorResId: Int) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val window = activity.window
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                //???????????????
                window.navigationBarColor = activity.resources.getColor(colorResId);
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setNavigationBarColor(dialog: Dialog, colorResId: Int) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val window: Window? = dialog.getWindow()
                window?.let {
                    it.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                    it.navigationBarColor = dialog.context.resources.getColor(colorResId);
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * ??????????????????
     *
     * @param context
     * @return width of the screen.
     */
    fun getScreenWidth(context: Context): Int {
        return context.resources.displayMetrics.widthPixels
    }

    /**
     * ??????????????????
     *
     * @param context
     * @return heiht of the screen.
     */
    fun getScreenHeight(context: Context): Int {
        return context.resources.displayMetrics.heightPixels
    }

    /**
     * ????????????APP?????????
     *
     * @param activity
     * @param brightnessPercent   0 to 1 adjusts the brightness from dark to full bright
     */
    fun setAppBrightness(context: Context, brightnessPercent: Float) {
        scanForActivity(context)?.let {
            val window = it.window
            val layoutParams = window.attributes
            layoutParams.screenBrightness = brightnessPercent
            window.attributes = layoutParams
        }
    }

    /**
     * ????????????????????????
     * @return
     */
    fun getAppBrightness(context: Context): Float {
        context?.let {
            var brightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            if (context is Activity) {
                val window = context.window
                val layoutParams = window.attributes
                brightness = layoutParams.screenBrightness
            }
            if (brightness == WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE) {
                brightness = getSystemBrightness(it) / 255f
            }
            return brightness
        }

        return 0f
    }

    /**
     * ??????????????????
     */
    fun getSystemBrightness(context: Context): Int {
        return Settings.System.getInt(
            context.contentResolver,
            Settings.System.SCREEN_BRIGHTNESS,
            255
        )
    }

    /**
     * dp???px
     *
     * @param context
     * @param dpVal   dp value
     * @return px value
     */
    fun dp2px(context: Context, dpVal: Float): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, dpVal,
            context.resources.displayMetrics
        ).toInt()
    }


    /**
     * ????????????????????????"##:##"?????????
     *
     * @param milliseconds ?????????
     * @return ##:##
     */
    fun formatTime(milliseconds: Long): String {
        if (milliseconds <= 0 || milliseconds >= 24 * 60 * 60 * 1000) {
            return "00:00"
        }
        val totalSeconds = milliseconds / 1000
        val seconds = totalSeconds % 60
        val minutes = totalSeconds / 60 % 60
        val hours = totalSeconds / 3600
        val stringBuilder = StringBuilder()
        val mFormatter =
            Formatter(stringBuilder, Locale.getDefault())
        return if (hours > 0) {
            mFormatter.format("%02d:%02d:%02d", hours, minutes, seconds).toString()
        } else {
            mFormatter.format("%02d:%02d", minutes, seconds).toString()
        }
    }

    /**
     * ???????????????????????????????????????????????????????????????????????????.
     *
     * @param context
     * @param url     ????????????url
     */
    fun savePlayPosition(
        context: Context,
        uri: Uri?,
        position: Long
    ) {
        uri?.let {
            context.getSharedPreferences(
                    "FIVE_FIVE_VIDEO_PALYER_PLAY_POSITION",
                    Context.MODE_PRIVATE
                )
                .edit()
                .putLong(it.toString(), position)
                .apply()
        }

    }

    /**
     * ?????????????????????????????????
     *
     * @param context
     * @param url     ????????????url
     * @return ???????????????????????????
     */
    fun getSavedPlayPosition(
        context: Context,
        uri: Uri?
    ): Long {
        uri?.let {
            return context.getSharedPreferences(
                    "FIVE_FIVE_VIDEO_PALYER_PLAY_POSITION",
                    Context.MODE_PRIVATE
                )
                .getLong(it.toString(), 0)
        }
        return 0
    }
}
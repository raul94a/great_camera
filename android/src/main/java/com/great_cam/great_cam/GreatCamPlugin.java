package com.great_cam.great_cam;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry;


public class GreatCamPlugin implements FlutterPlugin, PluginRegistry.RequestPermissionsResultListener, ActivityAware, MethodCallHandler, PluginRegistry.ActivityResultListener {

    private MethodChannel channel;
    private Activity activity;
    private MethodChannel.Result pendingResult;
    private String path;
    private static final String[] RECORD_AUDIO_PERMISSION = new String[]{Manifest.permission.RECORD_AUDIO};
    private static final int AUDIO_REQUEST_CODE = 10;


    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
        channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "great_cam");
        channel.setMethodCallHandler(this);
    }

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
        if (call.method.equals("startCamera")) {
            startCameraActivity(result);
        } else {
            result.notImplemented();
        }
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        channel.setMethodCallHandler(null);
    }

    @Override
    public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
        activity = binding.getActivity();
        binding.addActivityResultListener(this);
        binding.addRequestPermissionsResultListener(this);
    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {

    }

    @Override
    public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {

    }


    @Override
    public void onDetachedFromActivity() {

    }


    private void startCameraActivity(Result result) {
        pendingResult = result;
        Intent intent = new Intent(activity, CameraActivity.class);
        Intent videoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        videoIntent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);
        activity.startActivityForResult(intent, 1);

    }

    @Override
    public boolean onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        Log.i("DATA", " " + data.getData());
        if (data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                path = uri.getPath();
                pendingResult.success(path);
                clearPendingResult();
                return true;
            }
        }
        path = data.getStringExtra("path");
        if (path == null) {
            path = data.getStringExtra("dat");
            pendingResult.success(path.isEmpty() ? null : path);
            clearPendingResult();
            Log.e("Image path", "" + path);
            return true;
        }
        Log.e("Image path", "" + path);

        if (path.isEmpty()) {
            pendingResult.success(null);
        } else {
            pendingResult.success(path);

        }
        clearPendingResult();

        return true;

    }

    private void clearPendingResult() {
        pendingResult = null;
    }


    @Override
    public boolean onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        return false;
    }

    public boolean hasRecordAudioPermission() {
        return ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED;
    }

    public void requestPermission(Activity activity, String[] permission, int requestCode) {
        ActivityCompat.requestPermissions(
                activity,
                permission,
                requestCode
        );

    }
}

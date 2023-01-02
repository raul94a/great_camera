package com.great_cam.great_cam;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry;


public class GreatCamPlugin implements FlutterPlugin, ActivityAware, MethodCallHandler, PluginRegistry.ActivityResultListener {

    private MethodChannel channel;
    private Activity activity;
    private MethodChannel.Result pendingResult;
    private String path;

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
        activity.startActivityForResult(intent, 1);

    }

    @Override
    public boolean onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {


        path = data.getStringExtra("path");
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


}

package com.great_cam.great_cam;


import android.app.Activity;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.view.PreviewView;
import androidx.lifecycle.ViewModelProvider;

import com.great_cam.great_cam.utils.CameraHelper;
import com.great_cam.great_cam.viewmodels.CameraViewModel;

public class CameraActivity extends AppCompatActivity {
    private CameraHelper camera;
    private PreviewView preview;
    private ImageView btnPicture;
    private ImageView btnClose;
    private ImageView imgPreview;
    private LinearLayout handler;
    private LinearLayout previewButtons;
    private CameraViewModel cameraViewModel;
    private ImageView cancelPicture, refreshPicture, validPicture;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_camera);
        setLayout();
        cameraViewModel = new ViewModelProvider(this, ViewModelProvider.Factory.from(CameraViewModel.initializer)).get(CameraViewModel.class);
        startCameraStreaming();
        cameraActionsHandler();
        observe();

    }
    private void observe(){
        cameraViewModel.showPreview.observe(this, show->{
            if(show){
                preview.setVisibility(View.GONE);
                imgPreview.setVisibility(View.VISIBLE);
                handler.setVisibility(View.GONE);
                previewButtons.setVisibility(View.VISIBLE);
            }else{
                preview.setVisibility(View.VISIBLE);
                handler.setVisibility(View.VISIBLE);
                previewButtons.setVisibility(View.GONE);
                imgPreview.setVisibility(View.GONE);
                imgPreview.setImageBitmap(null);

            }
        });
    }
    private void cameraActionsHandler(){
        onTapCloseCamera();
        onTapRefresh();
        onTapValidate();
    }

    private void setCameraHandler() {
        if (camera != null) {
            camera = null;
        }
        camera = new CameraHelper(this.getApplicationContext(), this, preview);

    }


    private void startCameraStreaming() {
        setCameraHandler();
        onClickTakePicture();
    }

    private void onTapValidate(){
        validPicture.setOnClickListener(v->validate());
    }

    private void onTapRefresh(){
        refreshPicture.setOnClickListener(v->refresh());
    }

    private void onClickTakePicture() {
        camera.enableCamera(btnPicture, new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                Log.i("onImageSaved", "" + outputFileResults.getSavedUri());
                String path = outputFileResults.getSavedUri().getPath();
                cameraViewModel.picturePath.setValue(path);
                Log.i("Path", " " + path);
                imgPreview.setImageBitmap(BitmapFactory.decodeFile(path));
                cameraViewModel.show();

            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                Log.e("ImageCaptureException", exception.toString());
            }
        });
    }

    private void onTapCloseCamera(){

            cancelPicture.setOnClickListener(v-> closeCamera());
            btnClose.setOnClickListener(view-> closeCamera());

        }


    private void closeCamera(){
        camera.unbindAll();
        boolean imageRemoved = cameraViewModel.removeImage();
        Log.i("onTapCloseCamera", "Image removed: " + imageRemoved);
        Intent returnIntent = new Intent();
        returnIntent.putExtra("path", "");
        setResult(Activity.RESULT_OK,returnIntent);
        finish();
    }

    private void refresh(){
        boolean imageRemoved = cameraViewModel.removeImage();
        Log.i("onRefreshCamera", "Image removed: " + imageRemoved);
        cameraViewModel.hide();
    }

    private void validate(){
        camera.unbindAll();
        Intent returnIntent = new Intent();
        returnIntent.putExtra("path", cameraViewModel.picturePath.getValue());
        setResult(Activity.RESULT_OK,returnIntent);
        finish();
    }

    private void setLayout(){
        preview = this.findViewById(R.id.cameraPreview);
        btnPicture = this.findViewById(R.id.buttTakePicture);
        btnClose = this.findViewById(R.id.buttCancelCamera);
        imgPreview = this.findViewById(R.id.imgPreview);
        handler =  getView(R.id.handler);
        previewButtons = getView(R.id.previewButtons);
        cancelPicture = getView(R.id.cancelPicture);
        validPicture = getView(R.id.validPicture);
        refreshPicture = getView(R.id.refreshPicture);
    }

    private <T> T getView(int resource){
        return (T) this.findViewById(resource);
    }

}
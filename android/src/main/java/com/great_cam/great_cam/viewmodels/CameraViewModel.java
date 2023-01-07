package com.great_cam.great_cam.viewmodels;


import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.viewmodel.ViewModelInitializer;
import java.io.File;
import java.util.Objects;

public class CameraViewModel extends ViewModel {


    public MutableLiveData<Boolean> showPreview = new MutableLiveData<>(false);
    public MutableLiveData<String> picturePath = new MutableLiveData<>("");
    public MutableLiveData<Boolean> backCamera = new MutableLiveData<>(true);
    public MutableLiveData<Boolean> isVideoActive = new MutableLiveData<>(false);
    public MutableLiveData<Boolean> isVideoRunning = new MutableLiveData<>(false);

    public CameraViewModel() {
    }

    public static final ViewModelInitializer<CameraViewModel> initializer = new ViewModelInitializer<>(
            CameraViewModel.class,
            creationExtras -> new CameraViewModel());


    public void show() {
        showPreview.setValue(true);
    }

    public void hide() {
        showPreview.setValue(false);
    }


    public boolean removeImage() {
        String path = Objects.requireNonNull(picturePath.getValue());
        if (path.isEmpty()) {
            return false;
        }

        File file = new File(path);
        if (!file.exists()) {
            return false;
        }

        picturePath.setValue(null);
        return file.delete();

    }

    @NonNull
    @Override
    public String toString() {
        String showPreviewStatus = "Show preview: " + showPreview.getValue();
        String backCameraStatus = "Back camera: " + backCamera.getValue();
        String videoActiveStatus = "Video Active: " + isVideoActive.getValue();
        String videoRunningStatus = "Video Running: " + isVideoRunning.getValue();
        String picturePath = "Picture path: " + this.picturePath.getValue();
        String line = "\n";

        return showPreviewStatus + line + backCameraStatus + line + videoActiveStatus + line + videoRunningStatus + line + picturePath;
    }

/*
*    public MutableLiveData<String> picturePath = new MutableLiveData<>("");
    public MutableLiveData<Boolean> backCamera = new MutableLiveData<>(true);
    public MutableLiveData<Boolean> isVideoActive  = new MutableLiveData<>(false);
    public MutableLiveData<Boolean> isVideoRunning = new MutableLiveData<>(false);
* */

}

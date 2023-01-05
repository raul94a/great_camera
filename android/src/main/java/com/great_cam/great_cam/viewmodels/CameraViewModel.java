package com.great_cam.great_cam.viewmodels;


import static androidx.lifecycle.SavedStateHandleSupport.createSavedStateHandle;
import static androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY;

import android.util.Log;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.viewmodel.ViewModelInitializer;

import java.io.File;
import java.util.Objects;

public class CameraViewModel extends ViewModel {


    public MutableLiveData<Boolean> showPreview = new MutableLiveData<>(false);
    public MutableLiveData<String> picturePath = new MutableLiveData<>("");

    public MutableLiveData<Boolean> isUpdatingZoom = new MutableLiveData<>(false);
    public MutableLiveData<Boolean> backCamera = new MutableLiveData<>(true);
    public MutableLiveData<Boolean> isVideoActive  = new MutableLiveData<>(false);
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

        return file.delete();

    }



}

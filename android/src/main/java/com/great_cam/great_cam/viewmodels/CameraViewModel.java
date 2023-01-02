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
    public MutableLiveData<Float> zoomScaleFactor = new MutableLiveData<>(1.0f);
    public MutableLiveData<Float> previousFocus = new MutableLiveData<>(1.0f);
    public MutableLiveData<Boolean> isUpdatingZoom = new MutableLiveData<>(false);

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

    public void setPreviousFocus(float value) {
        previousFocus.setValue(value);
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


    public void zoomIn(float v) {
        float value = zoomScaleFactor.getValue();
        setPreviousFocus(v);
        if (zoomScaleFactor.getValue() == 2.0f) {
            return;
        }
        Log.i("Triggering ZOOM IN", "TRUE");

        zoomScaleFactor.setValue(value + 0.01f);
    }

    public void zoomOut(float v) {
        float value = zoomScaleFactor.getValue();
        setPreviousFocus(v);
        if (zoomScaleFactor.getValue() == 1.0f) {


            return;
        }
        Log.i("Triggering ZOOM OUT", "TRUE");
        zoomScaleFactor.setValue(value - 0.01f);
    }


}

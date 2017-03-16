package io.caleballen.audiojam.events;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Build;

import java.util.ArrayList;

import timber.log.Timber;

/**
 * Created by caleb on 3/15/17.
 */

public class TorchManager implements IBinaryEffect {

    private Context context;
    private boolean torchEnabled = false;

    private ArrayList<String> flashIds;
    private CameraManager camera;

    public TorchManager(Context context){
        this.context = context;

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {

                camera = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);

                String[] cameraIds = camera.getCameraIdList();
                flashIds = new ArrayList<>();
                for (String cameraId : cameraIds) {
                    CameraCharacteristics characteristics = camera.getCameraCharacteristics(cameraId);
                    Boolean flashAvailable = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                    if (flashAvailable != null && flashAvailable) {
                        flashIds.add(cameraId);
                    }
                }
            } catch (CameraAccessException e) {
                Timber.e(e);
            }
        } else {
            //TODO pre-marshmallow flash initialization
        }

    }

    @Override
    public void setEnabled(boolean enabled) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                }
                if (flashIds.size() > 0) {
                    camera.setTorchMode(flashIds.get(0), enabled);
                }
                torchEnabled = enabled;

            } catch (CameraAccessException e) {
                e.printStackTrace();
                //TODO error you need to close camera app
            }
        } else {
            //TODO pre-marshmallow flash
        }
    }

    @Override
    public boolean isEnabled() {
        return torchEnabled;
    }
}

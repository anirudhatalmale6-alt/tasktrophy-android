package com.tasktrophy.official.flashlight;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Flash Light manager class relies on CameraManager (Camera2 API) which needs min. API 23.
 */
public class FlashLightManager {

    private static final String TAG = FlashLightManager.class.getSimpleName();

    private final Context context;
    private final CameraManager cameraManager;
    private final List<String> flashCameraIds;

    private String firstCameraId = null;
    private String backfaceCameraId = null;
    private String frontfaceCameraId = null;

    public FlashLightManager(Context context) {
        this.context = context;
        this.cameraManager = (CameraManager) this.context.getSystemService(Context.CAMERA_SERVICE);
        this.flashCameraIds = findCamerasWithTorches();
    }

    public boolean hasFlashLight() {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
    }

    /**
     * Turns ON backface cameras flashlight
     */
    public void turnOn() {
        final String targetCameraId = backfaceCameraId != null ? backfaceCameraId : firstCameraId;
        if (targetCameraId == null) return;
        turnOn(targetCameraId);
    }

    /**
     * Turns OFF backface cameras flashlight
     */
    public void turnOff() {
        final String targetCameraId = backfaceCameraId != null ? backfaceCameraId : firstCameraId;
        if (targetCameraId == null) return;
        turnOff(targetCameraId);
    }

    public void turnOn(String cameraId) {
        if (cameraManager == null) return;

        try {
            cameraManager.setTorchMode(cameraId, true);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Turn on failed" + e.getMessage());
        }
    }

    public void turnOff(String cameraId) {
        if (cameraManager == null) return;

        try {
            cameraManager.setTorchMode(cameraId, false);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Turn off failed" + e.getMessage());
        }
    }

    public List<String> findCamerasWithTorches() {
        final List<String> flashCamIds = new ArrayList<>();

        try {
            final String[] cameraIds = cameraManager.getCameraIdList();

            this.firstCameraId = cameraIds[0];

            for (final String cameraId : cameraIds) {
                final CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);

                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                Boolean hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                if (hasFlash == null) continue;
                if (hasFlash.equals(Boolean.FALSE)) continue;

                flashCamIds.add(cameraId);

                if (facing == null) continue;
                if (facing == CameraCharacteristics.LENS_FACING_BACK)
                    backfaceCameraId = cameraId;
                else if (facing == CameraCharacteristics.LENS_FACING_FRONT)
                    frontfaceCameraId = cameraId;
            }
        } catch (CameraAccessException e) {
            final String details = e.getMessage() != null ? e.getMessage() : "No details!";
            final String message = "Finding Flash-Cams failed: " + details;

            Log.e(TAG, message);
        }

        return flashCamIds;
    }

    /**
     * Returns camera ids which has a flashlight.
     */
    public List<String> getFlashCameraIds() {
        return flashCameraIds;
    }
}

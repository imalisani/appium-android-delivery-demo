package dev.imalisani.deliveryqa;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.ComponentActivity;
import androidx.annotation.NonNull;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;

import java.io.File;
import java.util.concurrent.ExecutionException;

public final class DeliveryActivity extends ComponentActivity {
    private static final String TAG = "DeliveryActivity";
    private static final String CONFIRMED = "confirmed";
    private static final int CAMERA_REQUEST = 10;

    private TextView photoStatus;
    private TextView deliveryStatus;
    private ImageView attachedPhoto;
    private PreviewView cameraPreview;
    private Button addPhoto;
    private Button takePhoto;
    private Button confirmDelivery;
    private ProcessCameraProvider cameraProvider;
    private ImageCapture capture;
    private File photo;
    private File pendingPhoto;
    private SharedPreferences delivery;
    private boolean savingPhoto;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_delivery);

        photo = new File(getFilesDir(), "delivery-proof.jpg");
        pendingPhoto = new File(getFilesDir(), "delivery-proof-pending.jpg");
        pendingPhoto.delete();
        delivery = getSharedPreferences("delivery", MODE_PRIVATE);

        photoStatus = findViewById(R.id.photo_status);
        deliveryStatus = findViewById(R.id.delivery_status);
        attachedPhoto = findViewById(R.id.attached_photo);
        cameraPreview = findViewById(R.id.camera_preview);
        addPhoto = findViewById(R.id.add_photo);
        takePhoto = findViewById(R.id.take_photo);
        confirmDelivery = findViewById(R.id.confirm_delivery);

        cameraPreview.setImplementationMode(PreviewView.ImplementationMode.COMPATIBLE);
        cameraPreview.getPreviewStreamState().observe(this, stateOfStream -> {
            if (capture == null || savingPhoto) {
                return;
            }
            boolean ready = stateOfStream == PreviewView.StreamState.STREAMING;
            takePhoto.setEnabled(ready);
            if (ready) {
                photoStatus.setText(R.string.camera_ready);
            }
        });

        addPhoto.setOnClickListener(view -> requestCamera());
        takePhoto.setOnClickListener(view -> takePhoto());
        confirmDelivery.setOnClickListener(view -> confirmDelivery());
        renderDelivery();
    }

    private void requestCamera() {
        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        } else {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grants) {
        super.onRequestPermissionsResult(requestCode, permissions, grants);
        if (requestCode != CAMERA_REQUEST) {
            return;
        }
        if (grants.length > 0 && grants[0] == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        } else {
            photoStatus.setText(R.string.camera_permission_denied);
        }
    }

    private void openCamera() {
        addPhoto.setVisibility(View.GONE);
        cameraPreview.setVisibility(View.VISIBLE);
        takePhoto.setVisibility(View.VISIBLE);
        photoStatus.setText(R.string.camera_starting);

        var provider = ProcessCameraProvider.getInstance(this);
        provider.addListener(() -> {
            if (isDestroyed()) {
                return;
            }
            try {
                cameraProvider = provider.get();
                var preview = new Preview.Builder().build();
                preview.setSurfaceProvider(cameraPreview.getSurfaceProvider());
                capture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build();
                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA,
                        preview, capture);
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                cameraUnavailable(error);
            } catch (ExecutionException | RuntimeException error) {
                cameraUnavailable(error);
            }
        }, getMainExecutor());
    }

    private void cameraUnavailable(Exception error) {
        Log.w(TAG, "Unable to open the back camera", error);
        closeCamera();
        addPhoto.setVisibility(View.VISIBLE);
        photoStatus.setText(R.string.camera_unavailable);
    }

    private void takePhoto() {
        if (capture == null || savingPhoto) {
            return;
        }
        savingPhoto = true;
        takePhoto.setEnabled(false);
        photoStatus.setText(R.string.saving_photo);
        var output = new ImageCapture.OutputFileOptions.Builder(pendingPhoto).build();
        capture.takePicture(output, getMainExecutor(), new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults result) {
                if (isDestroyed()) {
                    return;
                }
                if (pendingPhoto.length() == 0 || !pendingPhoto.renameTo(photo)) {
                    showCaptureFailure();
                    return;
                }
                closeCamera();
                renderDelivery();
            }

            @Override
            public void onError(@NonNull ImageCaptureException error) {
                Log.w(TAG, "Unable to save delivery proof", error);
                if (!isDestroyed()) {
                    showCaptureFailure();
                }
            }
        });
    }

    private void showCaptureFailure() {
        pendingPhoto.delete();
        savingPhoto = false;
        takePhoto.setEnabled(cameraPreview.getPreviewStreamState().getValue()
                == PreviewView.StreamState.STREAMING);
        photoStatus.setText(R.string.capture_failed);
    }

    private void closeCamera() {
        capture = null;
        savingPhoto = false;
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
        takePhoto.setEnabled(false);
        takePhoto.setVisibility(View.GONE);
        cameraPreview.setVisibility(View.GONE);
    }

    private void confirmDelivery() {
        if (!hasPhoto()) {
            renderDelivery();
            return;
        }
        delivery.edit().putBoolean(CONFIRMED, true).apply();
        renderDelivery();
    }

    private boolean hasPhoto() {
        return photo.isFile() && photo.length() > 0;
    }

    private void renderDelivery() {
        boolean hasPhoto = hasPhoto();
        boolean confirmed = hasPhoto && delivery.getBoolean(CONFIRMED, false);
        deliveryStatus.setText(confirmed ? R.string.delivery_confirmed : R.string.delivery_pending);
        photoStatus.setText(hasPhoto ? R.string.photo_attached : R.string.no_photo);
        addPhoto.setVisibility(hasPhoto ? View.GONE : View.VISIBLE);
        confirmDelivery.setEnabled(hasPhoto && !confirmed);
        attachedPhoto.setVisibility(hasPhoto ? View.VISIBLE : View.GONE);
        if (hasPhoto) {
            attachedPhoto.setImageURI(Uri.fromFile(photo));
        }
    }
}

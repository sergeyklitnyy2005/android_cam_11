/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Project/Maven2/JavaApp/src/main/java/${packagePath}/${mainClassName}.java to edit this template
 */

package com.mycompany.android_cam;

/**
 *
 * @author ksv
 */

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import fi.iki.elonen.NanoHTTPD;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.graphics.Rect;
import android.graphics.ImageFormat;
import android.graphics.YuvImage;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;

public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";
    private VideoServer mServer;
    private Camera mCamera;
    private CameraPreview mPreview;
    private Button mStartButton, mStopButton;
    private TextView mAddressTextView;
    private FrameLayout mPreviewLayout;
    private Handler mHandler = new Handler(Looper.getMainLooper());
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Keep the screen on while the app is running
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        // Get UI elements
        mStartButton = findViewById(R.id.start_button);
        mStopButton = findViewById(R.id.stop_button);
        mAddressTextView = findViewById(R.id.address_text_view);
        mPreviewLayout = findViewById(R.id.preview_layout);
        // Set up start button click listener
        mStartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startServer();
            }
        });
        // Set up stop button click listener
        mStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopServer();
            }
        });
        // Get camera instance
        try {
            mCamera = Camera.open();
        } catch (Exception e) {
            Log.e(TAG, "Failed to open camera: " + e.getMessage());
            showError("Failed to open camera");
            return;
        }
        // Set up camera preview
        mPreview = new CameraPreview(this, mCamera);
        mPreviewLayout.addView(mPreview);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Release camera when the activity is destroyed
        releaseCamera();
    }
    private void startServer() {
        // Start server on port 8080
        mServer = new VideoServer(8080);
        try {
            mServer.start();
        } catch (IOException e) {
            Log.e(TAG, "Failed to start server: " + e.getMessage());
            showError("Failed to start server");
            return;
        }
        // Show server address on the UI
        String ipAddress = getLocalIpAddress();
        if (ipAddress == null) {
            Log.e(TAG, "Failed to get local IP address");
            showError("Failed to get IP address");
            return;
        }
        mAddressTextView.setText("http://" + ipAddress + ":8080");
        // Disable start button and enable stop button
        mStartButton.setEnabled(false);
        mStopButton.setEnabled(true);
        // Set up preview callback to send frames to server
        mCamera.setPreviewCallback(new PreviewCallback() {
            @Override
            public void onPreviewFrame(byte[] data, Camera camera) {
                // Convert image data to JPEG
                Size size = camera.getParameters().getPreviewSize();
                YuvImage image = new YuvImage(data, ImageFormat.NV21, size.width, size.height, null);
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                image.compressToJpeg(new Rect(0, 0, size.width, size.height), 50, out);
                byte[] jpegData = out.toByteArray();
                // Send JPEG data to server
                StreamVideoTask task = new StreamVideoTask();
                task.execute(jpegData);
            }
        });
    }
    private void stopServer() {
        // Stop server
        if (mServer != null) {
            mServer.stop();
            mServer = null;
        }
        // Reset UI
        mStartButton.setEnabled(true);
        mStopButton.setEnabled(false);
        mAddressTextView.setText("");
    }
    private void releaseCamera() {
        // Release camera if it's not null
        if (mCamera != null) {
            mPreviewLayout.removeView(mPreview);
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }
    private String getLocalIpAddress() {
        try {
            // Get all network interfaces
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            // Loop through interfaces
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                // Loop through addresses for each interface
                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    // Check if the address is not a loopback address and it's an IPv4 address
                    if (!addr.isLoopbackAddress() && addr.getAddress().length == 4) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to get local IP address: " + e.getMessage());
        }
        return null;
    }
    private void showError(final String message) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                // Show error message as toast
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }
    private class StreamVideoTask extends AsyncTask<byte[], Void, Void> {
        @Override
        protected Void doInBackground(byte[]... params) {
            // Get JPEG data
            byte[] jpegData = params[0];
            // Send data to server
            mServer.send(jpegData);
            return null;
        }
    }
    private class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
        private SurfaceHolder mHolder;
        private Camera mCamera;
        public CameraPreview(Context context, Camera camera) {
            super(context);
            mCamera = camera;
            mHolder = getHolder();
            mHolder.addCallback(this);
        }
        @Override
        public void surfaceCreated(SurfaceёHolder holder) {
            try {
                mCamera.setPreviewDisplay(holder);
                mCamera.startPreview();
            } catch (IOException e) {
                Log.e(TAG, "Failed to start camera preview: " + e.getMessage());
                showError("Failed to start camera preview");
            }
        }
        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            // Nothing to do here
        }
        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            releaseCamera();
        }
    }
}

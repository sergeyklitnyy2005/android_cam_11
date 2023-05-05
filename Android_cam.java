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



//Данный класс отвечает за основную логику приложения, связанную с отображением и запуском сервера
//для видеопотока и его остановки, а также управлением камерой и ее предварительным просмотром.
//Он также устанавливает обработчик кликов на кнопки "Start" и "Stop" и отображает адрес для доступа
//к серверу в TextView.
public class MainActivity extends Activity {
    
    //Это статическое поле со значением "MainActivity", которое используется для тегирования сообщений логирования.
    private static final String TAG = "MainActivity";
    
    //Это поле объекта типа VideoServer, который представляет видео-сервер.
    private VideoServer mServer;
    
    //Это поле объекта типа Camera, который представляет камеру устройства.
    private Camera mCamera;
    
    //Это поле объекта типа CameraPreview, который представляет превью камеры на экране устройства.
    private CameraPreview mPreview;
    
    //Это поля объектов типа Button, которые представляют кнопки для запуска и остановки видео-сервера.
    private Button mStartButton, mStopButton;
    
    //Это поле объекта типа TextView, который представляет текстовое поле для отображения адреса сервера.
    private TextView mAddressTextView;
    
    //Это поле объекта типа FrameLayout, который представляет контейнер для превью камеры.
    private FrameLayout mPreviewLayout;
    
    //Это поле объекта типа Handler, который используется для работы с потоками и выполнения операций в основном потоке (UI-потоке).
    private Handler mHandler = new Handler(Looper.getMainLooper());
    
    @Override
    
    //объявление метода onCreate, который является основным методом активности и будет вызываться при ее создании.
    //Метод принимает объект класса Bundle в качестве аргумента.
    protected void onCreate(Bundle savedInstanceState) {
        
        //вызов метода onCreate родительского класса.
        super.onCreate(savedInstanceState);
        
        //установка макета UI для данной активности.
        setContentView(R.layout.activity_main);
        
        //установка флага, который указывает на то, что экран должен быть включен, пока приложение работает.
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        
        //поиск кнопки "Старт" в макете UI и ее связывание с переменной mStartButton.
        mStartButton = findViewById(R.id.start_button);
        
        //поиск кнопки "Стоп" в макете UI и ее связывание с переменной mStopButton.
        mStopButton = findViewById(R.id.stop_button);
        
        //поиск текстового поля в макете UI и его связывание с переменной mAddressTextView.
        mAddressTextView = findViewById(R.id.address_text_view);
        
        //поиск макета для отображения видеопотока в макете UI и его связывание с переменной mPreviewLayout.
        mPreviewLayout = findViewById(R.id.preview_layout);
        
        //установка слушателя кликов на кнопку "Старт".
        mStartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            
            //вызов метода startServer() при нажатии на кнопку "Старт".
            public void onClick(View v) {
                startServer();
            }
        });
        
        //устанавливает слушателя OnClickListener на кнопку, где при нажатии будет вызвана функция "stopServer()".
        mStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopServer();
            }
        });
        
        //пытается получить доступ к камере через метод "Camera.open()". Если это не удалось, то будет выведено сообщение об ошибке,
        //записанное в объекте типа "Log" и вызван метод "showError()" для отображения сообщения об ошибке на экране.
        try {
            mCamera = Camera.open();
        } catch (Exception e) {
            Log.e(TAG, "Failed to open camera: " + e.getMessage());
            showError("Failed to open camera");
            return;
        }
        
        //создает экземпляр класса "CameraPreview" и присваивает его переменной "mPreview". 
        //Этот класс используется для предварительного просмотра камеры.
        mPreview = new CameraPreview(this, mCamera);
        
        //добавляет предварительный просмотр камеры на макет экрана приложения.
        mPreviewLayout.addView(mPreview);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Освобождаем камеру, когда активность уничтожается
        releaseCamera();
    }
    private void startServer() {
        // Запускаем сервер на порту 8080
        mServer = new VideoServer(8080);
        try {
            mServer.start();
        } catch (IOException e) {
            Log.e(TAG, "Failed to start server: " + e.getMessage());
            showError("Failed to start server");
            return;
        }
        // Показать адрес сервера в пользовательском интерфейсе
        String ipAddress = getLocalIpAddress();
        if (ipAddress == null) {
            Log.e(TAG, "Failed to get local IP address");
            showError("Failed to get IP address");
            return;
        }
        mAddressTextView.setText("http://" + ipAddress + ":8080");
        
        // Отключить кнопку запуска и включить кнопку остановки
        mStartButton.setEnabled(false);
        mStopButton.setEnabled(true);
        // Настраиваем обратный вызов предварительного просмотра для отправки кадров на сервер
        mCamera.setPreviewCallback(new PreviewCallback() {
            @Override
            public void onPreviewFrame(byte[] data, Camera camera) {
                // Преобразование данных изображения в JPEG
                Size size = camera.getParameters().getPreviewSize();
                YuvImage image = new YuvImage(data, ImageFormat.NV21, size.width, size.height, null);
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                image.compressToJpeg(new Rect(0, 0, size.width, size.height), 50, out);
                byte[] jpegData = out.toByteArray();
                // Отправляем данные JPEG на сервер
                StreamVideoTask task = new StreamVideoTask();
                task.execute(jpegData);
            }
        });
    }
    private void stopServer() {
        // Остановка сервера
        if (mServer != null) {
            mServer.stop();
            mServer = null;
        }
        // сбрасиваем UI
        mStartButton.setEnabled(true);
        mStopButton.setEnabled(false);
        mAddressTextView.setText("");
    }
    private void releaseCamera() {
        // Освобождаем камеру, если она не нулевая
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
            // Получить все сетевые интерфейсы
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            // Цикл по интерфейсу
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                // Перебираем адреса для каждого интерфейса
                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    // Проверьте, не является ли адрес петлевым адресом и адресом IPv4.
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
                // Показать сообщение об ошибке в качестве всплывающего окна 
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }
    private class StreamVideoTask extends AsyncTask<byte[], Void, Void> {
        @Override
        protected Void doInBackground(byte[]... params) {
            // Получить данные JPEG
            byte[] jpegData = params[0];
            // Отправить данные на сервер
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
            // В этом методе можно добавить свою собственную логику для обработки любых 
            //изменений свойств или атрибутов поверхности, таких как настройка размера
            //или ориентации вашего пользовательского интерфейса или обновление данных, отображаемых на поверхности.
            // но это пока не надо
        }
        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            releaseCamera();
        }
    }
}

package com.google.sample.eddystonevalidator;



import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class FlyBitch extends Service  {

	BluetoothAdapter bluetoothAdapter;
	BluetoothLeScanner leScanner;
	private WindowManager windowManager;
	private ImageView chatHead;
    MyScanCallback mCallback;

	@Override
	public IBinder onBind(Intent intent) {

		return null;
	}

	@Override 
	public void onCreate() {
		super.onCreate();
		BluetoothManager bluetoothManager;

		bluetoothManager = (BluetoothManager) this.getSystemService(BLUETOOTH_SERVICE);

		bluetoothAdapter = bluetoothManager.getAdapter();

		leScanner = bluetoothAdapter.getBluetoothLeScanner();

		ScanFilter.Builder builder = new ScanFilter.Builder();
		builder.setDeviceName("Trattention");

		ScanFilter filter = builder.build();
		ScanSettings.Builder settingsBuilder = new ScanSettings.Builder();
		settingsBuilder.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY);

		ScanSettings settings = settingsBuilder.build();
		ArrayList<ScanFilter> filterList = new ArrayList<ScanFilter>();
		filterList.add(filter);

        mCallback = new MyScanCallback(this) {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);
                Toast toast = Toast.makeText(myService,(CharSequence)(result.getDevice().getName() + new Date().toString()), Toast.LENGTH_SHORT);
                toast.show();
                //this.myService.popUp();
            }

            @Override
            public void onBatchScanResults(List<ScanResult> results) {
                super.onBatchScanResults(results);
            }

            @Override
            public void onScanFailed(int errorCode) {
                super.onScanFailed(errorCode);
                this.myService.popUp();
            }
        };

		leScanner.startScan(/*filterList, settings,*/ mCallback);




	}

	private class MyScanCallback extends ScanCallback {
			FlyBitch myService;

			public MyScanCallback(FlyBitch myService) {
				super();
				this.myService = myService;
			}

	}


	private void popUp() {
		try {
		windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

		chatHead = new ImageView(this);

		chatHead.setImageResource(R.drawable.floating);

		final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
				WindowManager.LayoutParams.WRAP_CONTENT,
				WindowManager.LayoutParams.WRAP_CONTENT,
				WindowManager.LayoutParams.TYPE_PHONE,
				WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
				PixelFormat.TRANSLUCENT);

		params.gravity = Gravity.TOP | Gravity.LEFT;
		params.x = 0;
		params.y = 100;

		windowManager.addView(chatHead, params);


			chatHead.setOnTouchListener(new View.OnTouchListener() {
				private WindowManager.LayoutParams paramsF = params;
				private int initialX;
				private int initialY;
				private float initialTouchX;
				private float initialTouchY;

				@Override public boolean onTouch(View v, MotionEvent event) {
					switch (event.getAction()) {
						case MotionEvent.ACTION_DOWN:

							// Get current time in nano seconds.

							initialX = paramsF.x;
							initialY = paramsF.y;
							initialTouchX = event.getRawX();
							initialTouchY = event.getRawY();
							break;
						case MotionEvent.ACTION_UP:
							break;
						case MotionEvent.ACTION_MOVE:
							paramsF.x = initialX + (int) (event.getRawX() - initialTouchX);
							paramsF.y = initialY + (int) (event.getRawY() - initialTouchY);
							windowManager.updateViewLayout(chatHead, paramsF);
							break;
					}
					return false;
				}
			});
		} catch (Exception e) {
			// TODO: handle exception
		}
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		return START_NOT_STICKY;
	}


	@Override
	public void onDestroy() {
        bluetoothAdapter.stopLeScan(new BluetoothAdapter.LeScanCallback(){

            @Override
            public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {

            }
        });
		leScanner.stopScan(mCallback);
		if (chatHead != null) windowManager.removeView(chatHead);

		super.onDestroy();
	}


}

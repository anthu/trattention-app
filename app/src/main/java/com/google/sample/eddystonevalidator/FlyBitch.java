package com.google.sample.eddystonevalidator;



import android.app.Activity;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.os.ParcelUuid;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Handler;


public class FlyBitch extends Service  {

	BluetoothAdapter bluetoothAdapter;
	BluetoothLeScanner leScanner;
	private WindowManager windowManager;
	private ImageView chatHead;
    private List<ScanFilter> scanFilters;
    private static final ScanSettings SCAN_SETTINGS =
            new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).setReportDelay(0)
                    .build();
    MyScanCallback mCallback;
    private Map<String /* device address */, Beacon> deviceToBeaconMap = new HashMap<>();
    private static final ParcelUuid EDDYSTONE_SERVICE_UUID =
            ParcelUuid.fromString("0000FEAA-0000-1000-8000-00805F9B34FB");
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

        // The Eddystone Service UUID, 0xFEAA.

		leScanner = bluetoothAdapter.getBluetoothLeScanner();

		ScanFilter.Builder builder = new ScanFilter.Builder();
		builder.setDeviceName("Trattention");

        scanFilters = new ArrayList<>();
        scanFilters.add(new ScanFilter.Builder().setServiceUuid(EDDYSTONE_SERVICE_UUID).build());
		ScanSettings.Builder settingsBuilder = new ScanSettings.Builder();
		settingsBuilder.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY);
		ArrayList<ScanFilter> filterList = new ArrayList<ScanFilter>();

        mCallback = new MyScanCallback(this) {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                ScanRecord scanRecord = result.getScanRecord();
                if (scanRecord == null) {
                    return;
                }

                String deviceAddress = result.getDevice().getAddress();
                Beacon beacon;
                if (!deviceToBeaconMap.containsKey(deviceAddress)) {
                    beacon = new Beacon(deviceAddress, result.getRssi());
                    deviceToBeaconMap.put(deviceAddress, beacon);
                    if (result.getScanRecord().getServiceUuids() != null) {
                        UidValidator.validate(deviceAddress,result.getScanRecord().getServiceData(result.getScanRecord().getServiceUuids().get(0)),beacon);
                        if(beacon.uidStatus != null && beacon.uidStatus.uidValue != null && beacon.uidStatus.uidValue.startsWith("6d9d1fe6b2f4a40ae168")) {
                            Log.d("Bitch ", "onScanResult: " + beacon);
                            Toast toast = Toast.makeText(myService, (CharSequence) (beacon.uidStatus.uidValue), Toast.LENGTH_SHORT);
                            toast.show();
                            popUp();



							new android.os.Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
								@Override
								public void run() {
									if (chatHead != null) windowManager.removeView(chatHead);
								}}
							, 5000);

						}
                    }
                } else {
                    deviceToBeaconMap.get(deviceAddress).lastSeenTimestamp = System.currentTimeMillis();
                    deviceToBeaconMap.get(deviceAddress).rssi = result.getRssi();

                }

                byte[] serviceData = scanRecord.getServiceData(EDDYSTONE_SERVICE_UUID);
                validateServiceData(deviceAddress, serviceData);
            }

            @Override
            public void onScanFailed(int errorCode) {
            }

            @Override
            public void onBatchScanResults(List<ScanResult> results) {
                super.onBatchScanResults(results);
            }

        };

		leScanner.startScan(filterList, SCAN_SETTINGS, mCallback);




	}
    private void validateServiceData(String deviceAddress, byte[] serviceData) {
        Beacon beacon = deviceToBeaconMap.get(deviceAddress);
        if (serviceData == null) {
            String err = "Null Eddystone service data";
            beacon.frameStatus.nullServiceData = err;
            return;
        }

        switch (serviceData[0]) {
            case Constants.UID_FRAME_TYPE:
                UidValidator.validate(deviceAddress, serviceData, beacon);
                break;
            case Constants.TLM_FRAME_TYPE:
                TlmValidator.validate(deviceAddress, serviceData, beacon);
                break;
            case Constants.URL_FRAME_TYPE:
                UrlValidator.validate(deviceAddress, serviceData, beacon);
                break;
            default:
                String err = String.format("Invalid frame type byte %02X", serviceData[0]);
                beacon.frameStatus.invalidFrameType = err;
//                logDeviceError(deviceAddress, err);
                break;
        }
//        arrayAdapter.notifyDataSetChanged();
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

		return START_STICKY;
	}


	@Override
	public void onDestroy() {
		leScanner.stopScan(mCallback);

		super.onDestroy();
	}


}

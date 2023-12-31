package com.example.test_uhf;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.BatteryManager;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.util.Log;
import android.view.KeyEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.sentry.Sentry;
import io.flutter.embedding.android.FlutterActivity;
import io.flutter.embedding.engine.FlutterEngine;

import io.flutter.plugin.common.MethodChannel;
import io.sentry.android.core.SentryAndroid;

import com.handheld.uhfr.UHFRManager;
import com.uhf.api.cls.Reader;

import java.util.List;

import android.content.BroadcastReceiver;


public class MainActivity extends FlutterActivity {
 
    private static final String CHANNEL = "aaa";

    private String batteryValue;

    private String barcode;

    public UHFRManager manager;

    private SharedPreferences mSharedPreferences;

    private ScanUtil scanUtil;

    private BarCodeScanUtil barCodeScanUtil;

    private MethodChannel methodChannel;

    private KeyReceiver keyReceiver;

  private BroadcastReceiver receiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        byte[] data = intent.getByteArrayExtra("data");
        if (data != null) {
          String code = new String(data);
          barcode = code;
          methodChannel.invokeMethod("getBarCode", code);
        }
      }
    };

    private void registerKeyCodeReceiver() {
      keyReceiver = new KeyReceiver();
      IntentFilter filter = new IntentFilter();
      filter.addAction("android.rfid.FUN_KEY");
      filter.addAction("android.intent.action.FUN_KEY");
      registerReceiver(keyReceiver, filter);
    }

    private class KeyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            int keyCode = intent.getIntExtra("keyCode", 0);
          Log.e("keycord", String.valueOf(keyCode));
            if (keyCode == 0) {
                keyCode = intent.getIntExtra("keycode", 0);
            }
            boolean keyDown = intent.getBooleanExtra("keydown", false);
            if (keyDown) {
//                ToastUtils.showText("KeyReceiver:keyCode = down" + keyCode);
            } else {
//                ToastUtils.showText("KeyReceiver:keyCode = up" + keyCode);
                switch (keyCode) {
                    case KeyEvent.KEYCODE_F1:
                        break;
                    case KeyEvent.KEYCODE_F2:
                        break;
                    case KeyEvent.KEYCODE_F5:
                        break;
                    case KeyEvent.KEYCODE_F3://C510x
                    case KeyEvent.KEYCODE_F4://6100
                    case KeyEvent.KEYCODE_F7://H3100
                        // invenroty();
                        break;
                }
            }
        }

    } 

    @Override
    public void configureFlutterEngine(@NonNull FlutterEngine flutterEngine) {
        super.configureFlutterEngine(flutterEngine);
        methodChannel = new MethodChannel(flutterEngine.getDartExecutor().getBinaryMessenger(),CHANNEL);
                methodChannel.setMethodCallHandler(
                        (call,result) -> {
                          // This method is invoked on the main thread.
                          if (call.method.equals("getBatteryLevel")) {
                            int batteryLevel = getBatteryLevel();

                            if (batteryLevel != -1) {
                              result.success(batteryLevel);
                            } else {
                              result.error("UNAVAILABLE", "Battery level not available.", null);
                            }
                          } else if (call.method.equals("getSyncRFID")) {
                            String rfid = getSyncRFID();
                            result.success(rfid);
                          } else if (call.method.equals("getPower")) {
                            String power = getPower();
                            result.success(power);
                          } else if (call.method.equals("setPower")) {
                            Boolean isSuccess = setPower(30, 30);
                            result.success(isSuccess);
                          } else if (call.method.equals("getHardware")) {
                            String hardware = getHardware();
                            result.success(hardware);
                          } else if (call.method.equals("close")) {
                            Boolean isCloseSuccess = close();
                            result.success(isCloseSuccess);
                          } else if (call.method.equals("getBarCode")) {
                            result.success(barcode);
                          } else if (call.method.equals("startScan")) {
                            String scanMessage = startScan();
                            result.success(scanMessage);
                          } else if (call.method.equals("stopScan")) {
                            stopScan();
                          } else {
                            result.notImplemented();
                          }
                        }
                );
    }

  private void initModule() {
    try {
      manager = UHFRManager.getInstance();
      if (manager != null) {
        SharedUtil sharedUtil = new SharedUtil(this);
        Reader.READER_ERR err = manager.setPower(sharedUtil.getPower(), sharedUtil.getPower());
        if(err== Reader.READER_ERR.MT_OK_ERR){
          manager.setRegion(Reader.Region_Conf.valueOf(sharedUtil.getWorkFreq()));
        } else {
          Reader.READER_ERR err1 = manager.setPower(30, 30);//set uhf module power
          if(err1== Reader.READER_ERR.MT_OK_ERR) {
            manager.setRegion(Reader.Region_Conf.valueOf(mSharedPreferences.getInt("freRegion", 1)));
          }
        }
      }
    } catch (Exception e) {
      Sentry.captureException(e);
    }
  }

  @Override
  protected void onStart() {
    super.onStart();
    try {
      initModule();
      setScanKeyDisable();
    } catch (Exception e) {
      Sentry.captureException(e);
    }
  }

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mSharedPreferences = this.getSharedPreferences("UHF", MODE_PRIVATE);
    IntentFilter filter = new IntentFilter();
    filter.addAction("com.rfid.SCAN");
    registerReceiver(receiver, filter);

    IntentFilter batteryfilter = new IntentFilter();
    batteryfilter.addAction(Intent.ACTION_BATTERY_CHANGED);
    registerReceiver(batteryReceiver, batteryfilter);
    Util.initSoundPool(this);
  }

  @Override
  protected void onResume() {
    super.onResume();
    try {
      if (barCodeScanUtil == null) {
        barCodeScanUtil = new BarCodeScanUtil(this);
        //we must set mode to 0 : BroadcastReceiver mode
        barCodeScanUtil.setScanMode(0);
      }
      registerKeyCodeReceiver();
    } catch (Exception e) {
      Sentry.captureException(e);
    }
  }

  @Override
  protected void onPause() {
    super.onPause();
    try {
      if (barCodeScanUtil != null) {
        barCodeScanUtil.setScanMode(1);
        barCodeScanUtil.close();
        barCodeScanUtil = null;
      }
      unregisterReceiver(keyReceiver);
    } catch (Exception e) {
      Sentry.captureException(e);
    }
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    unregisterReceiver(receiver);
    unregisterReceiver(batteryReceiver);
  }

  @Override
  protected void onStop() {
    super.onStop();
    try {
      setScanKeyEnable();
      try {
        Thread.sleep(500);
      } catch (InterruptedException e) {
        Sentry.captureException(e);
        e.printStackTrace();
      }
      closeModule() ;
    } catch (Exception e) {
      Sentry.captureException(e);
    }
  }

  private void closeModule() {
    if (manager != null) {//close uhf module
      manager.close();
      manager = null;
    }
  }

  private void setScanKeyDisable() {
    int currentApiVersion = Build.VERSION.SDK_INT;
    if (currentApiVersion > Build.VERSION_CODES.N) {
      // For Android10.0 module
      scanUtil = ScanUtil.getInstance(this);
      scanUtil.disableScanKey("134");
      scanUtil.disableScanKey("137");
    }
  }


  private void setScanKeyEnable() {
    int currentApiVersion = Build.VERSION.SDK_INT;
    if (currentApiVersion > Build.VERSION_CODES.N) {
      // For Android10.0 module
      scanUtil = ScanUtil.getInstance(this);
      scanUtil.enableScanKey("134");
      scanUtil.enableScanKey("137");
    }
    try {
      Thread.sleep(500);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  private int getBatteryLevel() {
    int batteryLevel = -1;
    if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
      BatteryManager batteryManager = (BatteryManager) getSystemService(BATTERY_SERVICE);
      batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
    } else {
      Intent intent = new ContextWrapper(getApplicationContext()).
          registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
      batteryLevel = (intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) * 100) /
          intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
    }

    return batteryLevel;
  }

  public String getHardware() {
    try {
      String data = manager.getHardware();
      return data;
    } catch(Exception e) {
      String msg = e.getMessage();
      return msg;
    }
  }

  /**
   * 设置设备读写功率
   * @param readPower 5~30
   * @param writePower 5~30
   * @return
   */
  public Boolean setPower(int readPower, int writePower){
    try {
      Reader.READER_ERR err = manager.setPower(readPower,writePower);
      return err == Reader.READER_ERR.MT_OK_ERR;
    } catch(Exception e) {
      return false;
    }
  }

  public String getSyncRFID() {
    String data = "";
    try {
      manager.asyncStartReading();
      // Thread.sleep(300);
      List<Reader.TAGINFO> list = manager.tagInventoryRealTime();
      if (list.size() > 0) {
        for (Reader.TAGINFO tfs : list) {
            byte[] epcdata = tfs.EpcId;
            data = Tools.Bytes2HexString(epcdata, epcdata.length);
        }
      }
      manager.asyncStopReading();
      return data;
    } catch(Exception e) {
      String message = e.getMessage();
      return message;
    }
  }

  public String getPower() {
    try {
      int [] result = manager.getPower();
      if(result != null){
        return "读功率："+result[0]+" 写功率："+result[1];
      }
      return "获取失败";
    } catch(Exception e) {
      return "get power 调用失败";
    }
  }

  public Boolean close() {
    try {
      Boolean isSuccess = manager.close();
      return isSuccess;
    } catch(Exception e) {
      return false;
    }
  }

  public String startScan() {
    try {
      if (barCodeScanUtil != null) {
        barCodeScanUtil.scan();
        return "Success";
      }
      return "Fail";
    } catch(Exception e) {
       return e.getMessage();
    }
  }

  public void stopScan() {
    if (barCodeScanUtil != null) {
      barCodeScanUtil.stopScan();
    }
  }

  private BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      int level = intent.getIntExtra("level", 0);
      Log.e("batteryReceiver", "batteryReceiver level =  " + level);
      batteryValue = String.valueOf(level);
    }
  };
}
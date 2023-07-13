package com.hdhe.scantest;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.TextView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    private Button btn;
    private Button btnClear;
    private ListView lv;
    private Context context;

    private HashSet<String> setBarcode = new HashSet<String>();
    private HashMap<String, Integer> mapBarcode = new HashMap<>();
    private List<Barcode> listBarcode;
    private String TAG = "MainActivity";
    private CheckBox checkBoxAuto;
    private TextView mTvTipsDate;
    private TextView mTvTipsBattery;
    private Timer timer;

    private MAdapter adapter;

    private ScanUtil scanUtil;

    // BroadcastReceiver to receiver scan data
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            byte[] data = intent.getByteArrayExtra("data");
            if (data != null) {
//                String barcode = Tools.Bytes2HexString(data, data.length);
                String barcode = new String(data);

                // for test
                String date = MainActivity.this.stampToDate(System.currentTimeMillis());
                Log.e(TAG, "onReceive, date:" + date + ", data:" + barcode);
                MainActivity.this.mTvTipsDate.setText(date);
                MainActivity.this.mTvTipsDate.append("  ");
                MainActivity.this.mTvTipsDate.append(MainActivity.this.mTvTipsBattery.getText());
                // end

                //first add
                if (setBarcode.isEmpty()) {
                    setBarcode.add(barcode);
                    listBarcode = new ArrayList<>();
                    Barcode b = new Barcode();
                    b.sn = 1;
                    b.barcode = barcode;
                    b.count = 1;
                    listBarcode.add(b);
                    //list index
                    mapBarcode.put(barcode, 0);
                    adapter = new MAdapter();
                    lv.setAdapter(adapter);
                } else {
                    if (setBarcode.contains(barcode)) {
                        Barcode b = listBarcode.get(mapBarcode.get(barcode));
                        b.count += 1;
                        listBarcode.set(mapBarcode.get(barcode), b);

                    } else {
                        Barcode b = new Barcode();
                        b.sn = listBarcode.size();
                        b.barcode = barcode;
                        b.count = 1;
                        listBarcode.add(b);
                        setBarcode.add(barcode);
                        //list index
                        mapBarcode.put(barcode, listBarcode.size() - 1);
                    }
                }
                adapter.notifyDataSetChanged();
            }

        }
    };

    /**
     * 将时间转换为时间戳
     */
    public String dateToStamp(String s) throws ParseException {
        String res;
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = simpleDateFormat.parse(s);
        long ts = date.getTime();
        res = String.valueOf(ts);
        return res;
    }

    /**
     * 将时间戳转换为时间
     */
    public String stampToDate(long l) {
        String res;
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date(l);
        res = simpleDateFormat.format(date);
        return res;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Register receiver to receive the result of scan
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.rfid.SCAN");
        registerReceiver(receiver, filter);

        Log.i(TAG, "onCreate--------------");
        IntentFilter batteryfilter = new IntentFilter();
        batteryfilter.addAction(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(batteryReceiver, batteryfilter);

        Util.initSoundPool(this);
        btn = (Button) findViewById(R.id.button_test);
        btnClear = (Button) findViewById(R.id.button_clear);
        lv = (ListView) findViewById(R.id.listview_barcode);
        mTvTipsDate = findViewById(R.id.tips_date);
        mTvTipsBattery = findViewById(R.id.tips_battery);
        checkBoxAuto = findViewById(R.id.checkbox_auto);
        context = this;
        checkBoxAuto.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    btn.setTextColor(Color.GRAY);
                    btn.setClickable(false);
                    timer = new Timer();
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            if (scanUtil != null) {
                                scanUtil.scan();
                            }

                        }
                    }, 10, 100);
                } else {
                    btn.setTextColor(Color.BLACK);
                    btn.setClickable(true);
                    if (timer != null) {
                        timer.cancel();
                    }

                }
            }
        });
        btnClear.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (setBarcode != null)
                    setBarcode.clear();
                if (mapBarcode != null)
                    mapBarcode.clear();

                if (listBarcode != null)
                    listBarcode.clear();
                if (adapter != null)
                    adapter.notifyDataSetChanged();

            }
        });

        btn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (scanUtil != null) {
                    scanUtil.scan();
                }
            }
        });

        ((Button) findViewById(R.id.button_stop)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (scanUtil != null) {
                    scanUtil.stopScan();
                }
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (scanUtil == null) {
            scanUtil = new ScanUtil(this);
            //we must set mode to 0 : BroadcastReceiver mode
            scanUtil.setScanMode(0);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (scanUtil != null) {
            scanUtil.setScanMode(1);
            scanUtil.close();
            scanUtil = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
        unregisterReceiver(batteryReceiver);
        if (timer != null) {
            timer.cancel();
        }
    }

    //listview adapter
    private class MAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            if (listBarcode != null) {
                return listBarcode.size();
            }
            return 0;
        }

        @Override
        public Object getItem(int position) {
            if (listBarcode != null) {
                return listBarcode.get(position);
            }
            return null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                holder = new ViewHolder();
                convertView = LayoutInflater.from(MainActivity.this).inflate(R.layout.listview_item, null);
                holder.tvSn = (TextView) convertView.findViewById(R.id.textView_list_item_id);
                holder.tvBarcode = (TextView) convertView.findViewById(R.id.textView_list_item_barcode);
                holder.tvCount = (TextView) convertView.findViewById(R.id.textView_list_item_count);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            if (listBarcode != null && !listBarcode.isEmpty()) {
                Barcode b = listBarcode.get(position);
                holder.tvSn.setText("" + b.sn);
                holder.tvBarcode.setText("" + b.barcode);
                holder.tvCount.setText("" + b.count);
            }
            return convertView;
        }


        class ViewHolder {
            TextView tvSn;
            TextView tvBarcode;
            TextView tvCount;

        }
    }

    //Test monitor battery power
    private BroadcastReceiver batteryReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            int level = intent.getIntExtra("level", 0);
            Log.e("batteryReceiver", "batteryReceiver level =  " + level);
            mTvTipsBattery.setText(String.valueOf(level));
        }
    };

}

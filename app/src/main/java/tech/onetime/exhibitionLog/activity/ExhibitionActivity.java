package tech.onetime.exhibitionLog.activity;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ViewById;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import tech.onetime.exhibitionLog.R;
import tech.onetime.exhibitionLog.api.ExcelBuilder;
import tech.onetime.exhibitionLog.ble.BeaconScanCallback;
import tech.onetime.exhibitionLog.schema.BeaconObject;

/**
 * Created by joe on 2018/04/16
 */

@EActivity(R.layout.activity_exhibition)
public class ExhibitionActivity extends AppCompatActivity implements BeaconScanCallback.iBeaconScanCallback {

    private static final String TAG = "ExhibitionActivity";

    private BeaconScanCallback _beaconCallback;
    @ViewById ImageView areaImage;
    @ViewById TextView times;
    @ViewById TextView resultA;
    @ViewById TextView resultB;
    @ViewById TextView resultC;
    @ViewById TextView offSetA;
    @ViewById TextView offSetB;
    @ViewById TextView offSetC;
    @ViewById TextView resultListSize;

    // for AcceleroMeter End

    private ArrayList<HashMap<String,Integer>> resultList = new ArrayList<>();


    static final int REQUEST_ENABLE_BT = 1001; // The request code


    @AfterViews
    void afterViews() {

        Log.d(TAG, "afterViews");
        if(bleInit()){
            Log.d(TAG, "[bleInit] true");
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private boolean bleInit() {

        // Use this check to determine whether BLE is supported on the device. Then
        // you can selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "This device does not support bluetooth", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        BluetoothManager bm = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        BluetoothAdapter mBluetoothAdapter = bm.getAdapter();

        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            return false;
        }

        return scanBeacon();

    }

    private boolean scanBeacon() {

        if (_beaconCallback != null)
            _beaconCallback.stopScan();

        _beaconCallback = new BeaconScanCallback(this, this);
        _beaconCallback.startScan();
        _beaconCallback.startTimerTask();

        return true;

    }

    @Override
    public void onBackPressed() {

//        super.onBackPressed();

        setResult(RESULT_CANCELED, ExhibitionActivity.this.getIntent());
        ExhibitionActivity.this.finish();

    }

    @Override
    public void scannedBeacons(BeaconObject beaconObject) {
        /**To do  each beaconObject be scanned */
    }

    int roundTimes = 0;
    @Override
    public void getNearestBeacon(BeaconObject beaconObject) {
        Log.d(TAG, "[getNearestBeacon]" + beaconObject.getMajorMinorString() + "[time]"+beaconObject.time);
        times.setText(Integer.toString(++roundTimes));

    }

    @Override
    public void getCurrentPosition(String position, final Map<String,Integer> scoringSet,final Map<String,Integer> offSet) {

        Log.d(TAG, "[getCurrentPosition] = " + position);
        HashMap<String,Integer> scoringSetClone = null;
        resultList.add(new HashMap<>(scoringSet));

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Stuff that updates the UI
                resultListSize.setText(Integer.toString(resultList.size()));
                resultA.setText(Integer.toString(scoringSet.get("A")));
                resultB.setText(Integer.toString(scoringSet.get("B")));
                resultC.setText(Integer.toString(scoringSet.get("C")));
                if(offSet.get("A") != null){
                    offSetA.setText(Integer.toString(offSet.get("A")));
                    offSetB.setText(Integer.toString(offSet.get("B")));
                    offSetC.setText(Integer.toString(offSet.get("C")));
                }

            }
        });
    }

    protected void onDestroy(){

        super.onDestroy();

        if (_beaconCallback != null) {
            _beaconCallback.stopScan();
            _beaconCallback.closeTimerTask();
        }

    }

    @Click(R.id.exit)
    void exit(){
        setResult(RESULT_OK, ExhibitionActivity.this.getIntent());
        doSaveResult();
        ExhibitionActivity.this.finish();
    }

    @Background
    void doSaveResult() {

        Log.d(TAG, "Saving result");

        ExcelBuilder.initExcel();

        for(int i = 0; i < resultList.size(); i++){
            ExcelBuilder.setRoundResult(resultList.get(i));
        }
        _beaconCallback.closeTimerTask();

        ExcelBuilder.saveExcelFile(this,"LogFile");


    }

}

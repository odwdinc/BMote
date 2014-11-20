package systems.movingdata.bmote;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.util.List;
import java.util.UUID;


public class DiscoveryActivity extends Activity {
    private static final String TAG = "DiscoveryActivity";
    private final static int REQUEST_ENABLE_BT = 1;
    private BluetoothAdapter mBluetoothAdapter;
    private static final long SCAN_PERIOD = 10000;
    private static final long RCAN_PERIOD = 5000;
    private Handler mHandler;
    private boolean mScanning;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_discovery);

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }
        mHandler = new Handler();

        Button btn = (Button) findViewById(R.id.button);

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scanLeDevice(true);
                //BluetoothDevice device = mBluetoothAdapter.getRemoteDevice("00:A0:50:65:43:42");
                //device.getBondState()
                //device.connectGatt(DiscoveryActivity.this, false, btleGattCallback);
            }
        });

        // Initializes Bluetooth adapter.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }



    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(leScanCallback);
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (mScanning) {
                                scanLeDevice(true);
                            }
                        }
                    },RCAN_PERIOD);
                }
            }, SCAN_PERIOD);
            if (!mScanning) {
                mScanning = true;
                mBluetoothAdapter.startLeScan(leScanCallback);
            }
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(leScanCallback);
        }
    }

    private BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
            // your implementation here

            if ( device.getAddress().contains("00:A0:50:65:43:42")) {
                BluetoothGatt bluetoothGatt = device.connectGatt(DiscoveryActivity.this, false, btleGattCallback);
                scanLeDevice(false);
            }
        }
    };

    private final BluetoothGattCallback btleGattCallback = new BluetoothGattCallback() {

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
            // this will get called anytime you perform a read or write characteristic operation


                Log.i(TAG, "onCharacteristicChanged: " + characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0));


        }

        @Override
        public void onConnectionStateChange(final BluetoothGatt gatt, final int status, final int newState) {
            // this will get called when a device connects or disconnects
            if(newState == BluetoothProfile.STATE_CONNECTED){
                Log.i(TAG,"STATE_CONNECTED");
                gatt.discoverServices();

            }else if(newState == BluetoothProfile.STATE_CONNECTING){
                Log.i(TAG,"STATE_CONNECTING");

            }else if(newState == BluetoothProfile.STATE_DISCONNECTED){
                Log.i(TAG,"STATE_DISCONNECTED");
                scanLeDevice(true);

            }else if(newState == BluetoothProfile.STATE_DISCONNECTING){
                Log.i(TAG,"STATE_DISCONNECTING");

            }

        }
        String ServiceUuid ="0000cab5-0000-1000-8000-00805f9b34fb";
        String CharacteristicUuid="0000caa2-0000-1000-8000-00805f9b34fb";
        String DescriptorUuid="00002902-0000-1000-8000-00805f9b34fb";

        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, final int status) {
            // this will get called after the client initiates a            BluetoothGatt.discoverServices() call
            Log.i(TAG,"onServicesDiscovered");

            BluetoothGattService service = gatt.getService(UUID.fromString(ServiceUuid));


                Log.i(TAG, "Service Uuid: " + service.getUuid());
                List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();

                for (BluetoothGattCharacteristic characteristic : characteristics){

                    Log.i(TAG,"Characteristic Uuid: "+characteristic.getUuid());
                    gatt.setCharacteristicNotification(characteristic, true);

                    BluetoothGattDescriptor mdescriptor = characteristic.getDescriptor(UUID.fromString(DescriptorUuid));
                    mdescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    gatt.writeDescriptor(mdescriptor);


                }

        }
    };


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_discovery, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}

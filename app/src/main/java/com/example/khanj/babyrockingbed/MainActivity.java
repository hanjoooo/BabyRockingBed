package com.example.khanj.babyrockingbed;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattService;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends BaseActivity {
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    DatabaseReference mRootRef = FirebaseDatabase.getInstance().getReference();
    DatabaseReference mConditionRef = mRootRef.child("users");
    DatabaseReference mchildRef;
    DatabaseReference mchild1Ref;
    DatabaseReference mchild2Ref;
    DatabaseReference mchild3Ref;
    Button bton;
    int btonoff = 0;
    int babystate = 0;
    ImageView baby;
    TextView txstate;
    TextView txtime;
    String btBabyState = "0";
    Timer timer;
    TimerTask adTast;

    private LinkedList<BluetoothDevice> mBluetoothDevices = new LinkedList<BluetoothDevice>();
    private ArrayAdapter<String> mDeviceArrayAdapter;
    public ProgressDialog mProgressDialog;
    private ProgressDialog mLoadingDialog;
    private android.app.AlertDialog mDeviceListDialog;
    private BluetoothSerialClient mClient;

    private Menu mMenu;
    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        MenuInflater inflater =getMenuInflater();
        mMenu = menu;
        inflater.inflate(R.menu.meue_sample,menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()){
            case R.id.action_logout:
                signOut();
                return true;
            case R.id.action_bt:
                Intent intent = new Intent(getApplicationContext(),BLTActivity.class);
                startActivity(intent);
            case R.id.action_connect:
                boolean connect = mClient.isConnection();
                    if (!connect) {
                        mDeviceListDialog.show();
                    } else {
                        mBTHandler.close();
                    }
                    return true;
            default:
                return  super.onOptionsItemSelected(item);
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mClient = BluetoothSerialClient.getInstance();

        mAuth = FirebaseAuth.getInstance();
        bton = (Button)findViewById(R.id.btOn);
        baby = (ImageView)findViewById(R.id.imageView);
        txstate = (TextView)findViewById(R.id.txstate);
        txtime =(TextView)findViewById(R.id.txtime);

        if(mClient == null) {
            Toast.makeText(getApplicationContext(), "Cannot use the Bluetooth device.", Toast.LENGTH_SHORT).show();
            finish();
        }
        overflowMenuInActionBar();
        initProgressDialog();
        initDeviceListDialog();
        bton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mchild2Ref.setValue("0");
                sendStringData("22");
            }
        });
    }

    private void overflowMenuInActionBar(){
        try {
            ViewConfiguration config = ViewConfiguration.get(this);
            Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
            if(menuKeyField != null) {
                menuKeyField.setAccessible(true);
                menuKeyField.setBoolean(config, false);
            }
        } catch (Exception ex) {
            // 무시한다. 3.x 이 예외가 발생한다.
            // 또, 타블릿 전용으로 만들어진 3.x 버전의 디바이스는 보통 하드웨어 버튼이 존재하지 않는다.
        }
    }


    @Override
    protected void onPause() {
        mClient.cancelScan(getApplicationContext());
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        enableBluetooth();


    }

    private void initProgressDialog() {
        mLoadingDialog = new ProgressDialog(this);
        mLoadingDialog.setCancelable(false);
    }



    private void initDeviceListDialog() {
        mDeviceArrayAdapter = new ArrayAdapter<String>(getApplicationContext(), R.layout.item_device);
        ListView listView = new ListView(getApplicationContext());
        listView.setAdapter(mDeviceArrayAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String item =  (String) parent.getItemAtPosition(position);
                for(BluetoothDevice device : mBluetoothDevices) {
                    if(item.contains(device.getAddress())) {
                        connect(device);
                        mDeviceListDialog.cancel();
                    }
                }
            }
        });
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Select bluetooth device");
        builder.setView(listView);
        builder.setPositiveButton("Scan",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        scanDevices();
                    }
                });
        mDeviceListDialog = builder.create();
        mDeviceListDialog.setCanceledOnTouchOutside(false);
    }

    private void addDeviceToArrayAdapter(BluetoothDevice device) {
        if(mBluetoothDevices.contains(device)) {
            mBluetoothDevices.remove(device);
            mDeviceArrayAdapter.remove(device.getName() + "\n" + device.getAddress());
        }
        mBluetoothDevices.add(device);
        mDeviceArrayAdapter.add(device.getName() + "\n" + device.getAddress() );
        mDeviceArrayAdapter.notifyDataSetChanged();

    }





    private void enableBluetooth() {
        BluetoothSerialClient btSet =  mClient;
        btSet.enableBluetooth(this, new BluetoothSerialClient.OnBluetoothEnabledListener() {
            @Override
            public void onBluetoothEnabled(boolean success) {
                if(success) {
                    getPairedDevices();
                } else {
                    finish();
                }
            }
        });
    }


    private void getPairedDevices() {
        Set<BluetoothDevice> devices =  mClient.getPairedDevices();
        for(BluetoothDevice device: devices) {
            addDeviceToArrayAdapter(device);
        }
    }

    private void scanDevices() {
        BluetoothSerialClient btSet = mClient;
        btSet.scanDevices(getApplicationContext(), new BluetoothSerialClient.OnScanListener() {
            String message ="";
            @Override
            public void onStart() {
                Log.d("Test", "Scan Start.");
                mLoadingDialog.show();
                message = "Scanning....";
                mLoadingDialog.setMessage("Scanning....");
                mLoadingDialog.setCancelable(true);
                mLoadingDialog.setCanceledOnTouchOutside(false);
                mLoadingDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        BluetoothSerialClient btSet = mClient;
                        btSet.cancelScan(getApplicationContext());
                    }
                });
            }

            @Override
            public void onFoundDevice(BluetoothDevice bluetoothDevice) {
                addDeviceToArrayAdapter(bluetoothDevice);
                message += "\n" + bluetoothDevice.getName() + "\n" + bluetoothDevice.getAddress();
                mLoadingDialog.setMessage(message);
            }

            @Override
            public void onFinish() {
                Log.d("Test", "Scan finish.");
                message = "";
                mLoadingDialog.cancel();
                mLoadingDialog.setCancelable(false);
                mLoadingDialog.setOnCancelListener(null);
                mDeviceListDialog.show();
            }
        });
    }


    private void connect(BluetoothDevice device) {
        mLoadingDialog.setMessage("Connecting....");
        mLoadingDialog.setCancelable(false);
        mLoadingDialog.show();
        BluetoothSerialClient btSet =  mClient;
        btSet.connect(getApplicationContext(), device, mBTHandler);
    }

    private BluetoothSerialClient.BluetoothStreamingHandler mBTHandler = new BluetoothSerialClient.BluetoothStreamingHandler(){
        ByteBuffer mmByteBuffer = ByteBuffer.allocate(1024);

        @Override
        public void onError(Exception e) {
            mLoadingDialog.cancel();
            Log.d("TAG","Messgae : Connection error - " +  e.toString() + "\n");
            mMenu.getItem(1).setTitle(R.string.action_connect);
        }

        @Override
        public void onDisconnected() {
            mMenu.getItem(1).setTitle(R.string.action_connect);
            mLoadingDialog.cancel();
            Log.d("TAG","Messgae : Disconnected.\n");
        }
        @Override
        public void onData(byte[] buffer, int length) {
            if(length == 0) return;
            if(mmByteBuffer.position() + length >= mmByteBuffer.capacity()) {
                ByteBuffer newBuffer = ByteBuffer.allocate(mmByteBuffer.capacity() * 2);
                newBuffer.put(mmByteBuffer.array(), 0,  mmByteBuffer.position());
                mmByteBuffer = newBuffer;
            }
            mmByteBuffer.put(buffer, 0, length);
            Log.d("Tag",new String(mmByteBuffer.array(),0,mmByteBuffer.position()));
            btBabyState = new String(mmByteBuffer.array(),0,mmByteBuffer.position());
            if(btBabyState.contains("1")){
                mchild1Ref.setValue("1");

            }
            else{
                mchild1Ref.setValue("0");
            }

            mmByteBuffer.clear();
        }

        @Override
        public void onConnected() {
            Log.d("TAG","Messgae : Connected. " + mClient.getConnectedDevice().getName() + "\n");
            mLoadingDialog.cancel();
            mMenu.getItem(1).setTitle(R.string.action_disconnect);
        }
    };

    public void sendStringData(String data) {
        data += '\0';
        byte[] buffer = data.getBytes();
        if(mBTHandler.write(buffer)) {
            ;
        }
    }

    protected void onDestroy() {
        super.onDestroy();
        mClient.claer();
    };

    @Override
    public void onStart() {
        super.onStart();
        sendStringData("1");
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        mchildRef = mConditionRef.child(currentUser.getUid());
        mchild1Ref = mchildRef.child("아기상태");
        mchild2Ref = mchildRef.child("흔들침대");
        mchild3Ref = mchildRef.child("울음시각");
        mchild1Ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String txt = dataSnapshot.getValue(String.class);
                if(txt.equals("1")){
                    btonoff=1;
                    baby.setImageResource(R.drawable.crying);
                    babystate =1;
                    txstate.setText("흔들침대 동작중...");
                    mchild2Ref.setValue("1");

                    SimpleDateFormat formatter = new SimpleDateFormat ( "MMddHHmmss", Locale.KOREAN );
                    SimpleDateFormat formatter1 = new SimpleDateFormat ( "HH시mm분", Locale.KOREAN );
                    Date currentTime = new Date ( );
                    String dTime = formatter.format ( currentTime );
                    String sTime = formatter1.format(currentTime);
                    txtime.setText("아이가 최근에 운 시각 : "+ dTime);
                    mchild3Ref.child(dTime).setValue(sTime);
                }
                else if(txt.equals("0"))
                {
                    babystate =0;
                    baby.setImageResource(R.drawable.smile);

                    txstate.setText("아기가 평온히 잠들어 있어요");                    }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
        mchild2Ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String txt = dataSnapshot.getValue(String.class);
                if(txt.equals("1")){
                    btonoff =1;
                    txstate.setText("흔들침대 동작중...");
                    bton.setText("흔들침대 동작끄기");
                }
                else{
                    btonoff=0;
                    if(babystate == 1){
                        txstate.setText("아이가 울고있어요ㅠㅠ");
                    }
                    else{
                        txstate.setText("아기가 평온히 잠들어 있어요");                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }
    private void signOut() {
        showProgressDialog();
        mAuth.signOut();
        hideProgressDialog();
        finish();
        Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
        startActivity(intent);
    }



}

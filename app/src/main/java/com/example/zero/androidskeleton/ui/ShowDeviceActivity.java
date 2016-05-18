package com.example.zero.androidskeleton.ui;

import android.app.Service;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.widget.ImageView;
import com.example.zero.androidskeleton.GlobalObjects;
import com.example.zero.androidskeleton.R;
import com.example.zero.androidskeleton.bt.BlueLockProtocol;
import com.example.zero.androidskeleton.bt.BtLeDevice;
import com.example.zero.androidskeleton.bt.BtLeService;
import com.example.zero.androidskeleton.component.PasswordEdit;
import com.example.zero.androidskeleton.log.Log;
import com.example.zero.androidskeleton.state.Context;
import com.example.zero.androidskeleton.state.State;
import com.example.zero.androidskeleton.state.StateMachine;
import com.example.zero.androidskeleton.ui.anim.Rotate3dAnimation;
import com.example.zero.androidskeleton.utils.Utils;

/**
 *
 */
public class ShowDeviceActivity extends BaseActivity implements NavigationView.OnNavigationItemSelectedListener, BtLeDevice.DeviceListener, SensorEventListener {
    private static final String TAG = "ShowDeviceActivity";

    private BtLeDevice mDevice = null;

    private SensorManager mSensorManager = null;
    private Vibrator mVibrator = null;

    private PasswordEdit mPasswordEdit;
    private ImageView mUnlockImg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_show_device_main);

        android.support.v7.widget.Toolbar toolbar = (android.support.v7.widget.Toolbar) findViewById(R.id.activity_show_device_toolbar);
        setSupportActionBar(toolbar);

        final DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout_show);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        View headView = navigationView.getHeaderView(0);
        ImageView imageView = (ImageView) headView.findViewById(R.id.backImg);
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawer.closeDrawer(GravityCompat.START);
            }
        });

        Intent intent = getIntent();
        if (intent == null || intent.getExtras() == null) {
            finish();
            return;
        }
        Bundle bundle = intent.getExtras();

        mDevice = BtLeService.INSTANCE.getDevice(bundle.getString("addr"));
        if (mDevice == null) {
            Utils.makeToast(this, "no device supplied");
            finish();
            return;
        }

        ActionBar actionBar = getSupportActionBar();
        Log.e(TAG, "action bar: " + actionBar);
        if (actionBar != null) {
            actionBar.setTitle(mDevice.getName());
        }

        setupUiComp();

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mVibrator = (Vibrator) getSystemService(Service.VIBRATOR_SERVICE);

        mDevice.addDeviceListener(this);
    }

    private void setupUiComp() {
        mPasswordEdit = (PasswordEdit) findViewById(R.id.password_edit);
        assert mPasswordEdit != null;

        mUnlockImg = (ImageView) findViewById(R.id.icon_mode_img);
        assert mUnlockImg != null;

        mPasswordEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (GlobalObjects.unlockMode != GlobalObjects.UNLOCK_MODE_AUTO) {
                    return;
                }

                String password = mPasswordEdit.getText().toString();
                if (password.length() != 6) {
                    return;
                }

                // cancel previous task
                // unlockSM.handle(EVENT_CANCEL, -1, null);

                // try this one
                unlockSM.handle(EVENT_UNLOCK, -1, password);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.show_device_menu, menu);

        final BtLeDevice.State state;
        if (mDevice == null) {
            state = BtLeDevice.State.DISCONNECTED;
        } else {
            state = mDevice.getState();
        }

        switch (state) {
            case DISCONNECTED:
                menu.findItem(R.id.menu_connect).setVisible(true);
                menu.findItem(R.id.menu_disconnect).setVisible(false);
                menu.findItem(R.id.menu_refresh).setActionView(null);
                break;
            case READY:
                menu.findItem(R.id.menu_connect).setVisible(false);
                menu.findItem(R.id.menu_disconnect).setVisible(true);
                menu.findItem(R.id.menu_refresh).setActionView(null);
                break;
            case CONNECTING:
            case CONNECTED:
            case DISCOVERING_SERVICE:
                menu.findItem(R.id.menu_connect).setVisible(false);
                menu.findItem(R.id.menu_disconnect).setVisible(true);
                menu.findItem(R.id.menu_refresh).setActionView(
                        R.layout.actionbar_indeterminate_progress);
                break;
            case DISCONNECTING:
                menu.findItem(R.id.menu_connect).setVisible(true);
                menu.findItem(R.id.menu_connect).setEnabled(false);
                menu.findItem(R.id.menu_disconnect).setVisible(false);
                menu.findItem(R.id.menu_refresh).setActionView(
                        R.layout.actionbar_indeterminate_progress);
                break;
            default:
                break;
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
            case R.id.menu_connect:
                mDevice.connectGatt(getApplicationContext());
                invalidateOptionsMenu();
                break;
            case R.id.menu_disconnect:
                mDevice.disconnectGatt();
                invalidateOptionsMenu();
                break;
            default:
                break;
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.i(TAG, "onResume");

        mSensorManager.unregisterListener(this);

        mUnlockImg.setImageResource(getUnlockImage());

        // 根据不同的模式决定界面如何显示
        switch (GlobalObjects.unlockMode) {
            case GlobalObjects.UNLOCK_MODE_MANUNAL: {
                mUnlockImg.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String password = mPasswordEdit.getText().toString();
                        if (password.length() != 6) {
                            Utils.makeToast(getApplicationContext(), "password incorrect");
                            return;
                        }
                        unlock(password);
                    }
                });
                break;
            }
            case GlobalObjects.UNLOCK_MODE_AUTO: {
                mUnlockImg.setOnClickListener(null);
                break;
            }
            case GlobalObjects.UNLOCK_MODE_SHAKE: {
                mUnlockImg.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String password = mPasswordEdit.getText().toString();
                        if (password.length() != 6) {
                            Utils.makeToast(getApplicationContext(), "password incorrect");
                            return;
                        }
                        unlock(password);
                    }
                });
                break;
            }
            default:
                break;
        }
    }

    private static int getUnlockImage() {
        switch (GlobalObjects.unlockMode) {
            case GlobalObjects.UNLOCK_MODE_MANUNAL:
                return R.drawable.icon_manualmode_lock;
            case GlobalObjects.UNLOCK_MODE_AUTO:
                return R.drawable.icon_automode_lock;
            case GlobalObjects.UNLOCK_MODE_SHAKE:
                return R.drawable.icon_rockmode_lock;
            default:
                return R.drawable.icon_manualmode_lock;
        }
    }

    private static int getUnlockImageSuccess() {
        switch (GlobalObjects.unlockMode) {
            case GlobalObjects.UNLOCK_MODE_MANUNAL:
                return R.drawable.icon_manualmode_succeed;
            case GlobalObjects.UNLOCK_MODE_AUTO:
                return R.drawable.icon_automode_succeed;
            case GlobalObjects.UNLOCK_MODE_SHAKE:
                return R.drawable.icon_rockmode_succeed;
            default:
                return R.drawable.icon_manualmode_succeed;
        }
    }

    @Override
    protected void onPause() {
        mSensorManager.registerListener(
            this,
            mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
            SensorManager.SENSOR_DELAY_NORMAL);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (mDevice != null) {
            mDevice.removeDeviceListener(this);
            mDevice.disconnectGatt();
        }
        super.onDestroy();
    }

    private void unlock(String password) {
        // FIXME: delete this line
        Log.d(TAG, "unlock: " + password);
        unlockSM.handle(EVENT_UNLOCK, -1, password);
    }

    private static final int EVENT_DEV_STATE_CHANGED = 1;
    private static final int EVENT_UNLOCK = 2;

    private final State IDLE = new IdleState();
    private final State WAIT_FOR_DISCONNECT = new WaitForDisconnectState();
    private final State WAIT_FOR_CONNECT = new WaitForConnectState();
    private final State READY = new ReadyState();

    private class IdleState implements State {
        @Override
        public void handle(Context context, int event, int arg, Object o) {
            switch (event) {
                case EVENT_UNLOCK:
                    handleUnlock(context, arg, o);
                default:
                    // ignore
                    break;
            }
        }

        private void handleUnlock(Context context, int arg, Object o) {
            if (!(o instanceof String)) {
                Log.e(TAG, "bad argument, string password expected");
                return;
            }

            String password = (String) o;

            // save password first
            context.putString("password", password);

            // then connect
            connect(context, arg, o);
        }

        private void connect(Context context, int arg, Object o) {
            BtLeDevice.State state = mDevice.getState();
            Log.d(TAG, "connect: current-state=" + state);

            switch (state) {
                case READY:
                    context.setState(READY);
                    context.handle(EVENT_UNLOCK, arg, o);
                    break;
                case DISCONNECTED:
                    mDevice.connectGatt(ShowDeviceActivity.this);
                    context.setState(WAIT_FOR_CONNECT);
                    break;
                default:
                    mDevice.disconnectGatt();
                    context.setState(WAIT_FOR_DISCONNECT);
                    break;
            }
        }
    }

    private class WaitForDisconnectState implements State {
        @Override
        public void handle(Context context, int event, int arg, Object o) {
            switch (event) {
                case EVENT_DEV_STATE_CHANGED:
                    BtLeDevice.State state = (BtLeDevice.State) o;
                    if (state == BtLeDevice.State.DISCONNECTED) {
                        mDevice.connectGatt(ShowDeviceActivity.this);
                        context.setState(WAIT_FOR_CONNECT);
                    } else {
                        Log.w(TAG, "expect disconnect event, received: " + state);
                    }
                    break;
                default:
                    // ignore
                    break;
            }
        }
    }

    private class WaitForConnectState implements State {
        @Override
        public void handle(Context context, int event, int arg, Object o) {
            switch (event) {
                case EVENT_DEV_STATE_CHANGED:
                    BtLeDevice.State state = (BtLeDevice.State) o;
                    handleStateChanged(context, state);
                    break;
                default:
                    // ignore
                    break;
            }
        }

        private void handleStateChanged(Context context, BtLeDevice.State state) {
            switch (state) {
                case READY:
                    context.setState(READY);
                    String password = context.getString("password", null);
                    Log.d(TAG, "context password: " + password);
                    if (password != null) {
                        context.handle(EVENT_UNLOCK, -1, password);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    private class ReadyState implements State {
        @Override
        public void handle(Context context, int event, int arg, Object o) {
            switch (event) {
                case EVENT_DEV_STATE_CHANGED:
                    // BtLeDevice.State state = (BtLeDevice.State) o;
                    // handleStateChanged(context, state);
                    context.setState(IDLE);
                    break;
                case EVENT_UNLOCK:
                    //String password = context.getString("password", null);
                    //if (password == null) {
                    //    log("no password , don't unlock");
                    //    return;
                    //}
                    String password = (String) o;
                    BluetoothGattCharacteristic char1 = mDevice.getCharacteristic(0xfff1);
                    if (char1 == null) {
                        Log.e(TAG, "failed to get characteristic 0xfff1");
                        return;
                    }
                    mDevice.writeCharacteristic(char1, BlueLockProtocol.unlock(password), new BtLeDevice.ResultListener<Boolean>() {
                        @Override
                        public void onResult(Boolean result) {
                            Log.d(TAG, "wrote password: " + result);
                        }
                    });
                    String phoneNum = Utils.getPhoneNum(getApplicationContext());
                    if (phoneNum == null || phoneNum.length() != 11) {
                        Utils.makeToast(getApplicationContext(), "invalid phone number read: " + phoneNum);
                        return;
                    }
                    mDevice.writeCharacteristic(char1, BlueLockProtocol.passPhone(phoneNum), new BtLeDevice.ResultListener<Boolean>() {
                        @Override
                        public void onResult(Boolean result) {
                            Log.d(TAG, "wrote phone num: " + result);
                        }
                    });

                    break;
                default:
                    // ignore
                    break;
            }
        }
    }

    /**
     * CONNECT --> OPEN --> TRANSFER-NUM -> DONE
     */
    private class UnlockSM extends StateMachine {
        UnlockSM() {
            init(IDLE);
        }
    }

    private final UnlockSM unlockSM = new UnlockSM();

    private class AutoUnlockTask extends Thread {
        private boolean isStop;
        @Override
        public void run() {
            // TODO
        }
    }

    private final Thread autoUnlockTask = new Thread(new AutoUnlockTask());

    @Override
    public void onDeviceStateChanged(final BtLeDevice.State state) {
        Log.e(TAG, "new state: " + state);

        unlockSM.handle(EVENT_DEV_STATE_CHANGED, -1, state);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                invalidateOptionsMenu();
            }
        });
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        byte[] value = characteristic.getValue();
        if (value == null || value.length <= 0) {
            // ignore
            return;
        }

        final byte result = characteristic.getValue()[0];
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Utils.makeToast(getApplicationContext(), BlueLockProtocol.getCodeDesc(result));
                if (result == BlueLockProtocol.RESULT_PASSWORD_CORRECT) {
                    playSuccessAnima();
                }
            }
        });

    }

    private static final long ANIME_INTERVAL = 100;
    private void playSuccessAnima() {
        mUnlockImg.setImageResource(getUnlockImage());
        Rotate3dAnimation anime1 = new Rotate3dAnimation(
                0, 90, mUnlockImg.getWidth()/2.0f, mUnlockImg.getHeight()/2.0f, 310.0f, true);
        anime1.setDuration(ANIME_INTERVAL);
        anime1.setFillAfter(true);
        anime1.setInterpolator(new AccelerateInterpolator());
        anime1.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mUnlockImg.setImageResource(getUnlockImageSuccess());
                Rotate3dAnimation anime2 = new Rotate3dAnimation(
                        90, 180, mUnlockImg.getWidth()/2.0f, mUnlockImg.getHeight()/2.0f, 310.0f, false);
                anime2.setDuration(ANIME_INTERVAL);
                anime2.setFillAfter(true);
                anime2.setInterpolator(new AccelerateInterpolator());
                mUnlockImg.startAnimation(anime2);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        mUnlockImg.startAnimation(anime1);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (GlobalObjects.unlockMode != GlobalObjects.UNLOCK_MODE_SHAKE) {
            return;
        }

        int sensorType = event.sensor.getType();
        float[] values = event.values;
        if (sensorType == Sensor.TYPE_ACCELEROMETER) {
            if ((Math.abs(values[0]) > 17 || Math.abs(values[1]) > 17 || Math.abs(values[2]) > 17)) {
                Log.d("sensor x ", "============ values[0] = " + values[0]);
                Log.d("sensor y ", "============ values[1] = " + values[1]);
                Log.d("sensor z ", "============ values[2] = " + values[2]);
                mVibrator.vibrate(500);

                String password = mPasswordEdit.getText().toString();
                if (password.length() == 6) {
                    unlock(password);
                } else {
                    Utils.makeToast(getApplicationContext(), "bad password");
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        switch (id) {
            case R.id.nav_show_password: {
                Intent intent = new Intent(this, ModifyPasswordActivity.class);
                startActivity(intent);
                break;
            }
            case R.id.nav_show_mode: {
                Intent intent = new Intent(this, ModeSettingActivity.class);
                startActivity(intent);
                break;
            }
            default:
                break;
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout_show);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}

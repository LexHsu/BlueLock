package com.example.zero.androidskeleton.ui;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import com.example.zero.androidskeleton.R;
import com.example.zero.androidskeleton.bt.BtLeDevice;
import com.example.zero.androidskeleton.bt.BtLeService;
import com.example.zero.androidskeleton.log.Log;
import com.example.zero.androidskeleton.sort.CharacterParser;
import com.example.zero.androidskeleton.sort.PinyinComparator;
import com.example.zero.androidskeleton.sort.SideBar;
import com.example.zero.androidskeleton.sort.SideBar.OnTouchingLetterChangedListener;
import com.example.zero.androidskeleton.sort.SortAdapter;
import com.example.zero.androidskeleton.sort.SortModel;
import com.example.zero.androidskeleton.utils.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SelectDeviceActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "SelectDeviceActivity";

    private Context mContext;
    private ListView mSortListView;
    private SideBar mSortSideBar;
    private TextView mSortDialog;
    private SortAdapter mSortAdapter;
    /**
     * 汉字转换成拼音的类
     */
    private CharacterParser characterParser;
    private List<BtLeDevice> SourceDateList = new ArrayList<>();
    /**
     * 根据拼音来排列ListView里面的数据类
     */
    private PinyinComparator pinyinComparator;


    private void log(final String msg) {
        Log.i(TAG, msg + '\n');
    }

    private class MyScanListener implements BtLeService.ScanListener {

        @Override
        public void onDeviceFound(BtLeDevice dev) {
            SourceDateList.add(dev);
            mSortAdapter = new SortAdapter(mContext, R.layout.select_list_item_device, SourceDateList);
            mSortListView.setAdapter(mSortAdapter);
        }

        @Override
        public void onScanChange(boolean isScanning) {
            invalidateOptionsMenu();
        }
    }

    private final MyScanListener mScanListener = new MyScanListener();

    private void checkAllMyPermission() {
        final String[] permission_list = {
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        };
        for (String permission: permission_list) {
            checkMyPermission(permission);
        }
    }

    private void checkMyPermission(String permission) {
        int result = ContextCompat.checkSelfPermission(getApplicationContext(), permission);
        if (result == PackageManager.PERMISSION_GRANTED) {
            return;
        }

        ActivityCompat.requestPermissions(this, new String[] { permission }, 0);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;

        checkAllMyPermission();

        setContentView(R.layout.activity_select_device_main);

        android.support.v7.widget.Toolbar toolbar = (android.support.v7.widget.Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);


        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Utils.makeToast(this, "该手机不支持低功耗蓝牙");
            finish();
        }

        if (BtLeService.INSTANCE.getAdapter() == null || BtLeService.INSTANCE.getScanner() == null) {
            Utils.makeToast(this, "蓝牙功能不支持或者开关未打开");
            finish();
        }

//        setupUiComp();

        initViews();
    }

    private void initViews() {
        //实例化汉字转拼音类
        characterParser = CharacterParser.getInstance();

        pinyinComparator = new PinyinComparator();

        mSortSideBar = (SideBar) findViewById(R.id.sidrbar);
        mSortDialog = (TextView) findViewById(R.id.dialog);
        mSortSideBar.setTextView(mSortDialog);

        //设置右侧触摸监听
        mSortSideBar.setOnTouchingLetterChangedListener(new OnTouchingLetterChangedListener() {

            @Override
            public void onTouchingLetterChanged(String s) {
                //该字母首次出现的位置
                int position = mSortAdapter.getPositionForSection(s.charAt(0));
                if(position != -1){
                    mSortListView.setSelection(position);
                }

            }
        });

        mSortListView = (ListView) findViewById(R.id.device_list_view);
        mSortListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //这里要利用adapter.getItem(position)来获取当前position所对应的对象
                BtLeService.INSTANCE.stopScan();

                // get selected info
                final BtLeDevice device = mSortAdapter.getItem(position);
                log("device: " + device.getName() + ", " + device.getAddress());

                Bundle bundle = new Bundle();
                bundle.putString("addr", device.getAddress());

                Intent intent = new Intent(SelectDeviceActivity.this, ShowDeviceActivity.class);
                // Intent intent = new Intent(SelectDeviceActivity.this, ModifyPasswordActivity.class);
                intent.putExtras(bundle);

                startActivity(intent);
            }
        });


        // 根据a-z进行排序源数据
        Collections.sort(SourceDateList, pinyinComparator);
        mSortAdapter = new SortAdapter(mContext, R.layout.select_list_item_device, SourceDateList);
        mSortListView.setAdapter(mSortAdapter);

    }


    private void startScan() {
        mSortAdapter.clear();
        BtLeService.INSTANCE.startScan();
        invalidateOptionsMenu();
    }

    private void stopScan() {
        BtLeService.INSTANCE.stopScan();
        invalidateOptionsMenu();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSortAdapter.clear();
        BtLeService.INSTANCE.addScanListener(mScanListener);
    }

    @Override
    protected void onPause() {
        BtLeService.INSTANCE.removeScanListener(mScanListener);
        stopScan();
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.select_device_menu, menu);
        // if (mScanner.isScanning()) {
        if (BtLeService.INSTANCE.isScanning()) {
            menu.findItem(R.id.menu_scan).setVisible(false);
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_refresh).setActionView(
                R.layout.actionbar_indeterminate_progress);
        } else {
            menu.findItem(R.id.menu_scan).setVisible(true);
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_refresh).setActionView(null);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
            case R.id.menu_scan:
                startScan();
                break;
            case R.id.menu_stop:
                stopScan();
                break;
            default:
                break;
        }
        return true;
    }

    private ToggleButton mAutoButton;
    
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}

package com.hwa.socketserverclient;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.TextView;

import com.hwa.socketserverclient.client.ClientFragement;
import com.hwa.socketserverclient.server.ServerFragement;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private ViewPager mViewPager;
    private BottomNavigationView mNavigationView;
    private MenuItem mMenuItem;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_server:
                    if(mViewPager != null) {
                        mViewPager.setCurrentItem(0);
                        initActionBar(0);
                    }
                    return true;
                case R.id.navigation_client:
                    if(mViewPager != null) {
                        mViewPager.setCurrentItem(1);
                        initActionBar(1);
                    }
                    return true;
            }
            return false;
        }
    };

    private ViewPager.OnPageChangeListener mOnPageChangeListener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(int position) {
            if (mMenuItem != null) {
                mMenuItem.setChecked(false);
            } else {
                mNavigationView.getMenu().getItem(0).setChecked(false);
            }
            mMenuItem = mNavigationView.getMenu().getItem(position);
            if(position == 0) {
                initActionBar(0);
            } else {
                initActionBar(1);
            }
            mMenuItem.setChecked(true);
        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }
    };



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mViewPager = (ViewPager) findViewById(R.id.viewpager);
        mViewPager.setOnPageChangeListener(mOnPageChangeListener);
        mNavigationView = (BottomNavigationView) findViewById(R.id.navigation);
        mNavigationView.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        setViewPagerData();
    }

    private void initActionBar(int id) {
        ActionBar actionBar = getSupportActionBar();
        if(id == 0) {
            actionBar.setTitle(getString(R.string.title_server));
        } else {
            actionBar.setTitle(getString(R.string.title_client));
        }
    }

    private void setViewPagerData() {
        ContentPagerAdapter adapter = new ContentPagerAdapter(getSupportFragmentManager());
        ArrayList<Fragment> fragmentArrayList = new ArrayList<Fragment>();
        fragmentArrayList.add(new ServerFragement());
        fragmentArrayList.add(new ClientFragement());
        adapter.setFragment(fragmentArrayList);
        mViewPager.setAdapter(adapter);
        mViewPager.setCurrentItem(0);
        initActionBar(0);
    }

}

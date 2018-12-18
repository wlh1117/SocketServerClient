package com.hwa.socketserverclient;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import java.util.ArrayList;

/**
 * Created by wenlihong on 18-3-9.
 */

public class ContentPagerAdapter extends FragmentPagerAdapter {
    private ArrayList<Fragment> mFragmentArrayList;

    public ContentPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    public void setFragment(ArrayList<Fragment> fragmentArrayList) {
        mFragmentArrayList = fragmentArrayList;
    }

    @Override
    public Fragment getItem(int position) {
        return mFragmentArrayList != null ? mFragmentArrayList.get(position):null;
    }

    @Override
    public int getCount() {
        return mFragmentArrayList != null ? mFragmentArrayList.size():0;
    }
}

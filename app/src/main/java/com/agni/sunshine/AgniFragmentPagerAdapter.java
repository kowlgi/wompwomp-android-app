package com.agni.sunshine;

import android.content.Context;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.Fragment;

/**
 * Created by kowlgi on 8/24/15.
 */
public class AgniFragmentPagerAdapter extends android.support.v4.app.FragmentPagerAdapter{
    final int PAGE_COUNT = 3;
    private String tabTitles[] = new String[] { "Publish", "Weather", "Explore" };
    private Context context;

    public AgniFragmentPagerAdapter(FragmentManager fm, Context context) {
        super(fm);
        this.context = context;
    }

    @Override
    public int getCount() {
        return PAGE_COUNT;
    }

    @Override
    public Fragment getItem(int position) {
        return MainActivityFragment.newInstance(position + 1);
    }

    @Override
    public CharSequence getPageTitle(int position) {
        // Generate title based on item position
        return tabTitles[position];
    }
}

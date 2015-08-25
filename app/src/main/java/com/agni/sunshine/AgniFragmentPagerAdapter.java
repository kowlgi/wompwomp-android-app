package com.agni.sunshine;


import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;

/**
 * Created by kowlgi on 8/24/15.
 */
public class AgniFragmentPagerAdapter extends android.support.v4.app.FragmentPagerAdapter{
    final int PAGE_COUNT = 2;
    private String tabTitles[] = new String[] { "Publish", "Explore"};
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
        if (position == 0) {
            return PublishFragment.newInstance(position + 1);
        }
        else {
            return ExploreFragment.newInstance(position + 1);
        }
    }

    @Override
    public CharSequence getPageTitle(int position) {
        // Generate title based on item position
        return tabTitles[position];
    }
}

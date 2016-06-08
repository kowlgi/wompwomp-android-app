package co.wompwomp.sunshine;



import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.ViewGroup;

import java.util.HashMap;


public class WompwompFragmentPagerAdapter extends android.support.v4.app.FragmentPagerAdapter{
    final int PAGE_COUNT = 2;
    private String tabTitles[] = new String[] { "Home", "Popular"};
    private HashMap<Integer, Fragment> mPageReferenceMap;

    public WompwompFragmentPagerAdapter(FragmentManager fm) {
        super(fm);
        mPageReferenceMap = new HashMap<>();
    }

    @Override
    public int getCount() {
        return PAGE_COUNT;
    }

    @Override
    public Fragment getItem(int position) {
        Fragment newFragment;
        if (position == 0) {
            newFragment = HomeFragment.newInstance();
        }
        else {
            newFragment = FeaturedFragment.newInstance();
        }

        mPageReferenceMap.put(position, newFragment);
        return newFragment;
    }

    @Override
    public void destroyItem (ViewGroup container, int position, Object object) {
        super.destroyItem(container, position, object);
        mPageReferenceMap.remove(position);
    }

    @Override
    public CharSequence getPageTitle(int position) {
        // Generate title based on item position
        return tabTitles[position];
    }

    public Fragment getFragment(int key) {
        return mPageReferenceMap.get(key);
    }

}


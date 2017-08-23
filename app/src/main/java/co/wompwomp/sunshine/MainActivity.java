package co.wompwomp.sunshine;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import android.preference.PreferenceManager;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashSet;

import co.wompwomp.sunshine.provider.FeedContract;
import co.wompwomp.sunshine.util.Utils;

public class MainActivity extends AppCompatActivity {

    private ViewPager mViewPager;
    private HashSet<String> mLikes = null;

    @SuppressLint("ShowToast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Utils.launchPermissionsDialogIfNecessary(this);
        setContentView(R.layout.activity_main);

        // Get the ViewPager and set it's PagerAdapter so that it can display items
        mViewPager = (ViewPager) findViewById(R.id.viewpager);
        if(mViewPager != null) {
            mViewPager.setAdapter(new WompwompFragmentPagerAdapter(getSupportFragmentManager()));
        }

        // Give the TabLayout the ViewPager
        TabLayout tabLayout = (TabLayout) findViewById(R.id.sliding_tabs);
        if(tabLayout != null) {
            tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);
            tabLayout.setupWithViewPager(mViewPager);
            tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    // do nothing
                    mViewPager.setCurrentItem(tab.getPosition());
                }

                @Override
                public void onTabUnselected(TabLayout.Tab tab) {
                    // do nothing
                }

                @Override
                public void onTabReselected(TabLayout.Tab tab) {
                    currentFragmentSmoothScrollToTop();
                }
            });
        }

        Toolbar myToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(myToolbar);
        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false); // disable default title
        }
        if(myToolbar != null) {
            myToolbar.setNavigationIcon(R.drawable.ic_appbar_wompwomp_newicon);
            myToolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    currentFragmentSmoothScrollToTop();
                }
            });
        }

        TextView toolbarTitle = (TextView) findViewById(R.id.toolbar_title);
        if(toolbarTitle != null) {
            toolbarTitle.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    currentFragmentSmoothScrollToTop();
                }
            });
        }

        if (Utils.fileExists(this, WompWompConstants.LIKES_FILENAME)) {
            try {
                mLikes =  getKeysFromFile(WompWompConstants.LIKES_FILENAME);
            } catch (java.lang.Exception e) {
                mLikes = new HashSet<>();
                e.printStackTrace();
            }
        } else mLikes = new HashSet<>();
    }

    @Override
    protected void onResume() {
        super.onResume();
        DateTime ldt = DateTime.now().withZone(DateTimeZone.UTC);
        PreferenceManager
                .getDefaultSharedPreferences(this)
                .edit()
                .putString(WompWompConstants.PREF_LAST_LOGGED_IN_TIMESTAMP, ldt.toString())
                .apply();
    }

    @Override
    protected void onPause(){
        super.onPause();
        try {
            FileOutputStream likesfos = openFileOutput(WompWompConstants.LIKES_FILENAME, Context.MODE_PRIVATE);
            ObjectOutputStream likesoos = new ObjectOutputStream(likesfos);
            likesoos.writeObject(mLikes);
            likesoos.close();
            likesfos.close();
        } catch (java.io.FileNotFoundException fnf) {
            fnf.printStackTrace();
        } catch (java.io.IOException ioe) {
            ioe.printStackTrace();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        Bundle extras = intent.getExtras();
        if (extras != null) {
            String itemId = extras.getString("itemid");
            if (itemId != null) {
                Answers.getInstance().logCustom(new CustomEvent("Push notification clicked")
                        .putCustomAttribute("itemlink", itemId));
                Utils.postToWompwomp(FeedContract.APP_PUSH_NOTIFICATION_CLICKED_URL + itemId, this);
                currentFragmentSmoothScrollToTop();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_rate_us) {
            Answers.getInstance().logCustom(new CustomEvent("Options menu: Rate"));
            Utils.showAppPageLaunchToast(this);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("http://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID));
            startActivity(intent);
            return true;
        } else if (id == R.id.action_share_app) {
            Answers.getInstance().logCustom(new CustomEvent("Options menu: Share_app"));
            Utils.showShareToast(this);
            startActivity(Intent.createChooser(Utils.getShareAppIntent(this),
                    getResources().getString(R.string.app_chooser_title)));
            return true;
        } else if (id == R.id.action_about) {
            FragmentManager fm = getSupportFragmentManager();
            AboutDialogFragment aboutDialogFragment = AboutDialogFragment.newInstance();
            aboutDialogFragment.show(fm, "dialog_about");
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void currentFragmentSmoothScrollToTop(){
        int index = mViewPager.getCurrentItem();
        WompwompFragmentPagerAdapter fragmentPagerAdapter =
                ((WompwompFragmentPagerAdapter)mViewPager.getAdapter());
        if(fragmentPagerAdapter != null) {
            SmoothScrollToTopListener listener =
                    (SmoothScrollToTopListener) fragmentPagerAdapter.getFragment(index);
            if(listener != null) listener.onSmoothScrollToTop();
        }
    }

    public void addLike(String id){
        mLikes.add(id);
    }

    public void removeLike(String id){
        mLikes.remove(id);
    }

    public Boolean hasLiked(String id){
        return mLikes.contains(id);
    }

    private HashSet<String> getKeysFromFile (String filePath) throws Exception {
        FileInputStream fis = openFileInput(filePath);
        ObjectInputStream ois = new ObjectInputStream(fis);
        HashSet<String> obj = (HashSet<String>) ois.readObject();
        ois.close();
        fis.close();
        return obj;
    }
}
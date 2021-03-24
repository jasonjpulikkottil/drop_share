package com.jdots.share.activity;

import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.LocaleList;
import android.os.Vibrator;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.multidex.BuildConfig;
import androidx.preference.PreferenceManager;


import com.anjlab.android.iab.v3.BillingProcessor;
import com.anjlab.android.iab.v3.TransactionDetails;
import com.facebook.ads.AudienceNetworkAds;
import com.github.javiersantos.appupdater.AppUpdater;
import com.github.javiersantos.appupdater.enums.UpdateFrom;
import com.jdots.share.fragment.HomeFragment;
import com.jdots.share.util.PowerfulActionModeSupport;
import com.jdots.share.util.AppUtils;
import com.jdots.share.R;
import com.jdots.share.model.NetworkDevice;
import com.jdots.share.service.CommunicationService;
import com.afollestad.materialdialogs.MaterialDialog;
import com.facebook.ads.AdSize;
import com.facebook.ads.AdView;
import com.genonbeta.android.framework.widget.PowerfulActionMode;
import com.google.android.material.navigation.NavigationView;

import es.dmoral.toasty.Toasty;

import static android.Manifest.permission.READ_PHONE_STATE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static com.facebook.FacebookSdk.getApplicationContext;

public class MainActivity
        extends Activity
        implements NavigationView.OnNavigationItemSelectedListener, PowerfulActionModeSupport
{
    public static final int REQUEST_PERMISSION_ALL = 1;
    private static final int PERMISSION_REQUEST_CODE = 200;
    private NavigationView mNavigationView;
    private DrawerLayout mDrawerLayout;
    private PowerfulActionMode mActionMode;
    private HomeFragment mHomeFragment;
    private IntentFilter mFilter = new IntentFilter();
    private BroadcastReceiver mReceiver = null;
    NetworkDevice localDevice;
    private long mExitPressTime;
    private int mChosenMenuItemId;
    BillingProcessor bp;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);


        try{ setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); }
        catch(Exception ignored){}

        final Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (checkPermission()) {


        } else {
            requestPermission();

        }
        mHomeFragment = (HomeFragment) getSupportFragmentManager().findFragmentById(R.id.activitiy_home_fragment);
        mActionMode = findViewById(R.id.content_powerful_action_mode);
        mNavigationView = findViewById(R.id.nav_view);
        mDrawerLayout = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, mDrawerLayout, toolbar, R.string.text_navigationDrawerOpen, R.string.text_navigationDrawerClose);
        mDrawerLayout.addDrawerListener(toggle);
        toggle.syncState();


        final AppUpdater appUpdater = new AppUpdater(this)
                .setUpdateFrom(UpdateFrom.GOOGLE_PLAY)
                .setCancelable(true)
               // .setIcon(R.drawable.)
                .setButtonDoNotShowAgain(null)
                .setTitleOnUpdateAvailable("UNINSTALL THIS APP AND INSTALL THE UPDATE");
        //.setContentOnUpdateAvailable("Uninstall this version and install the new version");
        appUpdater.setButtonDismissClickListener(new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toasty.info(getBaseContext(), "You may use new version of the app DropShare", Toast.LENGTH_LONG, true).show();
                //finish();
            }
        });

        appUpdater.start();





        AudienceNetworkAds.initialize(this);


        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        if( !(prefs.getBoolean("PRODUCT_PRO", false))) {
            AdView adView = new AdView(this, getString(R.string.facebook_banner_ad_unit), AdSize.BANNER_HEIGHT_50);
            LinearLayout adContainer = (LinearLayout) findViewById(R.id.banner_container);
            adContainer.addView(adView);
            adView.loadAd();

            adContainer.setVisibility(View.VISIBLE);

        }else
        { LinearLayout adContainer = (LinearLayout) findViewById(R.id.banner_container);

            adContainer.setVisibility(View.GONE);
        }



        Button actionHistory =  (Button) findViewById(R.id.history);
        actionHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ButtonSound();
                startActivity(new Intent(MainActivity.this, HistoryActivity.class));
            }
        });

        LinearLayout actionInvite = (LinearLayout) findViewById(R.id.invite);
        actionInvite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ButtonSound();
                startActivity(new Intent(MainActivity.this, InviteActivity.class));
            }
        });

        Button ButtonUpgrade = findViewById(R.id.buttonPro);
        ButtonUpgrade.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ButtonSound();
                onClickUpgradeButton();
            }
        });



        if(!BillingProcessor.isIabServiceAvailable(this)) {
            Toast("In-app billing service is unavailable, please upgrade Android Market/Play to version >= 3.9.16");
        }

        bp = new BillingProcessor(this, getString(R.string.LICENSE_KEY), getString(R.string.MERCHANT_ID), new BillingProcessor.IBillingHandler() {
            @Override
            public void onProductPurchased(@NonNull String productId, @Nullable TransactionDetails details) {
                Toast("Product Purchased: " + productId);
                purchaseUpdate();
            }
            @Override
            public void onBillingError(int errorCode, @Nullable Throwable error) {
                Toast("Billing Error: " + Integer.toString(errorCode));
            }
            @Override
            public void onBillingInitialized() {
                // Toast("onBillingInitialized");
                purchaseUpdate();
            }
            @Override
            public void onPurchaseHistoryRestored() {
                // Toast("onPurchaseHistoryRestored");
                for(String sku : bp.listOwnedProducts())
                    // Toast( "Owned Managed Product: " + sku);

                    purchaseUpdate();
            }
        });


        bp.initialize();

        Configuration configuration = getResources().getConfiguration();

        if (Build.VERSION.SDK_INT >= 24) {
            LocaleList list = configuration.getLocales();

            if (list.size() > 0)
                for (int pos = 0; pos < list.size(); pos++)
                    if (list.get(pos).toLanguageTag().startsWith("en")) {
                        break;
                    }
        }

        localDevice = AppUtils.getLocalDevice(this);

        ImageView imageView = findViewById(R.id.layout_profile_picture_image_default);
        ImageView editImageView = findViewById(R.id.layout_profile_picture_image_preferred);
        TextView deviceNameText = findViewById(R.id.header_default_device_name_text);
        TextView versionText = findViewById(R.id.header_default_device_version_text);

        deviceNameText.setText(localDevice.nickname);
        versionText.setText(localDevice.versionName);
        loadProfilePictureInto(localDevice.nickname, imageView);

        editImageView.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                startProfileEditor();
            }
        });


        mFilter.addAction(CommunicationService.ACTION_TRUSTZONE_STATUS);
        mDrawerLayout.addDrawerListener(new DrawerLayout.SimpleDrawerListener()
        {
            @Override
            public void onDrawerClosed(View drawerView)
            {
                applyAwaitingDrawerAction();
            }
        });

        mNavigationView.setNavigationItemSelectedListener(this);
        mActionMode.setOnSelectionTaskListener(new PowerfulActionMode.OnSelectionTaskListener()
        {
            @Override
            public void onSelectionTask(boolean started, PowerfulActionMode actionMode)
            {
                toolbar.setVisibility(!started ? View.VISIBLE : View.GONE);
            }
        });

      //  requestRequiredPermissions(false);

    }



    @Override
    protected void onStart()
    {
        super.onStart();
        createHeaderView();
    }

    @Override
    protected void onResume()
    {
        super.onResume();



        registerReceiver(mReceiver = new ActivityReceiver(), mFilter);
        requestTrustZoneStatus();
    }

    @Override
    protected void onPause()
    {
        super.onPause();

        if (mReceiver != null)
            unregisterReceiver(mReceiver);

        mReceiver = null;
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item)
    {
        mChosenMenuItemId = item.getItemId();

        if (mDrawerLayout != null)
            mDrawerLayout.closeDrawer(GravityCompat.START);

        return true;
    }

    @Override
    public void onBackPressed()
    {
            super.onBackPressed();
    }

    @Override
    public void onUserProfileUpdated()
    {
        createHeaderView();
    }

    public void ButtonSound() {
        try {
            final MediaPlayer mp = MediaPlayer.create(getApplicationContext(), R.raw.click);
            Vibrator vib = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
            vib.vibrate(50);
        }catch(Exception ignored){}

    }
    private void applyAwaitingDrawerAction()
    {

        /*


        if (mChosenMenuItemId == 0) {
        } else if (R.id.menu_activity_main_manage_devices == mChosenMenuItemId) {
            startActivity(new Intent(MainActivity.this, ConnectionManagerActivity.class)
                    .putExtra(ConnectionManagerActivity.EXTRA_REQUEST_TYPE, ConnectionManagerActivity.RequestType.RETURN_RESULT.toString())
                    .putExtra(ConnectionManagerActivity.EXTRA_ACTIVITY_SUBTITLE, "Manage Devices"));

          //  startActivity(new Intent(this, ManageDevicesActivity.class));

        } else if (R.id.menu_activity_home == mChosenMenuItemId) {
            startActivity(new Intent(this, MainActivity.class));
         //   finish();

        } else if (R.id.menu_activity_main_web_share == mChosenMenuItemId) {
            startActivity(new Intent(this, WebShareActivity.class));
        } else if (R.id.menu_activity_share == mChosenMenuItemId) {
            startActivity(new Intent(this, ContentSharingActivity.class));

        } else if (R.id.menu_activity_receive == mChosenMenuItemId) {
            startActivity(new Intent(this, ConnectionManagerActivity.class)
                    .putExtra(ConnectionManagerActivity.EXTRA_ACTIVITY_SUBTITLE, getString(R.string.text_receive))
                    .putExtra(ConnectionManagerActivity.EXTRA_REQUEST_TYPE, ConnectionManagerActivity.RequestType.MAKE_ACQUAINTANCE.toString()));

        } else

            */

            if (R.id.nav_share == mChosenMenuItemId) {


            Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
            sharingIntent.setType("text/plain");
            String shareBody = "Best File Sharing app download now. https://play.google.com/store/apps/details?id=" + getApplicationContext().getPackageName();
            sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Share App");
            sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
            startActivity(Intent.createChooser(sharingIntent, "Share via"));

        } else if (R.id.about_me == mChosenMenuItemId) {
            aboutMyApp();
        }

            /*

            else if (R.id.privacypolicy == mChosenMenuItemId) {
            AlertDialog.Builder alert = new AlertDialog.Builder(this);
            alert.setTitle("Privacy Policy");

            WebView wv = new WebView(this);
            wv.getSettings().setJavaScriptEnabled(true);
            wv.loadUrl("https://pharid.com/privacy-policy"); //Your Privacy Policy Url Here
            wv.setWebViewClient(new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    view.getSettings().setJavaScriptEnabled(true);
                    view.loadUrl(url);

                    return true;
                }
            });

            alert.setView(wv);
            alert.setNegativeButton("Close", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    dialog.dismiss();
                }
            });
            alert.show();
        }

        */

        else if (R.id.rate_us == mChosenMenuItemId) {


            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + getApplicationContext().getPackageName())));

        }


        else if (R.id.moreapp == mChosenMenuItemId) {

            Uri uri = Uri.parse("market://search?q=pub:" + "JDotsLab"); //Developer AC Name
            Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
            try {
                startActivity(goToMarket);
            } catch (ActivityNotFoundException e) {
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("http://play.google.com/store/search?q=pub:" + "JDotsLab"))); //Developer AC Name
            }
        }


        mChosenMenuItemId = 0;
    }

    private void aboutMyApp() {

        MaterialDialog.Builder bulder = new MaterialDialog.Builder(this)
                .title(R.string.app_name)
                .customView(R.layout.about, true)
                .backgroundColor(getResources().getColor(R.color.colorPrimary))
                .titleColorRes(android.R.color.white)
                .positiveText("MORE APPS")
                .positiveColor(getResources().getColor(android.R.color.white))
                .icon(getResources().getDrawable(R.mipmap.ic_launcher))
                .limitIconToDefaultSize()
                .onPositive((dialog, which) -> {

                    Uri uri = Uri.parse("market://search?q=pub:" + "JDotsLab"); //Developer AC Name
                    Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
                    try {
                        startActivity(goToMarket);
                    } catch (ActivityNotFoundException e) {
                        startActivity(new Intent(Intent.ACTION_VIEW,
                                Uri.parse("http://play.google.com/store/search?q=pub:" + "JDotsLab"))); //Developer AC Name
                    }
                });

        MaterialDialog materialDialog = bulder.build();

        TextView versionCode = (TextView) materialDialog.findViewById(R.id.version_code);
        versionCode.setText(String.valueOf("    DEVELOPED BY    " ));

        materialDialog.show();
    }

    private void createHeaderView()
    {
        View headerView = mNavigationView.getHeaderView(0);
        Configuration configuration = getApplication().getResources().getConfiguration();

        if (Build.VERSION.SDK_INT >= 24) {
            LocaleList list = configuration.getLocales();

            if (list.size() > 0)
                for (int pos = 0; pos < list.size(); pos++)
                    if (list.get(pos).toLanguageTag().startsWith("en")) {
                        break;
                    }
        }
        if (headerView != null) {
            NetworkDevice localDevice = AppUtils.getLocalDevice(getApplicationContext());

            ImageView imageView = headerView.findViewById(R.id.layout_profile_picture_image_default);
            ImageView editImageView = headerView.findViewById(R.id.layout_profile_picture_image_preferred);
            TextView deviceNameText = headerView.findViewById(R.id.header_default_device_name_text);
            TextView versionText = headerView.findViewById(R.id.header_default_device_version_text);

            deviceNameText.setText(localDevice.nickname);
            versionText.setText(localDevice.versionName);
            loadProfilePictureInto(localDevice.nickname, imageView);

            editImageView.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    startProfileEditor();
                }
            });
        }
    }

    @Override
    public PowerfulActionMode getPowerfulActionMode()
    {
        return mActionMode;
    }



    public void requestTrustZoneStatus()
    {
        AppUtils.startForegroundService(this, new Intent(this, CommunicationService.class)
                .setAction(CommunicationService.ACTION_REQUEST_TRUSTZONE_STATUS));
    }


    private class ActivityReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {

        }
    }

    private boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(getApplicationContext(), WRITE_EXTERNAL_STORAGE);
        int result2 = ContextCompat.checkSelfPermission(getApplicationContext(), READ_PHONE_STATE);
        return result == PackageManager.PERMISSION_GRANTED && result2 == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {

        ActivityCompat.requestPermissions(this, new String[]{WRITE_EXTERNAL_STORAGE, READ_PHONE_STATE}, PERMISSION_REQUEST_CODE);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0) {

                    boolean locationAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean contactAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    if (locationAccepted && contactAccepted)
                        Toast.makeText(this, "Permission Granted, Now you can access this app.", Toast.LENGTH_LONG).show();

                    else {
                        Toast.makeText(this, "Permission Denied, You cannot use this app.", Toast.LENGTH_LONG).show();

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            if (shouldShowRequestPermissionRationale(WRITE_EXTERNAL_STORAGE)) {
                                showMessageOKCancel("You need to allow access to both the permissions",
                                        new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                    requestPermissions(new String[]{WRITE_EXTERNAL_STORAGE, READ_PHONE_STATE},
                                                            PERMISSION_REQUEST_CODE);
                                                }
                                            }
                                        });
                                return;
                            }
                        }

                    }
                }


                break;
        }
    }

    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }
    private void Toast(String message) {

        Toasty.info(getBaseContext(), message, Toast.LENGTH_LONG, false).show();

    }
    private void onClickUpgradeButton() {

        bp.purchase(this,getString(R.string.PRODUCT_ID));

    }
    public void purchaseUpdate() {


        Button ButtonUpgrade = findViewById(R.id.buttonPro);
        TextView TextUpgrade = findViewById(R.id.textViewPro);



        bp.loadOwnedPurchasesFromGoogle();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        prefs.edit().putBoolean("PRODUCT_PRO", false).apply();




        if(bp.isPurchased(getString(R.string.PRODUCT_ID))) {
            prefs.edit().putBoolean("PRODUCT_PRO", true).apply();


            ButtonUpgrade.setVisibility(View.GONE);

            TextUpgrade.setText(getString(R.string.pro_text_purchased));


            LinearLayout adContainer = (LinearLayout) findViewById(R.id.banner_container);


            if( !(prefs.getBoolean("PRODUCT_PRO", false))) {

                adContainer.setVisibility(View.VISIBLE);

            }else
            {
                adContainer.setVisibility(View.GONE);
            }








        }else
        {
            ButtonUpgrade.setVisibility(View.VISIBLE);
            TextUpgrade.setText(getString(R.string.pro_text));

        }

    }
}

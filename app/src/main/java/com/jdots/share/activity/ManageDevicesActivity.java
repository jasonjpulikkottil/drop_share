package com.jdots.share.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;

import com.jdots.share.R;
import com.facebook.ads.AdSize;
import com.facebook.ads.AdView;

public class ManageDevicesActivity extends Activity
{
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_devices);

        try{ setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); }
        catch(Exception ignored){}

        final Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_close_white_24dp);

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
        findViewById(R.id.fixConnectionButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                startActivity(new Intent(ManageDevicesActivity.this, ConnectionManagerActivity.class)
                        .putExtra(ConnectionManagerActivity.EXTRA_REQUEST_TYPE, ConnectionManagerActivity.RequestType.RETURN_RESULT.toString())
                        .putExtra(ConnectionManagerActivity.EXTRA_ACTIVITY_SUBTITLE, getString(R.string.text_fixConnection)));
            }
        });


    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();

        if (id == android.R.id.home)
            finish();
        else
            return super.onOptionsItemSelected(item);

        return true;
    }
}

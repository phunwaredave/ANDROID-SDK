package com.phunware.testapplication;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.phunware.adcontainer.AdContainer;
import com.phunware.adcontainer.BannerContainer;
import com.phunware.adcontainer.InterstitialAdContainer;
import com.phunware.adcontainer.NativeAdContainer;
import com.phunware.adcontainer.RewardedVideoContainer;
import com.phunware.adcontainer.VideoContainer;
import com.phunware.adcontainer.context.PlacementType;
import com.phunware.adcontainer.listener.BannerOnLoadListener;
import com.phunware.adcontainer.listener.InterstitialListener;
import com.phunware.adcontainer.listener.NativeOnLoadListener;
import com.phunware.adcontainer.listener.OnCacheListener;
import com.phunware.info.device.ScreenInfo;
import com.phunware.info.provided.Gender;
import com.phunware.info.provided.UserKeywords;
import com.phunware.info.provided.YearOfBirth;
import java.util.Calendar;
import java.util.Collections;
import java.util.Random;

public class AdActivity extends AppCompatActivity {

  public static final String PLACEMENT_TYPE = "placement_type";

  private PlacementType mAdType;
  private TimeTracker mTimer;

  private TextView mStatusText;
  private View mContentView;
  private View mProgressView;
  private FrameLayout mAdContainer;
  private InterstitialAdContainer interstitialAdContainer;
  private BannerContainer bannerAdContainer;
  private VideoContainer videoContainer;
  private RewardedVideoContainer rewardedVideoContainer;
  private NativeAdContainer nativeAdContainer;
  private String reward = "coin";
  private int amount = 4;
  private EditText mPlacementEditText;

  public static void start(Context context, PlacementType placementType) {
    Intent intent = new Intent(context, AdActivity.class);
    intent.putExtra(PLACEMENT_TYPE, placementType);
    context.startActivity(intent);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_ad);

    mAdType = (PlacementType) getIntent().getExtras().get(PLACEMENT_TYPE);
    mTimer = new TimeTracker();

    // arrow back
    ActionBar actionBar = getSupportActionBar();
    if (actionBar != null) {
      actionBar.setHomeButtonEnabled(true);
      actionBar.setDisplayHomeAsUpEnabled(true);
      actionBar.setTitle(mAdType.getTitle());
    }

    // controls
    mStatusText = findViewById(R.id.status_text);
    mContentView = findViewById(R.id.control_layout);
    mProgressView = findViewById(R.id.loading_layout);
    mAdContainer = findViewById(R.id.ad_container);
    mPlacementEditText = findViewById(R.id.placementId_text);

    findViewById(R.id.load_button).setOnClickListener(view -> {
      // preparation
      mTimer.startTimeTracking();
      switchState(true);

      InputMethodManager inputManager = (InputMethodManager)
          this.getSystemService(Context.INPUT_METHOD_SERVICE);
      inputManager.hideSoftInputFromWindow(view.getWindowToken(),
          InputMethodManager.HIDE_NOT_ALWAYS);

      String placementId = mPlacementEditText.getText().toString();

      // processing
      if (mAdType == PlacementType.INTERSTITIAL) { // fullscreen banner
        loadInterstitial(placementId);
      } else if (mAdType == PlacementType.VIDEO) {
        loadVideo(placementId);
      } else if (mAdType == PlacementType.NATIVE) {
        loadNative(placementId);
      } else if (mAdType == PlacementType.REWARDED_VIDEO) {
        loadRewardedVideo(placementId);
      } else { // banners
        loadBanner(placementId);
      }
    });

    findViewById(R.id.show_button).setOnClickListener(view -> {
      if (mAdType == PlacementType.INTERSTITIAL) { // fullscreen banner
        showInterstitial();
      } else if (mAdType == PlacementType.VIDEO) {
        showVideo();
      } else if (mAdType == PlacementType.NATIVE) {
        showNative();
      } else if (mAdType == PlacementType.REWARDED_VIDEO) {
        showRewardedVideo();
      } else {
        showBanner();
      }
    });
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case android.R.id.home:
        onBackPressed();
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  private void switchState(boolean isContentLoading) {
    mContentView.setVisibility(isContentLoading ? View.GONE : View.VISIBLE);
    mProgressView.setVisibility(isContentLoading ? View.VISIBLE : View.GONE);
  }

  private void showResult(String result) {
    mStatusText.setText(
        String.format(getString(R.string.result_text), mTimer.stopTimeTracking() / 1000.00,
            result));
  }

  private void loadVideo(String placementId) {

    videoContainer = new VideoContainer(this, placementId);
    videoContainer.loadAd(provideFullScreenAdListener());
  }

  private void showVideo() {
    if (videoContainer != null) {
      videoContainer.showAd(provideOnCacheListener());
    } else {
      Toast.makeText(this, R.string.container_initialize, Toast.LENGTH_SHORT).show();
    }
  }

  private void loadRewardedVideo(String placementId) {

    rewardedVideoContainer = new RewardedVideoContainer(this, placementId, reward, amount);
    rewardedVideoContainer.loadAd(new InterstitialListener() {

      @Override
      public void closed() {
        // Fullscreen closed. Nothing to do here in example case
      }

      @Override
      public void onSuccess() {
        switchState(false);
        showResult(getString(R.string.success_load));
        Toast.makeText(AdActivity.this, R.string.success_load, Toast.LENGTH_SHORT).show();
      }

      @Override
      public void onFailure(Exception e) {
        switchState(false);
        showResult(e.getMessage());
      }
    });
  }

  private void showRewardedVideo() {
    if (rewardedVideoContainer != null) {
      rewardedVideoContainer.showAd(provideOnCacheListener());
      showResult("We win present: "
          + reward
          + "\n"
          + "count: "
          + amount);
      Toast.makeText(AdActivity.this, "We win present: " + reward + "\n" + "count: " + amount, Toast.LENGTH_SHORT).show();
    } else {
      Toast.makeText(this, R.string.container_initialize, Toast.LENGTH_SHORT).show();
    }
  }

  private void loadInterstitial(String placementId) {

    interstitialAdContainer = new InterstitialAdContainer(this, placementId);
    interstitialAdContainer.loadAd(

        provideFullScreenAdListener());
  }

  private void showInterstitial() {
    if (interstitialAdContainer != null) {
      interstitialAdContainer.showAd(provideOnCacheListener());
    } else {
      Toast.makeText(this, R.string.container_initialize, Toast.LENGTH_SHORT).show();
    }
  }

  private void loadBanner(String placementId) {
    // create custom banner container
    bannerAdContainer =
        new BannerContainer(this, mAdType.getWidth(), mAdType.getHeight(), 30, placementId);
    fillSomeTargetingData(bannerAdContainer); // adding targeting params
    mAdContainer.removeAllViews();
    bannerAdContainer.loadAd(provideBannerAdListener());
  }

  private void showBanner() {
    if (bannerAdContainer != null) {
      bannerAdContainer.showAd(provideOnCacheListener());
    } else {
      Toast.makeText(this, R.string.container_initialize, Toast.LENGTH_SHORT).show();
    }
  }

  private void loadNative(String placementId) {
    nativeAdContainer = new NativeAdContainer(this, placementId);
    nativeAdContainer.loadAd(provideNativeAdListener());
  }

  private void showNative() {
    if (nativeAdContainer != null) {
      RecyclerView recyclerView = new RecyclerView(this);
      recyclerView.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
          ScreenInfo.getScreenInfo(this).height / 2)); // List occupy half of the screen
      mAdContainer.addView(recyclerView);
      NativeAdapter nativeAdapter =
          new NativeAdapter(nativeAdContainer, provideNativeOnCacheListener());
      recyclerView.setLayoutManager(new LinearLayoutManager(this));
      recyclerView.setAdapter(nativeAdapter);
    } else {
      Toast.makeText(this, R.string.container_initialize, Toast.LENGTH_SHORT).show();
    }
  }

  private void fillSomeTargetingData(AdContainer adContainer) {
    Random random = new Random();

    Gender randomGender = Gender.values()[random.nextInt(Gender.values().length)];

    int maxAge = 100;
    YearOfBirth yearOfBirth = new YearOfBirth(
        Calendar.getInstance().get(Calendar.YEAR) - maxAge + random.nextInt(maxAge));

    UserKeywords userKeywords = new UserKeywords().addKeywords("food", "music")
        .addKeywords(Collections.singletonList("games"));

    adContainer.addTargetingData(randomGender)
        .addTargetingData(yearOfBirth)
        .addTargetingData(userKeywords);
  }

  private InterstitialListener provideFullScreenAdListener() {
    return new InterstitialListener() {
      @Override
      public void closed() {
        // Fullscreen closed. Nothing to do here in example case
      }

      @Override
      public void onSuccess() {
        switchState(false);
        showResult(getString(R.string.success_load));
        Toast.makeText(AdActivity.this, R.string.success_load, Toast.LENGTH_SHORT).show();
      }

      @Override
      public void onFailure(Exception e) {
        switchState(false);
        showResult(e.getMessage());
      }
    };
  }

  private BannerOnLoadListener provideBannerAdListener() {
    return new BannerOnLoadListener() {

      @Override
      public void onSuccess() {
        switchState(false);
        mAdContainer.addView(bannerAdContainer);
        showResult(getString(R.string.success_load));
        Toast.makeText(AdActivity.this, R.string.success_load, Toast.LENGTH_SHORT).show();
      }

      @Override
      public void onFailure(Exception e) {
        switchState(false);
        showResult(e.getMessage());
      }
    };
  }

  private NativeOnLoadListener provideNativeAdListener() {
    return new NativeOnLoadListener() {

      @Override
      public void onSuccess() {
        switchState(false);
        showResult(getString(R.string.success_load));
        Toast.makeText(AdActivity.this, R.string.success_load, Toast.LENGTH_SHORT).show();
      }

      @Override
      public void onFailure(Exception e) {
        switchState(false);
        showResult(e.getMessage());
      }
    };
  }

  private OnCacheListener provideOnCacheListener() {
    return () -> {
      mAdContainer.removeAllViews();
      showResult(getString(R.string.no_any_ad));
      Toast.makeText(AdActivity.this, R.string.no_any_ad, Toast.LENGTH_SHORT).show();
    };
  }

  private OnCacheListener provideNativeOnCacheListener() {
    return () -> {
      showResult(getString(R.string.no_any_ad));
      Toast.makeText(AdActivity.this, R.string.no_any_ad, Toast.LENGTH_SHORT).show();
    };
  }

  private class TimeTracker {
    private long mTimestamp;

    void startTimeTracking() {
      mTimestamp = System.currentTimeMillis();
    }

    long stopTimeTracking() {
      return System.currentTimeMillis() - mTimestamp;
    }
  }
}



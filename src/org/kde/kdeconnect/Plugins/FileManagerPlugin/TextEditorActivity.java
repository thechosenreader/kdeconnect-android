package org.kde.kdeconnect.Plugins.FileManagerPlugin;

import android.util.Log;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import org.kde.kdeconnect.async.BackgroundJob;
import org.kde.kdeconnect.BackgroundService;
import org.kde.kdeconnect_tp.R;
import org.kde.kdeconnect_tp.databinding.ActivityTextEditorBinding;

import java.util.Objects;


public class TextEditorActivity extends AppCompatActivity {
  private String deviceId;
  private String targetFilePath;
  private String cacheFilePath;

  private boolean downReady = false;
  private boolean downError = false;

  private boolean upReady = false;
  private boolean upError = false;

  private ActivityTextEditorBinding viewBinding;

  private final BackgroundJob.Callback<Void> fileReceivedCallback = new BackgroundJob.Callback<Void>() {
    @Override
    public void onResult(@NonNull BackgroundJob job, Void result) {
      Log.d("TextEditorActivity", "fileReceivedCallback - got " + targetFilePath);
      downReady = true;

      updateView();
    }

    @Override
    public void onError(@NonNull BackgroundJob job, @NonNull Throwable error) {
      Log.e("TextEditorActivity", "error receiving file", error);
      downError = true;

      updateView();
    }
  };

  private final BackgroundJob.Callback<Void> fileUploadedCallback = new BackgroundJob.Callback<Void>() {
    @Override
    public void onResult(@NonNull BackgroundJob job, Void result) {
      Log.d("TextEditorActivity", "fileReceivedCallback - got " + targetFilePath);
      upReady = true;

      updateView();
    }

    @Override
    public void onError(@NonNull BackgroundJob job, @NonNull Throwable error) {
      Log.e("TextEditorActivity", "error receiving file", error);
      upError = true;

      updateView();
    }
  };


  private void updateView() {
    if (!downReady && !downError) {
      viewBinding.textView.setText("Loading..");
    } else if (downReady) {
      viewBinding.textView.setText(String.format("File %s downloaded to %s", targetFilePath, cacheFilePath));
    } else if (downError) {
      viewBinding.textView.setText("Error :(");
    }
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    init();
    viewBinding = ActivityTextEditorBinding.inflate(getLayoutInflater());
    setContentView(viewBinding.getRoot());
    setSupportActionBar(viewBinding.toolbarLayout.toolbar);
    Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
    getSupportActionBar().setDisplayShowHomeEnabled(true);

    updateView();
  }

  private void init() {
    if (deviceId == null) {
      deviceId = getIntent().getStringExtra("deviceId");
      FileManagerPlugin plugin = (FileManagerPlugin) BackgroundService.getInstance().getDevice(deviceId).getPlugin(FileManagerPlugin.class);

      targetFilePath = getIntent().getStringExtra("targetFilePath");
      cacheFilePath = getIntent().getStringExtra("cacheFilePath");

      if (plugin.isCached(targetFilePath)) {
        cacheFilePath = plugin.getCachedPath(targetFilePath, "");
        downReady = true;
      } else {
        plugin.setFileReceivedCallback(fileReceivedCallback);
        plugin.requestDownloadForViewing(cacheFilePath, targetFilePath);
      }

      Log.d("TextEditorActivity", String.format("init with target = %s, cache = %s", targetFilePath, cacheFilePath));
    }
  }

}

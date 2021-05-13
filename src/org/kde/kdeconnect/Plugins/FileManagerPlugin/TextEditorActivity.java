package org.kde.kdeconnect.Plugins.FileManagerPlugin;

import android.util.Log;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.Menu;
import android.widget.Toast;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import org.kde.kdeconnect.async.BackgroundJob;
import org.kde.kdeconnect.async.BackgroundJobHandler;
import org.kde.kdeconnect.BackgroundService;
import org.kde.kdeconnect_tp.R;
import org.kde.kdeconnect_tp.databinding.ActivityTextEditorBinding;

import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Objects;


public class TextEditorActivity extends AppCompatActivity {
  private String deviceId;
  private String targetFilePath;
  private String cacheFilePath;

  private boolean downReady = false;
  private boolean downError = false;

  private boolean upReady = false;
  private boolean upError = false;

  private boolean loaded = false;

  private ActivityTextEditorBinding viewBinding;

  private BackgroundJobHandler backgroundJobHandler = BackgroundJobHandler.newFixedThreadPoolBackgroundJobHander(5);

  class LoadingJob extends BackgroundJob<String, String> {
    private String pathToLoad;
    public LoadingJob(String pathToLoad, BackgroundJob.Callback<String> callback) {
      super(pathToLoad, callback);
      this.pathToLoad = pathToLoad;
    }

    @Override
    public void run() {
      try {
        File file = new File(cacheFilePath);
        StringBuilder fileText = new StringBuilder((int) file.length());
        BufferedReader br = new BufferedReader(new FileReader(file));
        String st;
        while ((st = br.readLine()) != null) {
          fileText.append(st + "\n");
        }
        br.close();
        reportResult(fileText.toString());
      } catch (Exception e) {
          reportError(e);
        }
    }
  }



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
      viewBinding.textView.setText(String.format(TextEditorActivity.this.getResources().getString(R.string.errordownload), error.getMessage()));
      downError = true;
    }
  };



  private final BackgroundJob.Callback<Void> fileUploadedCallback = new BackgroundJob.Callback<Void>() {
    @Override
    public void onResult(@NonNull BackgroundJob job, Void result) {
      runOnUiThread(() -> Toast.makeText(TextEditorActivity.this, TextEditorActivity.this.getResources().getString(R.string.uploaded) + targetFilePath, Toast.LENGTH_SHORT).show());
      upReady = true;

      updateView();
    }

    @Override
    public void onError(@NonNull BackgroundJob job, @NonNull Throwable error) {
      Log.e("TextEditorActivity", "error uploading file", error);
      runOnUiThread(() -> Toast.makeText(TextEditorActivity.this, String.format(TextEditorActivity.this.getResources().getString(R.string.errorupload), error.getMessage()), Toast.LENGTH_SHORT).show());
      upError = true;

      updateView();
    }
  };



  private final BackgroundJob.Callback<String> fileLoadedCallback = new BackgroundJob.Callback<String>() {
    @Override
    public void onResult(@NonNull BackgroundJob job, String result) {
      runOnUiThread(() -> {
        viewBinding.textView.setText(result);
        loaded = true;
        updateView();
      });
    }

    @Override
    public void onError(@NonNull BackgroundJob job, @NonNull Throwable error) {
      Log.e("TextEditorActivity", "exception reading file " + cacheFilePath, error);
      runOnUiThread(() -> {
        viewBinding.textView.setText(String.format(TextEditorActivity.this.getResources().getString(R.string.errorread), error.getMessage()));
        Toast.makeText(TextEditorActivity.this, String.format(TextEditorActivity.this.getResources().getString(R.string.errorread), error.getMessage()), Toast.LENGTH_SHORT).show();
      });

    }
  };

  private void updateView() {
    runOnUiThread(() -> {
      ((TextView) findViewById(R.id.toolbar_title)).setText(targetFilePath);
      if (!loaded) {
        viewBinding.textView.setText(R.string.loading);
        if (downReady) {
          LoadingJob job = new LoadingJob(cacheFilePath, fileLoadedCallback);
          backgroundJobHandler.runJob(job);
        }
      }
    });
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    init();
    viewBinding = ActivityTextEditorBinding.inflate(getLayoutInflater());
    viewBinding.textView.setText("Loading..");
    setContentView(viewBinding.getRoot());
    setSupportActionBar(viewBinding.toolbarLayout.toolbar);
    Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
    getSupportActionBar().setDisplayShowHomeEnabled(true);

    getSupportActionBar().setDisplayShowTitleEnabled(false);
    getSupportActionBar().setDisplayShowCustomEnabled(true);
    getSupportActionBar().setCustomView(R.layout.toolbar_title_layout);



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

  @Override
  public boolean onCreateOptionsMenu(@NonNull Menu menu) {
    super.onCreateOptionsMenu(menu);
    getMenuInflater().inflate(R.menu.texteditor_options, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch(item.getItemId()) {
      case R.id.fm_save:
      save();
      break;

      case R.id.fm_upload:
      upload();
      break;

      case R.id.fm_save_upload:
      save();
      upload();
      break;
    }

    return true;
  }

  private void save() {
    try {
      FileWriter fw = new FileWriter(new File(cacheFilePath));
      fw.write(viewBinding.textView.getText().toString());
      fw.close();

      runOnUiThread(() -> Toast.makeText(this, "saved " + targetFilePath, Toast.LENGTH_SHORT).show());
    } catch (IOException ioe) {
        Log.e("TextEditorActivity", "error saving file " + cacheFilePath, ioe);
        runOnUiThread(() -> {
          Toast.makeText(this, String.format("Could not save file\n%s", ioe.getMessage()), Toast.LENGTH_LONG).show();
        });
    }
  }

  private void upload() {
    BackgroundService.RunWithPlugin(this, deviceId, FileManagerPlugin.class, plugin -> {
      plugin.setFileUploadedCallback(fileUploadedCallback);
      plugin.requestUpload(targetFilePath);
    });

  }

}

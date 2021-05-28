package org.kde.kdeconnect.Plugins.FileManagerPlugin;

import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.Menu;
import android.widget.Toast;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import org.kde.kdeconnect.async.BackgroundJob;
import org.kde.kdeconnect.async.BackgroundJobHandler;
import org.kde.kdeconnect.BackgroundService;
import org.kde.kdeconnect_tp.R;
import org.kde.kdeconnect_tp.databinding.ActivityTextEditorBinding;

import java.io.File;
// import java.nio.file.Files;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.FileReader;
import java.io.BufferedReader;
// import java.io.RandomAccessFile;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.Objects;


public class TextEditorActivity extends AppCompatActivity {
  private static String deviceId;
  private static String targetFilePath;
  private static String cacheFilePath;

  private boolean downReady = false;
  private boolean downError = false;

  private boolean upReady = false;
  private boolean upError = false;

  private boolean loaded = false;

  private ActivityTextEditorBinding viewBinding;

  private BackgroundJobHandler backgroundJobHandler = BackgroundJobHandler.newFixedThreadPoolBackgroundJobHander(5);

  static class LoadingJob extends BackgroundJob<String, ArrayDeque<String>> {
    private final String pathToLoad;
    private final ArrayDeque<String> strings = new ArrayDeque<>();
    private final Integer readChunkSize = 10240;  // 10 Kb; amount to read each time
    private final Integer addChunkSize = 1024;  // 1 Kb; amount for each string added to queue

    public LoadingJob(String pathToLoad, BackgroundJob.Callback<ArrayDeque<String>> callback) {
      super(pathToLoad, callback);
      this.pathToLoad = pathToLoad;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void run() {
      try {
        /* this is probably less fast than BufferedReader but no where near enough to warrant
        but its plenty fast enough, plus ive already thrown performance to the wind so who tf cares */
        byte[] bytes = Files.readAllBytes(Paths.get(cacheFilePath));
        int off = 0;
        while (off < bytes.length) {
          String chunk = new String(bytes, off, Math.min(addChunkSize, bytes.length - off));
          strings.add(chunk);

          off += addChunkSize;
        }

        reportResult(strings);
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



  private final BackgroundJob.Callback<ArrayDeque<String>> fileLoadedCallback = new BackgroundJob.Callback<ArrayDeque<String>>() {
    @Override
    public void onResult(@NonNull BackgroundJob job, ArrayDeque<String> result) {
      Log.d("FileManagerPlugin", "file loaded, setting text");
      viewBinding.scrollView.setBinding(viewBinding);
      viewBinding.scrollView.setStrings(result);
      viewBinding.textView.setText("");
      viewBinding.scrollView.addToEditText(10);
      runOnUiThread(() -> {
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
    ((TextView) findViewById(R.id.toolbar_title)).setText(targetFilePath);
    if (!loaded) {
      viewBinding.textView.setText(R.string.loading);
      if (downReady) {
        LoadingJob job = new LoadingJob(cacheFilePath, fileLoadedCallback);
        backgroundJobHandler.runJob(job);
      }
    }
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    init();
    viewBinding = ActivityTextEditorBinding.inflate(getLayoutInflater());
    // viewBinding.
    viewBinding.textView.setText(R.string.loading);
    setContentView(viewBinding.getRoot());
    setSupportActionBar(viewBinding.toolbarLayout.toolbar);
    Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
    getSupportActionBar().setDisplayShowHomeEnabled(true);

    getSupportActionBar().setDisplayShowTitleEnabled(false);
    getSupportActionBar().setDisplayShowCustomEnabled(true);
    getSupportActionBar().setCustomView(R.layout.toolbar_title_layout);
  }

  private void init() {
    if (deviceId == null) {
      deviceId = getIntent().getStringExtra("deviceId");
      BackgroundService.RunWithPlugin(this, deviceId, FileManagerPlugin.class, plugin -> {
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
        updateView();
      });
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
    int id = item.getItemId();
    if (id == R.id.fm_save) {
      BackgroundService.RunCommand(this, service -> save());
    }

    else if (id == R.id.fm_upload) {
      upload();
    }

    else if (id == R.id.fm_save_upload) {
      BackgroundService.RunCommand(this, service -> {
        save();
        upload();
      });
    }

    else {
      return super.onOptionsItemSelected(item);
    }

    return true;
  }

  @Nullable
  @Override
  public Intent getParentActivityIntent() {
    Intent intent = super.getParentActivityIntent();
    if (intent != null) {
      enhanceParentActivityIntent(intent);
    }
    return intent;
  }

  private void enhanceParentActivityIntent(Intent intent) {
    intent.putExtra("deviceId", deviceId);
  }

  private void save() {
    try {
      viewBinding.scrollView.setBinding(viewBinding);
      ArrayDeque<String> strings = viewBinding.scrollView.getStrings();
      FileWriter fw = new FileWriter(new File(cacheFilePath));
      fw.write(viewBinding.textView.getText().toString());
      while (!strings.isEmpty()) {
        fw.write(strings.pop());
      }
      fw.close();
      // RandomAccessFile f = new RandomAccessFile(new File(cacheFilePath), "rwd");
      // byte[] toWrite = viewBinding.textView.getText().toString().getBytes();
      // f.write(toWrite);

      runOnUiThread(() -> Toast.makeText(this, "saved " + targetFilePath, Toast.LENGTH_SHORT).show());
    } catch (IOException ioe) {
        Log.e("TextEditorActivity", "error saving file " + cacheFilePath, ioe);
        runOnUiThread(() -> Toast.makeText(this, String.format("Could not save file\n%s", ioe.getMessage()), Toast.LENGTH_LONG).show());
    }
  }

  private void upload() {
    BackgroundService.RunWithPlugin(this, deviceId, FileManagerPlugin.class, plugin -> {
      plugin.setFileUploadedCallback(fileUploadedCallback);
      plugin.requestUpload(targetFilePath);
    });

  }


}

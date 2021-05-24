
package org.kde.kdeconnect.Plugins.FileManagerPlugin;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.os.Handler;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;
import android.widget.AdapterView;
import android.widget.Toast;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import org.kde.kdeconnect.Helpers.RandomHelper;
import org.kde.kdeconnect.BackgroundService;
import org.kde.kdeconnect.UserInterface.List.ListAdapter;
import org.kde.kdeconnect.UserInterface.ThemeUtil;
import org.kde.kdeconnect_tp.R;
import org.kde.kdeconnect_tp.databinding.ActivityFileManagerBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class FileManagerActivity extends AppCompatActivity {
  private ActivityFileManagerBinding binding;
  private List<FileEntry> directoryItems;
  private int lastViewedPosition;

  private String deviceId;
  private String currentDirectory;

  private final FileManagerPlugin.ListingChangedCallback listingChangedCallback = this::updateView;
  private void updateView() {
    BackgroundService.RunWithPlugin(this, deviceId, FileManagerPlugin.class, plugin -> runOnUiThread(() -> {

      // set toolbar title to current path
      currentDirectory = plugin.getCurrentDirectory();
      if (currentDirectory.equals("")) {
        plugin.requestDirectoryListing();
        return;
      }

      ((TextView) findViewById(R.id.toolbar_title)).setText(String.format("%s", currentDirectory));

      // create Toasts for received error messages
      ArrayList<String> errors = plugin.getErrorMessages();
      plugin.clearErrorMessages();

      // display all error messages
      Handler handler = new Handler();
      if (!errors.isEmpty())
        Toast.makeText(FileManagerActivity.this, errors.get(0), Toast.LENGTH_SHORT).show();

      for (int i = 1; i < errors.size(); i++) {
        String error = errors.get(i);
        handler.postDelayed(() -> Toast.makeText(FileManagerActivity.this, error, Toast.LENGTH_SHORT).show(), Toast.LENGTH_SHORT);
      }

      registerForContextMenu(binding.directoryListing);

      directoryItems = plugin.getDirectoryItems();
      ListAdapter adapter = new ListAdapter(FileManagerActivity.this, directoryItems);
      binding.directoryListing.setAdapter(adapter);

      binding.directoryListing.setOnItemClickListener((adapterView, view1, i, l) -> {
        updateLastViewedPosition(plugin);
        FileEntry selectedItem = directoryItems.get(i);

        if (!selectedItem.isReadable()) {
          new AlertDialog.Builder(FileManagerActivity.this)
                  .setTitle(R.string.fm_permission_denied)
                  .setMessage(R.string.fm_permission_denied_desc)
                  .setPositiveButton(R.string.ok, null)
                  .show();
        }

        else if (selectedItem.isDirectory()) {
          plugin.requestDirectoryListing(selectedItem.getAbsPath());
        }

        else {
          new AlertDialog.Builder(FileManagerActivity.this)
            .setTitle(R.string.fm_confirm_download)
            .setMessage(String.format(FileManagerActivity.this.getResources().getString(R.string.fm_confirm_download_desc), selectedItem.getAbsPath()))
            .setPositiveButton(R.string.ok, (dialog, which) -> {
              plugin.requestDownload(selectedItem.getAbsPath());
              dialog.dismiss();
            })
            .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.cancel())
            .show();

        }
      });

      gotoLastPosition(plugin);

    }));
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    ThemeUtil.setUserPreferredTheme(this);

    binding = ActivityFileManagerBinding.inflate(getLayoutInflater());
    setContentView(binding.getRoot());
    setSupportActionBar(binding.toolbarLayout.toolbar);
    Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
    getSupportActionBar().setDisplayShowHomeEnabled(true);
    getSupportActionBar().setDisplayShowTitleEnabled(false);
    getSupportActionBar().setDisplayShowCustomEnabled(true);
    getSupportActionBar().setCustomView(R.layout.toolbar_title_layout);
    // getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_TITLE_MULTIPLE_LINES);

    // setHasOptionsMenu(true);
    Log.d("FileManagerActivity", "onCreate()");
    deviceId = getIntent().getStringExtra("deviceId");

    binding.gotoPathButton.setOnClickListener(
        v -> BackgroundService.RunWithPlugin(FileManagerActivity.this, deviceId, FileManagerPlugin.class, plugin -> {
        updateLastViewedPosition(plugin);
        EditText input = new EditText(FileManagerActivity.this);
        new AlertDialog.Builder(FileManagerActivity.this)
              .setTitle(R.string.fm_goto_path)
              .setView(input)
              .setPositiveButton(R.string.ok, (dialog, which) -> {
                final String inputtedPath = input.getText().toString();
                plugin.requestDirectoryListing(inputtedPath);
                dialog.dismiss();
              })
              .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.cancel())
              .show();

            }));

    // make sure directory listing is recent
    BackgroundService.RunWithPlugin(this, deviceId, FileManagerPlugin.class, FileManagerPlugin::requestDirectoryListing);
    updateView();
  }

  @Override
  public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
    super.onCreateContextMenu(menu, v, menuInfo);

    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.filemanager_context, menu);
    FileEntry selectedItem = directoryItems.get(((AdapterView.AdapterContextMenuInfo) menuInfo).position);
    String selectedPath = selectedItem.getAbsPath();
    boolean isDirectorySelected = selectedItem.isDirectory();
    if (isDirectorySelected) {
      menu.findItem(R.id.directory_download_zip).setVisible(true);
      menu.findItem(R.id.fm_view_as_text).setVisible(false);
    } else {
      menu.findItem(R.id.directory_download_zip).setVisible(false);
    }

    boolean isCached = BackgroundService.getInstance().getDevice(deviceId).getPlugin(FileManagerPlugin.class).isCached(selectedPath);
    if (!isCached) {
      Log.d("FileManagerActivity", selectedPath + " is not cached");
      menu.findItem(R.id.fm_upload).setVisible(false);
    } else { Log.d("FileManagerActivity", selectedPath + " is cached"); }
  }

  @Override
  public boolean onContextItemSelected(MenuItem item) {
    BackgroundService.RunWithPlugin(this, deviceId, FileManagerPlugin.class, this::updateLastViewedPosition);
    AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
    FileEntry selectedItem = directoryItems.get(info.position);

    int id = item.getItemId();

    if (id == R.id.fm_file_info_details_item) {
      Toast toast = Toast.makeText(this, selectedItem.getFullInfo(), Toast.LENGTH_LONG);
      toast.show();
    }

    else if (id == R.id.fm_view_as_text) {
      final String cachePath = String.format("%s/%s", this.getCacheDir().getAbsolutePath(), RandomHelper.randomString(15));
      Intent intent = new Intent(this, TextEditorActivity.class);
      intent.putExtra("deviceId", deviceId);
      intent.putExtra("targetFilePath", selectedItem.getAbsPath());
      intent.putExtra("cacheFilePath", cachePath);
      this.startActivity(intent);
    }

    else if (id == R.id.directory_download_zip) {
      BackgroundService.RunWithPlugin(this, deviceId, FileManagerPlugin.class, plugin ->
              new AlertDialog.Builder(this)
                      .setTitle(R.string.fm_confirm_download)
                      .setMessage(String.format(this.getResources().getString(R.string.fm_confirm_download_desc), selectedItem.getAbsPath() + ".zip"))
                      .setPositiveButton(R.string.ok, (dialog, which) -> {
                        plugin.requestDirectoryDownload(selectedItem.getAbsPath());
                        dialog.dismiss();
                      })
                      .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.cancel())
                      .show());
    }

    else if (id == R.id.fm_delete) {
      BackgroundService.RunWithPlugin(this, deviceId, FileManagerPlugin.class, plugin -> new AlertDialog.Builder(this)
              .setTitle(R.string.fm_confirm_delete)
              .setMessage(String.format(this.getResources().getString(R.string.fm_confirm_delete_desc), selectedItem.getAbsPath()))
              .setPositiveButton(R.string.ok, (dialog, which) -> {
                plugin.requestDelete(selectedItem.getAbsPath());
                dialog.dismiss();
              })
              .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.cancel())
              .show());
    }

    else if (id == R.id.fm_move) {
      final EditText input = new EditText(this);
      BackgroundService.RunWithPlugin(this, deviceId, FileManagerPlugin.class, plugin -> new AlertDialog.Builder(this)
              .setView(input)
              .setTitle(R.string.move)
              .setMessage(String.format(this.getResources().getString(R.string.fm_rename_desc), selectedItem.getAbsPath()))
              .setPositiveButton(R.string.ok, (dialog, which) -> {
                plugin.requestRename(selectedItem.getAbsPath(), input.getText().toString());
                dialog.dismiss();
              })
              .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.cancel())
              .show());
    }

    else if (id == R.id.fm_copy_path_to_clipboard) {
      ClipboardManager cm = ContextCompat.getSystemService(this, ClipboardManager.class);
      cm.setPrimaryClip(new ClipData("kdeconnect_runcommand", new String[]{"text/plain"}, new ClipData.Item(selectedItem.getAbsPath())));
      Toast.makeText(this, "copied " + selectedItem.getAbsPath(), Toast.LENGTH_SHORT).show();
    }

    else if (id == R.id.fm_upload) {
      BackgroundService.RunWithPlugin(this, deviceId, FileManagerPlugin.class, plugin -> new AlertDialog.Builder(this)
              .setTitle(R.string.fm_confirm_upload)
              .setMessage(String.format(this.getResources().getString(R.string.fm_confirm_upload_desc), selectedItem.getAbsPath()))
              .setPositiveButton(R.string.ok, (dialog, which) -> {
                plugin.requestUpload(selectedItem.getAbsPath());
                dialog.dismiss();
              })
              .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.cancel())
              .show());
    }

    else {
      super.onContextItemSelected(item);
    }

    return true;
  }

  @Override
  protected void onResume() {
      super.onResume();
      BackgroundService.RunWithPlugin(this, deviceId, FileManagerPlugin.class, plugin -> plugin.addListingChangedCallback(listingChangedCallback));
  }

  @Override
  protected void onPause() {
      super.onPause();
      BackgroundService.RunWithPlugin(this, deviceId, FileManagerPlugin.class, plugin -> plugin.removeListingChangedCallback(listingChangedCallback));
  }

  @Override
  public void onBackPressed() {
    BackgroundService.RunWithPlugin(FileManagerActivity.this, deviceId, FileManagerPlugin.class, plugin -> {
      updateLastViewedPosition(plugin);
      if (!plugin.requestPreviousOrLeave()) // return false means no more dirs left in stack
        super.onBackPressed();
    });
  }

  @Override
  public boolean onCreateOptionsMenu(@NonNull Menu menu) {
    super.onCreateOptionsMenu(menu);
    getMenuInflater().inflate(R.menu.filemanager_options, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    updateLastViewedPosition(BackgroundService.getInstance().getDevice(deviceId).getPlugin(FileManagerPlugin.class));
    final EditText input = new EditText(this);
    int id = item.getItemId();

    if (id == R.id.fm_refresh) {
      BackgroundService.RunWithPlugin(this, deviceId, FileManagerPlugin.class, FileManagerPlugin::requestDirectoryListing);
    }

    else if (id == R.id.fm_togglehidden) {
      BackgroundService.RunWithPlugin(this, deviceId, FileManagerPlugin.class, FileManagerPlugin::requestToggleHidden);
    }

    else if (id == R.id.fm_create_directory) {
      BackgroundService.RunWithPlugin(this, deviceId, FileManagerPlugin.class, plugin -> new AlertDialog.Builder(this)
              .setView(input)
              .setTitle(R.string.fm_create_directory)
              .setMessage(String.format(this.getResources().getString(R.string.fm_create_directory_desc), currentDirectory))
              .setPositiveButton(R.string.ok, (dialog, which) -> {
                plugin.requestMakeDirectory(input.getText().toString());
                dialog.dismiss();
              })
              .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.cancel())
              .show());
    }

    else if (id == R.id.fm_runcommand) {
      BackgroundService.RunWithPlugin(this, deviceId, FileManagerPlugin.class, plugin -> new AlertDialog.Builder(this)
              .setView(input)
              .setTitle(R.string.fm_runcommand)
              .setMessage(String.format(this.getResources().getString(R.string.fm_runcommand_desc), currentDirectory))
              .setPositiveButton(R.string.ok, (dialog, which) -> {
                final String inputtedCmd = input.getText().toString();
                Log.d("FileManagerPlugin", "running command " + inputtedCmd + " in " + currentDirectory);
                plugin.requestRunCommand(inputtedCmd);
                dialog.dismiss();
              })
              .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.cancel())
              .show());
    }

    else if (id == R.id.details) {
      BackgroundService.RunWithPlugin(this, deviceId, FileManagerPlugin.class,
              plugin -> Toast.makeText(FileManagerActivity.this, plugin.getCWDDetails(), Toast.LENGTH_LONG).show());
    }

    else if (id == R.id.fm_home) {
      BackgroundService.RunWithPlugin(this, deviceId, FileManagerPlugin.class, FileManagerPlugin::requestHomeDirectoryListing);
    }

    else if (id == R.id.fm_wipe_cache) {
      BackgroundService.RunWithPlugin(this, deviceId, FileManagerPlugin.class, FileManagerPlugin::wipeCache);
    }

    else {
      return super.onOptionsItemSelected(item);
    }

    return true;
  }


  private void updateLastViewedPosition(FileManagerPlugin plugin) {
    int pos = binding.directoryListing.getFirstVisiblePosition();
    plugin.setLastViewedPositionForCWD(pos);
    Log.d("FileManagerActivity", "changed lastViewedPosition for " + plugin.getCurrentDirectory() + " to " + pos);
  }

  private void gotoLastPosition(FileManagerPlugin plugin) {
    lastViewedPosition = plugin.getLastViewedPositionForCWD();
    binding.directoryListing.post(() -> {
      binding.directoryListing.smoothScrollToPosition(lastViewedPosition);
      binding.directoryListing.setSelection(lastViewedPosition);
    });

    Log.d("FileManagerPlugin", "scrolled to " + lastViewedPosition);
  }

}

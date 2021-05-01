
package org.kde.kdeconnect.Plugins.FileManagerPlugin;

import android.app.ActionBar;
// import android.content.ClipboardManager;
import android.content.DialogInterface;
// import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.AdapterView;
import android.widget.Toast;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import org.json.JSONException;
import org.json.JSONObject;
import org.kde.kdeconnect.BackgroundService;
import org.kde.kdeconnect.UserInterface.List.ListAdapter;
import org.kde.kdeconnect.UserInterface.ThemeUtil;
import org.kde.kdeconnect_tp.R;
import org.kde.kdeconnect_tp.databinding.ActivityFileManagerBinding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;


public class FileManagerActivity extends AppCompatActivity {
  private ActivityFileManagerBinding binding;
  private List<FileEntry> directoryItems;

  private String deviceId;

  private final FileManagerPlugin.ListingChangedCallback listingChangedCallback = this::updateView;
  private void updateView() {
    BackgroundService.RunWithPlugin(this, deviceId, FileManagerPlugin.class, plugin -> runOnUiThread(() -> {

      // BackgroundService.RunWithPlugin(this, deviceId, FileManagerPlugin.class,
        //    plugin1 -> getSupportActionBar().setTitle(String.format("%s", plugin1.getCurrentDirectory())));

      BackgroundService.RunWithPlugin(this, deviceId, FileManagerPlugin.class,
            plugin1 -> ((TextView) findViewById(R.id.toolbar_title)).setText(
            String.format("%s", plugin1.getCurrentDirectory())));
      registerForContextMenu(binding.directoryListing);

      directoryItems = plugin.getDirectoryItems();
      ListAdapter adapter = new ListAdapter(FileManagerActivity.this, directoryItems);
      binding.directoryListing.setAdapter(adapter);

      binding.directoryListing.setOnItemClickListener((adapterView, view1, i, l) -> {
        FileEntry selectedItem = directoryItems.get(i);

        if (!selectedItem.isReadable()) {
          new AlertDialog.Builder(FileManagerActivity.this)
                  .setTitle(R.string.fm_permission_denied)
                  .setMessage(R.string.fm_permission_denied_desc)
                  .setPositiveButton(R.string.ok, null)
                  .show();
        }

        else if (selectedItem.getFileName().endsWith("/")) {
          plugin.requestDirectoryListing(selectedItem.getAbsPath());
        }

        else {
          new AlertDialog.Builder(FileManagerActivity.this)
            .setTitle(R.string.fm_confirm_download)
            .setMessage(FileManagerActivity.this.getResources().getString(R.string.fm_confirm_download_desc) + "\n" + selectedItem.getAbsPath())
            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialog, int which) {
                plugin.requestDownload(selectedItem.getAbsPath());
                dialog.dismiss();
              }
            })
            .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
              }
            })
            .show();

        }
      });

    binding.gotoPathButton.setOnClickListener(
        v -> BackgroundService.RunWithPlugin(FileManagerActivity.this, deviceId, FileManagerPlugin.class, plugin2 -> {

        EditText input = new EditText(FileManagerActivity.this);
        new AlertDialog.Builder(FileManagerActivity.this)
              .setTitle(R.string.fm_goto_path)
              .setView(input)
              .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                  final String inputtedPath = input.getText().toString();
                  plugin2.requestDirectoryListing(inputtedPath);
                  dialog.dismiss();
                }
              })
              .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                  dialog.cancel();
                }
              })
              .show();

            }));

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

    deviceId = getIntent().getStringExtra("deviceId");

    updateView();
  }

  @Override
  public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
    super.onCreateContextMenu(menu, v, menuInfo);

    boolean isDirectorySelected = directoryItems.get(((AdapterView.AdapterContextMenuInfo) menuInfo).position).getFileName().endsWith("/");

    MenuInflater inflater = getMenuInflater();
    inflater.inflate(isDirectorySelected ? R.menu.filemanager_context_directory : R.menu.filemanager_context_file, menu);
  }

  @Override
  public boolean onContextItemSelected(MenuItem item) {
    AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
    FileEntry selectedItem = directoryItems.get(info.position);

    if (item.getItemId() == R.id.fm_file_info_details_item) {
      Toast toast = Toast.makeText(this, selectedItem.getFullInfo(), Toast.LENGTH_LONG);
      toast.show();
    }

    else if (item.getItemId() == R.id.directory_download_zip) {
      BackgroundService.RunWithPlugin(this, deviceId, FileManagerPlugin.class, plugin -> {
        new AlertDialog.Builder(this)
          .setTitle(R.string.fm_confirm_download)
          .setMessage(this.getResources().getString(R.string.fm_confirm_download_desc) + "\n" + selectedItem.getAbsPath() + ".zip")
          .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
              // plugin.requestDownload(selectedItem.getAbsPath());
              dialog.dismiss();
            }
          })
          .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
              dialog.cancel();
            }
          })
          .show();
        });
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

}

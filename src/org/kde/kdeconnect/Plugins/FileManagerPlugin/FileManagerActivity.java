
package org.kde.kdeconnect.Plugins.FileManagerPlugin;

// import android.content.ClipboardManager;
import android.content.DialogInterface;
// import android.os.Build;
import android.os.Bundle;
import android.util.Log;
// import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
// import android.widget.Toast;
// import android.widget.EditText;

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
      registerForContextMenu(binding.directoryListing);

      // TODO: i want the JSONObjects being sent to be more organized
      // this means parsing them will be slightly more complex
      directoryItems = plugin.getDirectoryItems();
      // directoryItems = new ArrayList<>();
      // for (JSONObject obj : plugin.getDirectoryListing()) {
      //   try {
      //     directoryItems.add(new FileEntry(
      //                               obj.getString("filename"),
      //                               obj.getString("permissions"),
      //                               obj.getString("owner"),
      //                               obj.getString("group"),
      //                               obj.getLong("size"),
      //                               obj.getString("lastmod"),
      //                               obj.getBoolean("readable")
      //                               obj.getString("path")
      //                             ));
      //   }
      //   catch (Exception e) {
      //     Log.e("FileManager", "Error parsing JSON", e);
      //   }
      // }

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
      });

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

    deviceId = getIntent().getStringExtra("deviceId");

    updateView();
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

package org.kde.kdeconnect.Plugins.FileManagerPlugin;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.kde.kdeconnect.NetworkPacket;
import org.kde.kdeconnect.Plugins.Plugin;
import org.kde.kdeconnect.Plugins.PluginFactory;
import org.kde.kdeconnect_tp.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

import androidx.core.content.ContextCompat;

@PluginFactory.LoadablePlugin
public class FileManagerPlugin extends Plugin {

  private final static String PACKET_TYPE_FILEMANAGER = "kdeconnect.filemanager";
  private final static String PACKET_TYPE_FILEMANAGER_REQUEST = "kdeconnect.filemanager.request";

  private final ArrayList<JSONObject> directoryListing = new ArrayList<>();
  private final ArrayList<ListingChangedCallback> callbacks = new ArrayList<>();
  private final ArrayList<FileEntry> directoryItems = new ArrayList<>();

  private final ArrayList<String> errorMessages = new ArrayList<>();
  private static String currentDirectory;

  interface ListingChangedCallback  {
    void update();
  }

  public void addListingChangedCallback(ListingChangedCallback newCallback) {
    callbacks.add(newCallback);
  }

  public void removeListingChangedCallback(ListingChangedCallback theCallback) {
    callbacks.remove(theCallback);
  }

  @Override
  public String getDisplayName() {
      return context.getResources().getString(R.string.pref_plugin_filemanager);
  }

  @Override
  public String getDescription() {
      return context.getResources().getString(R.string.pref_plugin_filemanager_desc);
  }

  @Override
  public Drawable getIcon() {
      return ContextCompat.getDrawable(context, R.drawable.ic_filemanager_plugin_24dp);
  }

  public ArrayList<JSONObject> getDirectoryListing() {
    return directoryListing;
  }

  public ArrayList<FileEntry> getDirectoryItems() {
    return directoryItems;
  }

  public ArrayList<String> getErrorMessages() {
    return new ArrayList<>(errorMessages);
  }

  public void clearErrorMessages() {
    errorMessages.clear();
  }

  public String getCurrentDirectory() {
    return currentDirectory;
  }

  @Override
  public boolean onCreate() {
      requestDirectoryListing();
      return true;
  }

  @Override
  public boolean onPacketReceived(NetworkPacket np) {
    if (np.has("Error")) {
      Log.d("FileManager", "received error packet " + np.getString("Error"));
      errorMessages.add(np.getString("Error"));
    }

    if (np.has("directoryListing")) {

        if (np.has("directoryPath"))
          currentDirectory = np.getString("directoryPath");

        directoryListing.clear();
        try {
          directoryItems.clear();
          JSONObject obj = new JSONObject(np.getString("directoryListing"));
          Iterator<String> paths = obj.keys();

          while (paths.hasNext()) {
            String p = paths.next();
            JSONObject o = obj.getJSONObject(p);
            o.put("path", p);
            directoryListing.add(o);

            try {
              directoryItems.add(new FileEntry(
                                o.getString("filename"),
                                o.getString("permissions"),
                                o.getString("owner"),
                                o.getString("group"),
                                o.getLong("size"),
                                o.getString("lastmod"),
                                o.getBoolean("readable"),
                                o.getString("path")
              ));
            } catch (JSONException e) {
              Log.e("FileManager", "Error parsing JSON", e);
            }
          }

          // Collections.sort(directoryListing, Comparator.comparing(FileEntry::getName));

        } catch (JSONException e) {
          Log.e("FileManager", "Error parsing JSON", e);
        }

        for (ListingChangedCallback callback : callbacks) {
          callback.update();
        }

        device.onPluginsChanged();

        return true;
    }

    return false;
  }

  public void requestDirectoryListing() {
    NetworkPacket np = new NetworkPacket(PACKET_TYPE_FILEMANAGER_REQUEST);
    np.set("requestDirectoryListing", true);
    device.sendPacket(np);
  }

  public void requestDirectoryListing(String path) {
    NetworkPacket np = new NetworkPacket(PACKET_TYPE_FILEMANAGER_REQUEST);
    np.set("requestDirectoryListing", true);
    np.set("directoryPath", path);
    device.sendPacket(np);
  }

  public void requestDownload(String path) {
    NetworkPacket np = new NetworkPacket(PACKET_TYPE_FILEMANAGER_REQUEST);
    np.set("requestFileDownload", true);
    np.set("path", path);
    device.sendPacket(np);
  }

  public void requestDirectoryDownload(String path) {
    NetworkPacket np = new NetworkPacket(PACKET_TYPE_FILEMANAGER_REQUEST);
    np.set("requestDirectoryDownload", true);
    np.set("path", path);
    device.sendPacket(np);
  }

  @Override
  public String[] getSupportedPacketTypes() {
      return new String[]{PACKET_TYPE_FILEMANAGER};
  }

  @Override
  public String[] getOutgoingPacketTypes() {
      return new String[]{PACKET_TYPE_FILEMANAGER_REQUEST};
  }

  @Override
  public boolean hasMainActivity() {
      return true;
  }

  @Override
  public void startMainActivity(Activity parentActivity) {
      Intent intent = new Intent(parentActivity, FileManagerActivity.class);
      intent.putExtra("deviceId", device.getDeviceId());
      parentActivity.startActivity(intent);
  }

  @Override
  public String getActionName() {
      return context.getString(R.string.pref_plugin_filemanager);
  }

}

package org.kde.kdeconnect.Plugins.FileManagerPlugin;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.graphics.drawable.Drawable;
import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;
import org.kde.kdeconnect.NetworkPacket;
import org.kde.kdeconnect.Helpers.RandomHelper;
import org.kde.kdeconnect.Helpers.FilesHelper;
import org.kde.kdeconnect.async.BackgroundJob;
import org.kde.kdeconnect.async.BackgroundJobHandler;
import org.kde.kdeconnect.Plugins.SharePlugin.CompositeReceiveFileJob;
import org.kde.kdeconnect.Plugins.SharePlugin.CompositeUploadFileJob;
import org.kde.kdeconnect.Plugins.Plugin;
import org.kde.kdeconnect.Plugins.PluginFactory;
import org.kde.kdeconnect_tp.R;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.HashMap;
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
  private final ArrayDeque<String> lastVisitedStack = new ArrayDeque<>();
  private final HashMap<String, Integer> lastPositionsMap = new HashMap(); // default capacity 16, probably good enough right?

  // will probably need to keep track of files to maintain the cache;
  // map to an ordered pair with time info or use something that is ordered?
  private final HashMap<String, String> cachedFilesMap = new HashMap();

  private int lastViewedPosition;
  private static String currentDirectory = "";

  private static String lastDownloadedFile;
  private BackgroundJobHandler backgroundJobHandler = BackgroundJobHandler.newFixedThreadPoolBackgroundJobHander(5);
  private final BackgroundJob.Callback<Void> fileReceivedCallback = new BackgroundJob.Callback<Void>() {
    @Override
    public void onResult(@NonNull BackgroundJob job, Void result) {
      Log.d("FileManagerPlugin", "fileReceivedCallback - got " + lastDownloadedFile);
    }

    @Override
    public void onError(@NonNull BackgroundJob job, @NonNull Throwable error) {
      Log.e("FileManagerPlugin", "error receiving file", error);
    }
  };

  private final BackgroundJob.Callback<Void> fileUploadedCallback = new BackgroundJob.Callback<Void>() {
    @Override
    public void onResult(@NonNull BackgroundJob job, Void result) {
      Log.d("FileManagerPlugin", "fileUploadedCallback - success!");
    }

    @Override
    public void onError(@NonNull BackgroundJob job, @NonNull Throwable error) {
      Log.e("FileManagerPlugin", "error uploading file", error);
    }
  };

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

  public int getLastViewedPositionForCWD() {
    return lastPositionsMap.getOrDefault(currentDirectory, 0);
  }

  public void setLastViewedPositionForCWD(int pos) {
    lastPositionsMap.put(currentDirectory, pos);
  }

  public boolean requestPreviousOrLeave() {
    int s = lastVisitedStack.size();
    if (s == 1) {
      return false;
    }

    else if (s >= 2) {
      // this removes the cwd from the stack
      lastVisitedStack.poll();
      // this removes the target directory from the stack
      // when the requested listing is received, the target dir is pushed back on
      requestDirectoryListing(lastVisitedStack.poll());
      return true;
    }

    // this should never be executed since the stack size should never be smaller than 1
    Log.w("FileManagerPlugin", "executing outside if/elseif in requestPreviousOrLeave. likely a bug");
    return false;
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

      for (ListingChangedCallback callback : callbacks) {
        Log.d("FileManagerPlugin", "calling updateView for error packet");
        callback.update();
      }

      // device.onPluginsChanged();
    }

    if (np.has("directoryListing")) {
        if (np.has("directoryPath")) {
          String dirPath = np.getString("directoryPath");
          if (!lastPositionsMap.containsKey(dirPath)) {
            lastPositionsMap.put(dirPath, 0);
          }
          // do not push if dirPath is currentDirectory
          // since unsolicited listings are possible (when deleting/renaming)
          // not doing this can fuck up the stack
          // when the user specifically requests the cwd (through gotoPathButton->".")
          // it is manually put on the stack in requestDirectoryListing()
          if (!currentDirectory.equals(dirPath)) {
            currentDirectory = dirPath;
            Log.d("FileManagerPlugin", "pushing " + currentDirectory + " to the stack");
            lastVisitedStack.push(currentDirectory);
          }
        }

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
          Log.d("FileManagerPlugin", "calling updateView for listing changed");
          callback.update();
        }

        device.onPluginsChanged();

        return true;
    }

    if (np.has("filename")) {
      lastDownloadedFile = np.getString("filename");
      Log.d("FileManagerPlugin", "receiving np for " + lastDownloadedFile);
      CompositeReceiveFileJob job = new CompositeReceiveFileJob(device, fileReceivedCallback);
      job.addNetworkPacket(np);

      backgroundJobHandler.runJob(job);
    }

    return false;
  }

  public void requestDirectoryListing() {
    NetworkPacket np = new NetworkPacket(PACKET_TYPE_FILEMANAGER_REQUEST);
    np.set("requestDirectoryListing", true);
    device.sendPacket(np);
  }

  public void requestHomeDirectoryListing() {
    NetworkPacket np = new NetworkPacket(PACKET_TYPE_FILEMANAGER_REQUEST);
    np.set("requestDirectoryListing", true);
    np.set("home", true);
    device.sendPacket(np);
  }

  public void requestDirectoryListing(final String path) {
    if (path == currentDirectory || path == ".")
      lastVisitedStack.push(currentDirectory);
    NetworkPacket np = new NetworkPacket(PACKET_TYPE_FILEMANAGER_REQUEST);
    np.set("requestDirectoryListing", true);
    np.set("directoryPath", path);
    device.sendPacket(np);
  }

  public void requestDownload(final String path) {
    NetworkPacket np = new NetworkPacket(PACKET_TYPE_FILEMANAGER_REQUEST);
    np.set("requestFileDownload", true);
    np.set("path", path);
    device.sendPacket(np);
  }

  public void requestDirectoryDownload(final String path) {
    NetworkPacket np = new NetworkPacket(PACKET_TYPE_FILEMANAGER_REQUEST);
    np.set("requestDirectoryDownload", true);
    np.set("path", path);
    device.sendPacket(np);
  }

  public void requestDelete(final String path) {
    NetworkPacket np = new NetworkPacket(PACKET_TYPE_FILEMANAGER_REQUEST);
    np.set("requestDelete", true);
    np.set("path", path);
    device.sendPacket(np);
  }

  public void requestRename(final String path, final String newName) {
    NetworkPacket np = new NetworkPacket(PACKET_TYPE_FILEMANAGER_REQUEST);
    np.set("requestRename", true);
    np.set("path", path);
    np.set("newname", newName);
    device.sendPacket(np);
  }

  public void requestMakeDirectory(final String dirName) {
    NetworkPacket np = new NetworkPacket(PACKET_TYPE_FILEMANAGER_REQUEST);
    np.set("requestMakeDirectory", true);
    np.set("dirname", dirName);
    device.sendPacket(np);
  }

  public void requestRunCommand(final String cmd) {
    NetworkPacket np = new NetworkPacket(PACKET_TYPE_FILEMANAGER_REQUEST);
    np.set("requestRunCommand", true);
    np.set("command", cmd);
    np.set("wd", currentDirectory);
    device.sendPacket(np);
  }

  public void requestToggleHidden() {
    NetworkPacket np = new NetworkPacket(PACKET_TYPE_FILEMANAGER_REQUEST);
    np.set("togglehidden", true);
    device.sendPacket(np);
  }

  public void requestDownloadForViewing(final String cachedir, final String targetpath) {
    NetworkPacket np = new NetworkPacket(PACKET_TYPE_FILEMANAGER_REQUEST);
    np.set("requestDownloadForViewing", true);
    np.set("path", targetpath);
    String dest = String.format("%s/%s", cachedir, RandomHelper.randomString(15));
    np.set("dest", dest);
    Log.d("FileManagerPlugin", String.format("adding (%s, %s) to cache", targetpath, dest));
    cachedFilesMap.put(targetpath, dest);
    device.sendPacket(np);
  }

  public void requestUpload(final String path) {
    CompositeUploadFileJob job = new CompositeUploadFileJob(device, fileUploadedCallback);
    Uri uri = new Uri.Builder().scheme("file").path(cachedFilesMap.get(path)).build();
    Log.d("FileManagerPlugin", "built uri for upload with " + uri.toString());
    NetworkPacket np = FilesHelper.uriToNetworkPacket(context, uri, PACKET_TYPE_FILEMANAGER_REQUEST);
    np.set("filename", path);
    job.addNetworkPacket(np);
    backgroundJobHandler.runJob(job);
  }

  public String getCWDDetails() {
    return String.format("Count: %d\nHistory stack size: %d\nPositions cache size: %d\nFiles cache size: %d",
                          directoryItems.size(),
                          lastVisitedStack.size(),
                          lastPositionsMap.size(),
                          cachedFilesMap.size());
  }

  public boolean isCached(final String filename) {
    return cachedFilesMap.containsKey(filename);
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

package org.kde.kdeconnect.Plugins.FileManagerPlugin;

import android.view.LayoutInflater;
import android.view.View;
import androidx.annotation.NonNull;

import org.kde.kdeconnect.UserInterface.List.EntryItem;
import org.kde.kdeconnect_tp.databinding.FmListItemEntryBinding;

public class FileEntry extends EntryItem {
  private final String abspath, permissions, owner, group, lastModified;
  private final long size;
  private final boolean readable;

  private final String fullInfoFormat = "Path: %s\nPermissions: %s\nOwner: %s\nGroup: %s\nSize: %d (%dM)\nLast Modified: %s";
         // Path: %s\ns
         // Permissions: %s\n
         // Owner: %s\n
         // Group: %s\n
         // Size: %d\n
         // Last Modified: %s"

  public FileEntry(String filename, String permissions, String owner, String group, long size, String lastModified, boolean readable, String abspath) {
      super(filename, formatFileInfo(permissions, owner, group, size, lastModified)); // subtitle matches output of ls -l

      this.abspath      = abspath;
      this.permissions  = permissions;
      this.owner        = owner;
      this.size         = size;
      this.group        = group;
      this.lastModified = lastModified;
      this.readable     = readable;
  }

  private static String formatFileInfo(String permissions, String owner, String group, long size, String lastModified) {
    return String.format("%s %s %s   %d   %s", permissions, owner, group, size, lastModified);
  }

  public String getAbsPath() {
      return abspath;
  }

  public String getPermissions() {
      return permissions;
  }

  public String getOwner() {
      return owner;
  }

  public String getGroup() {
      return group;
  }

  public long getSize() {
      return size;
  }

  public String getLastModified() {
      return lastModified;
  }

  public String getFileName() {
      return title;
  }

  public String getFileInfo() {
      return subtitle;
  }

  public String getFullInfo() {
      return String.format(fullInfoFormat, abspath, permissions, owner, group, size, size / 1024 / 1024, lastModified);
  }

  public boolean isReadable() {
    return readable;
  }

  @NonNull
  @Override
  public View inflateView(@NonNull LayoutInflater layoutInflater) {
      final FmListItemEntryBinding binding = FmListItemEntryBinding.inflate(layoutInflater);

      binding.listItemEntryTitle.setText(title);

      if (subtitle != null) {
          binding.listItemEntrySummary.setVisibility(View.VISIBLE);
          binding.listItemEntrySummary.setText(subtitle);
      }

      return binding.getRoot();
  }

}

package org.kde.kdeconnect.Plugins.FileManagerPlugin;

import org.kde.kdeconnect.UserInterface.List.EntryItem;

public class FileEntry extends EntryItem {
  private final String abspath, permissions, owner, group, lastModified;
  private final long size;
  private final boolean readable;


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
    return permissions + " " + owner + " " + group + " " + size + " " + lastModified;
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

  public boolean isReadable() {
    return readable;
  }

}

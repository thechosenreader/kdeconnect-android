package org.kde.kdeconnect.Plugins.FileManagerPlugin;

import org.kde.kdeconnect.UserInterface.List.EntryItem;

public class FileEntry extends EntryItem {
  private final String path;

  public FileEntry(String filename, String fileinfo, String path) {
      super(filename, fileinfo);
      this.path = path;
  }

  public String getPath() {
      return path;
  }

  public String getName() {
      return title;
  }

  public String getCommand() {
      return subtitle;
  }

}

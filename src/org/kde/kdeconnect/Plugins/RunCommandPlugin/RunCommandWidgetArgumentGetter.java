package org.kde.kdeconnect.Plugins.RunCommandPlugin;

import android.os.Bundle;
import android.view.Window;
import androidx.appcompat.app.AppCompatActivity;

public final class RunCommandWidgetArgumentGetter extends AppCompatActivity {
  public static RunCommandPlugin plugin;
  public static String cmdKey, cmd;
  @Override
  public void onCreate(Bundle bundle) {
    super.onCreate(bundle);
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    ArgumentParser.getAndRunWithArgs(this, plugin, cmdKey,cmd, new Runnable(){
      @Override
      public void run(){
        finish();
      }
    });
  }
}

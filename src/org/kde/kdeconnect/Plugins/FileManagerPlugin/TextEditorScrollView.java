package org.kde.kdeconnect.Plugins.FileManagerPlugin;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ScrollView;

import org.kde.kdeconnect_tp.databinding.ActivityTextEditorBinding;

import java.util.ArrayDeque;

public class TextEditorScrollView extends ScrollView {
  private ArrayDeque<String> stringsToAdd;
  private ActivityTextEditorBinding viewBinding;

  public TextEditorScrollView(Context context) {
    super(context);
  }

  public TextEditorScrollView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public TextEditorScrollView(Context context, AttributeSet attrs, int defStyleAttr ) {
    super(context, attrs, defStyleAttr);
  }

  public TextEditorScrollView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
  }

  @Override
  public void computeScroll() {
    addToEditText(1);
    super.computeScroll();
  }

  public void setStrings(ArrayDeque<String> strings) {
    stringsToAdd = strings;
  }

  public ArrayDeque<String> getStrings() {
    return new ArrayDeque<>(stringsToAdd);
  }

  public void setBinding(ActivityTextEditorBinding b) {
    viewBinding = b;
  }

  public void addToEditText(int amount) {
    for (int i = 0; i < amount; i++) {
      if (!(stringsToAdd == null || stringsToAdd.isEmpty())) {
        Log.d("TextEditorScrollView", "appending chunk");
        viewBinding.textView.append(stringsToAdd.pop());
      } else if (stringsToAdd == null) {
        Log.d("TextEditorScrollView", "stringsToAdd is empty!");
      }
    }
  }

  public void addAll() {
    if (stringsToAdd != null) {
      while (!stringsToAdd.isEmpty()) {
        viewBinding.textView.append(stringsToAdd.pop());
      }
    }
  }
}

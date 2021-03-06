package org.thoughtcrime.securesms.wallpaper;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager2.widget.ViewPager2;

import com.annimon.stream.Stream;

import org.thoughtcrime.securesms.PassphraseRequiredActivity;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.recipients.RecipientId;
import org.thoughtcrime.securesms.util.DynamicNoActionBarTheme;
import org.thoughtcrime.securesms.util.DynamicTheme;
import org.thoughtcrime.securesms.util.FullscreenHelper;
import org.thoughtcrime.securesms.util.MappingModel;

import java.util.Collections;

public class ChatWallpaperPreviewActivity extends PassphraseRequiredActivity {

  public  static final String EXTRA_CHAT_WALLPAPER   = "extra.chat.wallpaper";
  private static final String EXTRA_DIM_IN_DARK_MODE = "extra.dim.in.dark.mode";

  private final DynamicTheme dynamicTheme = new DynamicNoActionBarTheme();

  public static @NonNull Intent create(@NonNull Context context, @NonNull ChatWallpaper selection, boolean dimInDarkMode) {
    Intent intent = new Intent(context, ChatWallpaperPreviewActivity.class);

    intent.putExtra(EXTRA_CHAT_WALLPAPER, selection);
    intent.putExtra(EXTRA_DIM_IN_DARK_MODE, dimInDarkMode);

    return intent;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState, boolean ready) {
    dynamicTheme.onCreate(this);

    setContentView(R.layout.chat_wallpaper_preview_activity);

    ViewPager2                       viewPager  = findViewById(R.id.preview_pager);
    ChatWallpaperPreviewAdapter      adapter    = new ChatWallpaperPreviewAdapter();
    View                             submit     = findViewById(R.id.preview_set_wallpaper);
    ChatWallpaperRepository          repository = new ChatWallpaperRepository();
    ChatWallpaper                    selected   = getIntent().getParcelableExtra(EXTRA_CHAT_WALLPAPER);
    boolean                          dim        = getIntent().getBooleanExtra(EXTRA_DIM_IN_DARK_MODE, false);
    Toolbar                          toolbar    = findViewById(R.id.toolbar);

    toolbar.setNavigationOnClickListener(unused -> {
      finish();
    });

    viewPager.setAdapter(adapter);

    adapter.submitList(Collections.singletonList(new ChatWallpaperSelectionMappingModel(selected)));
    repository.getAllWallpaper(wallpapers -> adapter.submitList(Stream.of(wallpapers)
                                                                      .map(wallpaper -> ChatWallpaperFactory.updateWithDimming(wallpaper, dim ? 1f : 0f))
                                                                      .<MappingModel<?>>map(ChatWallpaperSelectionMappingModel::new)
                                                                      .toList()));

    submit.setOnClickListener(unused -> {
      ChatWallpaperSelectionMappingModel model = (ChatWallpaperSelectionMappingModel) adapter.getCurrentList().get(viewPager.getCurrentItem());

      setResult(RESULT_OK, new Intent().putExtra(EXTRA_CHAT_WALLPAPER, model.getWallpaper()));
      finish();
    });

    new FullscreenHelper(this).showSystemUI();
  }

  @Override
  protected void onResume() {
    super.onResume();
    dynamicTheme.onResume(this);
  }
}

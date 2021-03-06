package org.thoughtcrime.securesms.wallpaper;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;

import org.signal.core.util.concurrent.SignalExecutors;
import org.thoughtcrime.securesms.conversation.colors.ChatColors;
import org.thoughtcrime.securesms.conversation.colors.ChatColorsPalette;
import org.thoughtcrime.securesms.database.DatabaseFactory;
import org.thoughtcrime.securesms.dependencies.ApplicationDependencies;
import org.thoughtcrime.securesms.keyvalue.SignalStore;
import org.thoughtcrime.securesms.recipients.Recipient;
import org.thoughtcrime.securesms.recipients.RecipientId;
import org.thoughtcrime.securesms.util.concurrent.SerialExecutor;
import org.whispersystems.libsignal.util.guava.Optional;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;

class ChatWallpaperRepository {

  private static final Executor EXECUTOR = new SerialExecutor(SignalExecutors.BOUNDED);

  @MainThread
  @Nullable ChatWallpaper getCurrentWallpaper(@Nullable RecipientId recipientId) {
    if (recipientId != null) {
      return Recipient.resolved(recipientId).getWallpaper();
    } else {
      return SignalStore.wallpaper().getWallpaper();
    }
  }

  @MainThread
  @NonNull ChatColors getCurrentChatColors(@Nullable RecipientId recipientId) {
    if (recipientId != null) {
      return Recipient.live(recipientId).get().getChatColors();
    } else if (SignalStore.chatColorsValues().hasChatColors()) {
      return Objects.requireNonNull(SignalStore.chatColorsValues().getChatColors());
    } else if (SignalStore.wallpaper().hasWallpaperSet()) {
      return Objects.requireNonNull(SignalStore.wallpaper().getWallpaper()).getAutoChatColors();
    } else {
      return ChatColorsPalette.Bubbles.getDefault().withId(ChatColors.Id.Auto.INSTANCE);
    }
  }

  void getAllWallpaper(@NonNull Consumer<List<ChatWallpaper>> consumer) {
    consumer.accept(ChatWallpaper.BuiltIns.INSTANCE.getAllBuiltIns());
  }

  void saveWallpaper(@Nullable RecipientId recipientId, @Nullable ChatWallpaper chatWallpaper, @NonNull Runnable onWallpaperSaved) {
    if (recipientId != null) {
      //noinspection CodeBlock2Expr
      EXECUTOR.execute(() -> {
        DatabaseFactory.getRecipientDatabase(ApplicationDependencies.getApplication()).setWallpaper(recipientId, chatWallpaper);
        onWallpaperSaved.run();
      });
    } else {
      SignalStore.wallpaper().setWallpaper(ApplicationDependencies.getApplication(), chatWallpaper);
      onWallpaperSaved.run();
    }
  }

  void resetAllWallpaper(@NonNull Runnable onWallpaperReset) {
    SignalStore.wallpaper().setWallpaper(ApplicationDependencies.getApplication(), null);
    EXECUTOR.execute(() -> {
      DatabaseFactory.getRecipientDatabase(ApplicationDependencies.getApplication()).resetAllWallpaper();
      onWallpaperReset.run();
    });
  }

  void resetAllChatColors(@NonNull Runnable onColorsReset) {
    SignalStore.chatColorsValues().setChatColors(null);
    EXECUTOR.execute(() -> {
      DatabaseFactory.getRecipientDatabase(ApplicationDependencies.getApplication()).clearAllColors();
      onColorsReset.run();
    });
  }

  void setDimInDarkTheme(@NonNull RecipientId recipientId, boolean dimInDarkTheme) {
    if (recipientId != null) {
      EXECUTOR.execute(() -> {
        DatabaseFactory.getRecipientDatabase(ApplicationDependencies.getApplication()).setDimWallpaperInDarkTheme(recipientId, dimInDarkTheme);
      });
    } else {
      SignalStore.wallpaper().setDimInDarkTheme(dimInDarkTheme);
    }
  }

  public void clearChatColor(@Nullable RecipientId recipientId, @NonNull Runnable onChatColorCleared) {
    if (recipientId == null) {
      SignalStore.chatColorsValues().setChatColors(null);
      onChatColorCleared.run();
    } else {
      EXECUTOR.execute(() -> {
        DatabaseFactory.getRecipientDatabase(ApplicationDependencies.getApplication()).clearColor(recipientId);
        onChatColorCleared.run();
      });
    }
  }
}

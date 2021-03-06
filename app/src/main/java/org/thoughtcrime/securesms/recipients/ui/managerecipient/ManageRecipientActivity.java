package org.thoughtcrime.securesms.recipients.ui.managerecipient;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityOptionsCompat;

import org.thoughtcrime.securesms.PassphraseRequiredActivity;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.components.settings.conversation.sounds.custom.CustomNotificationsSettingsFragment;
import org.thoughtcrime.securesms.recipients.RecipientId;
import org.thoughtcrime.securesms.util.DynamicNoActionBarTheme;
import org.thoughtcrime.securesms.util.DynamicTheme;

public class ManageRecipientActivity extends PassphraseRequiredActivity {

  private static final String RECIPIENT_ID      = "RECIPIENT_ID";
  private static final String FROM_CONVERSATION = "FROM_CONVERSATION";

  private final DynamicTheme dynamicTheme = new DynamicNoActionBarTheme();
  private ManageRecipientFragment manageRecipientFragment;

  public static Intent newIntent(@NonNull Context context, @NonNull RecipientId recipientId) {
    Intent intent = new Intent(context, ManageRecipientActivity.class);
    intent.putExtra(RECIPIENT_ID, recipientId);
    return intent;
  }

  /**
   * Makes the message button behave like back.
   */
  public static Intent newIntentFromConversation(@NonNull Context context, @NonNull RecipientId recipientId) {
    Intent intent = new Intent(context, ManageRecipientActivity.class);
    intent.putExtra(RECIPIENT_ID, recipientId);
    intent.putExtra(FROM_CONVERSATION, true);
    return intent;
  }

  public static @Nullable Bundle createTransitionBundle(@NonNull Context activityContext, @NonNull View from) {
    if (activityContext instanceof Activity) {
      return ActivityOptionsCompat.makeSceneTransitionAnimation((Activity) activityContext, from, "avatar").toBundle();
    } else {
      return null;
    }
  }

  @Override
  protected void onPreCreate() {
    dynamicTheme.onCreate(this);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState, boolean ready) {
    super.onCreate(savedInstanceState, ready);
    getWindow().getDecorView().setSystemUiVisibility(getWindow().getDecorView().getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    setContentView(R.layout.recipient_manage_activity);
    if (savedInstanceState == null) {
      manageRecipientFragment = ManageRecipientFragment.newInstance(getIntent().getParcelableExtra(RECIPIENT_ID), getIntent().getBooleanExtra(FROM_CONVERSATION, false));
      getSupportFragmentManager().beginTransaction()
                                 .replace(R.id.container, manageRecipientFragment)
                                 .commitNow();
    }
  }
  CustomNotificationsSettingsFragment fragment;
  public void replaceFragment(RecipientId recipientId){
    fragment = new CustomNotificationsSettingsFragment();
    final Bundle                              bundle   = new Bundle();
    bundle.putParcelable("recipientId",recipientId);
    fragment.setArguments(bundle);
    getSupportFragmentManager().beginTransaction()
                               .hide(manageRecipientFragment)
                               .add(R.id.container, fragment)
                               .commitNow();
  }

  @Override public void onBackPressed() {
    if (fragment != null) {
      getSupportFragmentManager().beginTransaction().show(manageRecipientFragment).remove(fragment).commitNow();
      fragment=null;
    }else {
      super.onBackPressed();
    }

  }
  @Override
  public void onResume() {
    super.onResume();
    dynamicTheme.onResume(this);
  }
}

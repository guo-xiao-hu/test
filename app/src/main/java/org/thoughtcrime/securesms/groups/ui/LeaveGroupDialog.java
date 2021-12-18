package org.thoughtcrime.securesms.groups.ui;

import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;

import com.annimon.stream.Stream;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.signal.core.util.logging.Log;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.components.Mp02CustomDialog;
import org.thoughtcrime.securesms.database.DatabaseFactory;
import org.thoughtcrime.securesms.database.GroupDatabase;
import org.thoughtcrime.securesms.groups.GroupChangeException;
import org.thoughtcrime.securesms.groups.GroupId;
import org.thoughtcrime.securesms.groups.GroupManager;
import org.thoughtcrime.securesms.groups.ui.chooseadmin.ChooseNewAdminActivity;
import org.thoughtcrime.securesms.permissions.RationaleDialog;
import org.thoughtcrime.securesms.recipients.Recipient;
import org.thoughtcrime.securesms.util.concurrent.SimpleTask;
import org.thoughtcrime.securesms.util.views.SimpleProgressDialog;

import java.io.IOException;
import java.util.List;

public final class LeaveGroupDialog {

  private static final String TAG = Log.tag(LeaveGroupDialog.class);

  @NonNull  private final FragmentActivity activity;
  @NonNull  private final GroupId.Push     groupId;
  @Nullable private final Runnable         onSuccess;

  public static void handleLeavePushGroup(@NonNull FragmentActivity activity,
                                          @NonNull GroupId.Push groupId,
                                          @Nullable Runnable onSuccess) {
    new LeaveGroupDialog(activity, groupId, onSuccess).show();
  }

  private LeaveGroupDialog(@NonNull FragmentActivity activity,
                           @NonNull GroupId.Push groupId,
                           @Nullable Runnable onSuccess) {
    this.activity  = activity;
    this.groupId   = groupId;
    this.onSuccess = onSuccess;
  }

  public void show() {
    if (!groupId.isV2()) {
      showLeaveDialog();
      return;
    }

    SimpleTask.run(activity.getLifecycle(), () -> {
      GroupDatabase.V2GroupProperties groupProperties = DatabaseFactory.getGroupDatabase(activity)
                                                                       .getGroup(groupId)
                                                                       .transform(GroupDatabase.GroupRecord::requireV2GroupProperties)
                                                                       .orNull();

      if (groupProperties != null && groupProperties.isAdmin(Recipient.self())) {
        List<Recipient> otherMemberRecipients = groupProperties.getMemberRecipients(GroupDatabase.MemberSet.FULL_MEMBERS_EXCLUDING_SELF);
        long            otherAdminsCount      = Stream.of(otherMemberRecipients).filter(groupProperties::isAdmin).count();

        return otherAdminsCount == 0 && !otherMemberRecipients.isEmpty();
      }

      return false;
    }, mustSelectNewAdmin -> {
      if (mustSelectNewAdmin) {
        showSelectNewAdminDialog();
      } else {
        showLeaveDialog();
      }
    });
  }

  private void showSelectNewAdminDialog() {
    Mp02CustomDialog dialog = new Mp02CustomDialog(activity);
    dialog.setMessage(activity.getString(R.string.LeaveGroupDialog_before_you_leave_you_must_choose_at_least_one_new_admin_for_this_group));
    dialog.setNegativeListener(android.R.string.cancel, new Mp02CustomDialog.Mp02DialogKeyListener() {
      @Override
      public void onDialogKeyClicked() {
        dialog.dismiss();
      }
    });
    dialog.setPositiveListener(R.string.LeaveGroupDialog_choose_admin, new Mp02CustomDialog.Mp02DialogKeyListener() {
      @Override
      public void onDialogKeyClicked() {
        activity.startActivity(ChooseNewAdminActivity.createIntent(activity, groupId.requireV2()));
        dialog.dismiss();
      }
    });
    dialog.show();
  }

  private void showLeaveDialog() {
    android.app.AlertDialog builder = RationaleDialog.createNonMsgDialog(activity,
            activity.getString(R.string.LeaveGroupDialog_you_will_no_longer_be_able_to_send_or_receive_messages_in_this_group),
            R.string.yes,
            R.string.no,
            () -> {
              AlertDialog progressDialog = SimpleProgressDialog.show(activity);
              SimpleTask.run(activity.getLifecycle(), this::leaveGroup, result -> {
                progressDialog.dismiss();
                handleLeaveGroupResult(result);
              });},
            null,
            null
    );
    builder.setCancelable(true);
    builder.show();
  }

  private @NonNull GroupChangeResult leaveGroup() {
    try {
      GroupManager.leaveGroup(activity, groupId);
      return GroupChangeResult.SUCCESS;
    } catch (GroupChangeException | IOException e) {
      Log.w(TAG, e);
      return GroupChangeResult.failure(GroupChangeFailureReason.fromException(e));
    }
  }

  private void handleLeaveGroupResult(@NonNull GroupChangeResult result) {
    if (result.isSuccess()) {
      if (onSuccess != null) onSuccess.run();
    } else {
      Toast.makeText(activity, GroupErrors.getUserDisplayMessage(result.getFailureReason()), Toast.LENGTH_LONG).show();
    }
  }
}
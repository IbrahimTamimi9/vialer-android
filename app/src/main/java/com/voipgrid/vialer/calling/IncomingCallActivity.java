package com.voipgrid.vialer.calling;

import static com.voipgrid.vialer.calling.CallingConstants.CONTACT_NAME;
import static com.voipgrid.vialer.calling.CallingConstants.PHONE_NUMBER;
import static com.voipgrid.vialer.calling.CallingConstants.TAG_CALL_INCOMING_FRAGMENT;
import static com.voipgrid.vialer.calling.CallingConstants.TAG_CALL_LOCK_RING_FRAGMENT;
import static com.voipgrid.vialer.calling.CallingConstants.TYPE_INCOMING_CALL;
import static com.voipgrid.vialer.calling.CallingConstants.TYPE_NOTIFICATION_ACCEPT_INCOMING_CALL;
import static com.voipgrid.vialer.media.BluetoothMediaButtonReceiver.DECLINE_BTN;
import static com.voipgrid.vialer.sip.SipConstants.CALL_CONNECTED_MESSAGE;
import static com.voipgrid.vialer.sip.SipConstants.CALL_DISCONNECTED_MESSAGE;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.voipgrid.vialer.CallActivity;
import com.voipgrid.vialer.R;
import com.voipgrid.vialer.VialerApplication;
import com.voipgrid.vialer.sip.SipService;
import com.voipgrid.vialer.util.LoginRequiredActivity;
import com.wearespindle.spindlelockring.library.LockRing;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.hdodenhof.circleimageview.CircleImageView;

public class IncomingCallActivity extends AbstractCallActivity {

    @Inject KeyguardManager mKeyguardManager;

    @BindView(R.id.incoming_caller_title) TextView mIncomingCallerTitle;
    @BindView(R.id.incoming_caller_subtitle) TextView mIncomingCallerSubtitle;
    @BindView(R.id.profile_image) CircleImageView mProfileImage;
    @BindView(R.id.button_decline) ImageButton mButtonDecline;
    @BindView(R.id.button_pickup) ImageButton mButtonPickup;
    @BindView(R.id.lock_ring_container) View mLockRingContainer;
    @BindView(R.id.lock_ring) LockRing mLockRing;
    @BindView(R.id.call_buttons) View mCallButtons;

    private boolean ringingIsPaused = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_incoming_call);
        ButterKnife.bind(this);
        VialerApplication.get().component().inject(this);

        mMediaManager.startIncomingCallRinger();
        updateViewBasedOnIntent();
    }

    private boolean currentlyOnLockScreen() {
        return mKeyguardManager.inKeyguardRestrictedInputMode();
    }

    /**
     * Update the labels on the view based on the information from the
     * intent.
     */
    private void updateViewBasedOnIntent() {
        String contactName = null; // TODO this should use the method to lookup the name from the contacts
        String callerId = getCallerIdFromIntent();
        String number = getPhoneNumberFromIntent();

        if (contactName == null) {
            contactName = callerId;
        }

        if (contactName != null) {
            mIncomingCallerTitle.setText(contactName);
            mIncomingCallerSubtitle.setText(number);
            return;
        }

        mIncomingCallerTitle.setText(number);
        mIncomingCallerSubtitle.setText("");
    }

    @OnClick(R.id.button_decline)
    public void onDeclineButtonClicked() {
        mLogger.d("decline");

        disableAllButtons();

        if (!mSipServiceConnection.isAvailable()) {
            return;
        }

        if (mSipServiceConnection.get().getCurrentCall() == null) {
            return;
        }

        try {
            mSipServiceConnection.get().getCurrentCall().decline();
        } catch (Exception e) {
            e.printStackTrace();
        }

        mCallNotifications.removeAll();
        sendBroadcast(new Intent(DECLINE_BTN));
        endRinging();
    }

    @OnClick(R.id.button_pickup)
    public void onPickupButtonClicked() {
        if (!mSipServiceConnection.isAvailable() || mSipServiceConnection.get().getCurrentCall() == null) {
            finish();
            return;
        }

        disableAllButtons();

        try {
            mSipServiceConnection.get().getCurrentCall().answer();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void disableAllButtons() {
        mButtonPickup.setEnabled(false);
        mButtonDecline.setEnabled(false);
    }

    @Override
    public void onCallStatusReceived(String status, String callId) {
        super.onCallStatusReceived(status, callId);

        if (status.equals(CALL_DISCONNECTED_MESSAGE)) {
            mSipServiceConnection.disconnect(true);
            endRinging();
            return;
        }

        if (status.equals(CALL_CONNECTED_MESSAGE)) {
            mSipServiceConnection.disconnect(true);
            mMediaManager.stopIncomingCallRinger();
            startCallActivity();
            return;
        }
    }

    /**
     * Start the call activity.
     *
     */
    private void startCallActivity() {
        Intent intent = getIntent();
        intent.setClass(this, CallActivity.class);
        startActivity(intent);
        mLogger.d("callVisibleForUser");
    }

    /**
     * Ends the incoming calling.
     *
     */
    private void endRinging() {
        mMediaManager.stopIncomingCallRinger();
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (!allPermissionsGranted(permissions, grantResults)) {
            return;
        }

        mMediaManager.startIncomingCallRinger();
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Check if the screen is interactive because when the activity becomes active.
        // After the screen turns on onStart and onPause are called again.
        // Hence : onCreate - onStart - onResume - onPause - onStop - onStart - onPause.
        if (!isScreenInteractive()) {
            mLogger.i("We come from an screen that has been off. Don't execute the onPause!");
            return;
        }

        if (mSipServiceConnection.isAvailableAndHasActiveCall()) {
            mCallNotifications.callScreenIsBeingHiddenOnRingingCall(getCallNotificationDetails());
        }
        mMediaManager.stopIncomingCallRinger();
        ringingIsPaused = true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        mCallNotifications.removeAll();

        if (currentlyOnLockScreen()) {
            mLockRing.setOnTriggerListener(new LockRingListener(mLockRing, this));
            mLockRingContainer.setVisibility(View.VISIBLE);
            mCallButtons.setVisibility(View.GONE);
        } else {
            mLockRingContainer.setVisibility(View.GONE);
            mCallButtons.setVisibility(View.VISIBLE);
        }

        if (ringingIsPaused) {
            mMediaManager.startIncomingCallRinger();
            ringingIsPaused = false;
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (userPressedAcceptFromNotification(intent)) {
            new Handler().postDelayed(this::onPickupButtonClicked, 500);
            mCallNotifications.acceptedFromNotification(getCallNotificationDetails());
            return;
        }
    }

    /**
     * Check if the received intent is from the user pressing the accept button on the notification.
     *
     * @param intent
     * @return
     */
    private boolean userPressedAcceptFromNotification(Intent intent) {
        return TYPE_NOTIFICATION_ACCEPT_INCOMING_CALL.equals(intent.getType());
    }
}
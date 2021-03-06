package com.voipgrid.vialer.call;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.voipgrid.vialer.R;
import com.voipgrid.vialer.dialer.DialpadButton;
import com.voipgrid.vialer.dialer.KeyPadView;
import com.voipgrid.vialer.dialer.NumberInputView;
import com.voipgrid.vialer.dialer.ToneGenerator;

/**
 * Fragment for the keypad when calling.
 */
public class CallKeyPadFragment extends Fragment implements KeyPadView.OnKeyPadClickListener, View.OnClickListener {
    private NumberInputView mNumberInputView;
    private CallKeyPadFragmentListener mCallback;
    private View mHangupButton;
    private ToneGenerator mToneGenerator;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_call_key_pad, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ViewGroup keyPadViewContainer = (ViewGroup) view.findViewById(R.id.fragment_call_key_pad);
        KeyPadView keyPadView = (KeyPadView) keyPadViewContainer.findViewById(R.id.key_pad_view);
        keyPadView.setOnKeyPadClickListener(this);

        mHangupButton = view.findViewById(R.id.button_keypad_call_hangup);
        mHangupButton.setOnClickListener(this);

        mNumberInputView = (NumberInputView) view.findViewById(R.id.number_input_edit_text);
        mNumberInputView.setOnInputChangedListener(new NumberInputView.OnInputChangedListener() {
            @Override
            public void onInputChanged(String number) {
                // This is needed to get the remove button to show up.
            }
        });

        AudioManager audioManager = (AudioManager) getActivity().getSystemService(
                Context.AUDIO_SERVICE
        );
        mToneGenerator = new ToneGenerator(
                AudioManager.STREAM_DTMF,
                (int) (Math.floor(audioManager.getStreamVolume(AudioManager.STREAM_DTMF) * 5))
        );
    }

    /**
     * Use the deprecated version of the onAttach method for api levels < 23
     * The old android 4.1.2 API level 16 calls this method.
     * If don't use this function no callback will be set.
     *
     * @param activity Activity the attached activity.
     */
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mCallback = (CallKeyPadFragmentListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement CallKeyPadFragmentListener");
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mNumberInputView.clear();
    }

    @Override
    public void onKeyPadButtonClick(String digit, String chars) {
        String currentDTMF = mNumberInputView.getNumber();
        mNumberInputView.setNumber(currentDTMF + digit);
        if (mCallback != null) {
            mCallback.callKeyPadButtonClicked(digit);
        }
    }

    @Override
    public void onClick(View view) {
        int viewId = view.getId();
        if (view instanceof DialpadButton) {
            DialpadButton button = (DialpadButton) view;
            mToneGenerator.startTone(button.getDtmfTone(), KeyPadView.DTMF_TONE_DURATION);
        }
        switch (viewId) {
            case R.id.button_keypad_call_hangup:
                if (mCallback != null) {
                    mCallback.hangupFromKeypad();
                }
        }
    }

    public interface CallKeyPadFragmentListener {
        void callKeyPadButtonClicked(String dtmf);
        void hangupFromKeypad();
    }
}

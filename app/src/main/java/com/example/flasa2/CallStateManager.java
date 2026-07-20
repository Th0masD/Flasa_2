package com.example.flasa2;

import android.content.Context;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

public class CallStateManager {

    public interface CallListener {
        void onRinging();
        void onAnswered();
        void onEnded(CallResult result);
    }

    public enum CallResult {
        MISSED,
        ANSWERED,
        BUSY_OR_REJECTED
    }

    private boolean isRinging = false;
    private boolean isAnswered = false;

    public void start(Context context, CallListener listener) {

        TelephonyManager telephonyManager =
                (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

        telephonyManager.listen(new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String phoneNumber) {

                switch (state) {

                    case TelephonyManager.CALL_STATE_RINGING:
                        isRinging = true;
                        listener.onRinging();
                        break;

                    case TelephonyManager.CALL_STATE_OFFHOOK:
                        isAnswered = true;
                        listener.onAnswered();
                        break;

                    case TelephonyManager.CALL_STATE_IDLE:

                        if (isAnswered) {
                            listener.onEnded(CallResult.ANSWERED);
                        }
                        else if (isRinging) {
                            listener.onEnded(CallResult.MISSED);
                        }
                        else {
                            listener.onEnded(CallResult.BUSY_OR_REJECTED);
                        }

                        isRinging = false;
                        isAnswered = false;
                        break;
                }
            }
        }, PhoneStateListener.LISTEN_CALL_STATE);
    }

    public void stop(Context context) {
        TelephonyManager telephonyManager =
                (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

        telephonyManager.listen(
                null,
                PhoneStateListener.LISTEN_NONE
        );
    }


}

package tech.bogomolov.incomingsmsgateway.sms;

import static tech.bogomolov.incomingsmsgateway.data.ApiServiceKt.callWebHook;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;

import androidx.lifecycle.Observer;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import tech.bogomolov.incomingsmsgateway.R;

public class SmsBroadcastReceiver extends BroadcastReceiver {
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    public void someMethod(String msg) {
        // Running a task on a background thread
        new Thread(new Runnable() {
            @Override
            public void run() {
                // Background thread work

                // Switch to UI thread
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context,msg,Toast.LENGTH_SHORT).show();

                        // UI thread work
                    }
                });
            }
        }).start();
    }
    private Context context;

    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context;

        Bundle bundle = intent.getExtras();
        if (bundle == null) {
            return;
        }

        Object[] pdus = (Object[]) bundle.get("pdus");
        if (pdus == null || pdus.length == 0) {
            return;
        }

        StringBuilder content = new StringBuilder();
        final SmsMessage[] messages = new SmsMessage[pdus.length];
        for (int i = 0; i < pdus.length; i++) {
            messages[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
            content.append(messages[i].getDisplayMessageBody());
            someMethod(messages[i].getDisplayMessageBody());
            Log.e("TTT",messages[i].getDisplayMessageBody());
        }


        ArrayList<ForwardingConfig> configs = ForwardingConfig.getAll(context);
        String asterisk = context.getString(R.string.asterisk);

        String sender = messages[0].getOriginatingAddress();
        if (sender == null) {
            return;
        }

        for (ForwardingConfig config : configs) {
            if (!sender.equals(config.getSender()) && !config.getSender().equals(asterisk)) {
                continue;
            }

//            if (!config.getIsSmsEnabled()) {
//                continue;
//            }

            int slotId = this.detectSim(bundle) + 1;
            String slotName = "undetected";
            if (slotId < 0) {
                slotId = 0;
            }

            if (config.getSimSlot() > 0 && config.getSimSlot() != slotId) {
                continue;
            }

            if (slotId > 0) {
                slotName = "sim" + slotId;
            }

            callWebHook(config,
                    sender,
                    content.toString(),
                    messages[0].getTimestampMillis(),
                    context
            );
        }
    }

    protected void callWebHook1(ForwardingConfig config, String sender, String slotName,
                               String content, long timeStamp) {

        String message = config.prepareMessage(
                sender,
                content,
                timeStamp
        );

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        Data data = new Data.Builder()
                .putString(RequestWorker.DATA_URL, config.getUrl())
                .putString(RequestWorker.DATA_TEXT, message)
                .putString(RequestWorker.DATA_HEADERS, config.getHeaders())
                .putBoolean(RequestWorker.DATA_IGNORE_SSL, config.getIgnoreSsl())
                .putBoolean(RequestWorker.DATA_CHUNKED_MODE, config.getChunkedMode())
                .putInt(RequestWorker.DATA_MAX_RETRIES, config.getRetriesNumber())
                .build();

        WorkRequest workRequest =
                new OneTimeWorkRequest.Builder(RequestWorker.class)
                        .setConstraints(constraints)
                        .setBackoffCriteria(
                                BackoffPolicy.EXPONENTIAL,
                                OneTimeWorkRequest.MIN_BACKOFF_MILLIS,
                                TimeUnit.MILLISECONDS
                        )
                        .setInputData(data)
                        .build();
        Log.d("TTT_REQUEST_SMS_URL", "Request URL: " + config.getUrl());
        Log.d("TTT_REQUEST_SMS_MESSAGE", "Request Body: " + message);
        Log.e("TTT_REQUEST_SMS",workRequest.toString());
         WorkManager
                .getInstance(this.context)
                .enqueue(workRequest);
                // Observe the WorkInfo for the WorkRequest
//        WorkManager.getInstance(this.context)
//                .getWorkInfoByIdLiveData(workRequest.getId())
//                .observeForever( new Observer<WorkInfo>() {
//                    @Override
//                    public void onChanged(WorkInfo workInfo) {
//                        if (workInfo != null && workInfo.getState().isFinished()) {
//                            // Get the result data
//                            Data outputData = workInfo.getOutputData();
//
//                            String responseBody = outputData.getString("response_body");
//                            Log.d("TTT_RequestWorker", "Response Body: " + responseBody);
//
//                            // Handle the result here
//                        }
//                    }
//                });

    }

    private int detectSim(Bundle bundle) {
        int slotId = -1;
        Set<String> keySet = bundle.keySet();
        for (String key : keySet) {
            switch (key) {
                case "phone":
                    slotId = bundle.getInt("phone", -1);
                    break;
                case "slot":
                    slotId = bundle.getInt("slot", -1);
                    break;
                case "simId":
                    slotId = bundle.getInt("simId", -1);
                    break;
                case "simSlot":
                    slotId = bundle.getInt("simSlot", -1);
                    break;
                case "slot_id":
                    slotId = bundle.getInt("slot_id", -1);
                    break;
                case "simnum":
                    slotId = bundle.getInt("simnum", -1);
                    break;
                case "slotId":
                    slotId = bundle.getInt("slotId", -1);
                    break;
                case "slotIdx":
                    slotId = bundle.getInt("slotIdx", -1);
                    break;
                case "android.telephony.extra.SLOT_INDEX":
                    slotId = bundle.getInt("android.telephony.extra.SLOT_INDEX", -1);
                    break;
                default:
                    if (key.toLowerCase().contains("slot") | key.toLowerCase().contains("sim")) {
                        String value = bundle.getString(key, "-1");
                        if (value.equals("0") | value.equals("1") | value.equals("2")) {
                            slotId = bundle.getInt(key, -1);
                        }
                    }
            }

            if (slotId != -1) {
                break;
            }
        }

        return slotId;
    }
}

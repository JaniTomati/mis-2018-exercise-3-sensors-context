package com.example.mis.sensor;

import android.app.IntentService;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

import java.util.List;

/**
 * https://code.tutsplus.com/tutorials/how-to-recognize-user-activity-with-activity-recognition--cms-25851
 */
public class ActivityIntentService extends IntentService {

    private DetectedActivity mActivity;

    public ActivityIntentService() {
        super("ActivityIntentService");
    }

    public ActivityIntentService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
/*        if(ActivityRecognitionResult.hasResult(intent)) {
            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
            handleDetectedActivities( result.getProbableActivities() );

        }
*/
        Log.i("Activity Recognition", Boolean.toString(ActivityRecognitionResult.hasResult(intent)));
        if(ActivityRecognitionResult.hasResult(intent)) {
            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
            Log.i("User Activity", result.getMostProbableActivity().toString());
            mActivity = result.getMostProbableActivity();
        }
    }

    public DetectedActivity getActivity() {
        return mActivity;
    }
}

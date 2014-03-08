/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.mines.letschat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.internal.ex;
import com.orm.dsl.Collection;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.SystemClock;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

/**
 * This {@code IntentService} does the actual handling of the GCM message.
 * {@code GcmBroadcastReceiver} (a {@code WakefulBroadcastReceiver}) holds a
 * partial wake lock for this service while the service does its work. When the
 * service is finished, it calls {@code completeWakefulIntent()} to release the
 * wake lock.
 */
public class GcmIntentService extends IntentService {
    public static int NOTIFICATION_ID;
    private NotificationManager mNotificationManager;
    public static ArrayList<String> messages = new ArrayList<String>();
    NotificationCompat.Builder builder;

    public GcmIntentService() {
        super("GcmIntentService");
    }
    public static final String TAG = "GCM Demo";
    public static final String OUTPUT_TEXT = "bababa";
    public static int notificationCounter = 1;

    @Override
    protected void onHandleIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
        // The getMessageType() intent parameter must be the intent you received
        // in your BroadcastReceiver.
        String messageType = gcm.getMessageType(intent);

        if (!extras.isEmpty()) {  // has effect of unparcelling Bundle
            /*
             * Filter messages based on message type. Since it is likely that GCM will be
             * extended in the future with new message types, just ignore any message types you're
             * not interested in, or that you don't recognize.
             */
            if (GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR.equals(messageType)) {
                sendNotification("Send error: " + extras.toString());
            } else if (GoogleCloudMessaging.MESSAGE_TYPE_DELETED.equals(messageType)) {
                sendNotification("Deleted messages on server: " + extras.toString());
            // If it's a regular GCM message, do some work.
            } else if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {
                // This loop represents the service doing some work.
//                for (int i = 0; i < 5; i++) {
//                    Log.i(TAG, "Working... " + (i + 1)
//                            + "/5 @ " + SystemClock.elapsedRealtime());
//                    try {
//                        Thread.sleep(5000);
//                    } catch (InterruptedException e) {
//                    }
//                }
                Log.i(TAG, "Completed work @ " + SystemClock.elapsedRealtime());
                // Post notification of received message.
//                sendNotification("Received: " + extras.toString());
                
                String message = (String) extras.get("message");
                messages.add(message);
                
                sendNotification(message);
                String senderID = (String) extras.get("sender");
                String recipientID = (String) extras.get("recipient");
                Log.v(TAG, "Received: " + message);
                
                Log.i(TAG, "Received: " + extras.toString());
                Intent resultBroadCastIntent =new Intent();
        		/*set action here*/
        		resultBroadCastIntent.setAction(edu.mines.letschat.MessageActivity.TextCapitalizeResultReceiver.ACTION_TEXT_CAPITALIZED);
        		/*set intent category as default*/
        		resultBroadCastIntent.addCategory(Intent.CATEGORY_DEFAULT);
         
        		/*add data to intent*/
        		resultBroadCastIntent.putExtra(OUTPUT_TEXT, message);
        		Log.v(TAG, "Sender id " + senderID + " recieve id " + recipientID);
        		Conversation convo = new Conversation(getApplicationContext(), senderID, recipientID, message, false);
    			convo.save();
        		/*send broadcast */
        		sendBroadcast(resultBroadCastIntent);
            }
        }
        // Release the wake lock provided by the WakefulBroadcastReceiver.
        GcmBroadcastReviever.completeWakefulIntent(intent);
    }
    
    protected PendingIntent getDeleteIntent()
    {
    	Intent resultBroadCastIntent = new Intent();
    	resultBroadCastIntent.setAction("deletion");
    	resultBroadCastIntent.addCategory(Intent.CATEGORY_DEFAULT);
    	sendBroadcast(resultBroadCastIntent);
    	return PendingIntent.getBroadcast(getBaseContext(), 0, resultBroadCastIntent, PendingIntent.FLAG_CANCEL_CURRENT);
   }

    // Put the message into a notification and post it.
    // This is just one simple example of what you might choose to do with
    // a GCM message.
    private void sendNotification(String msg) {
        mNotificationManager = (NotificationManager)
                this.getSystemService(Context.NOTIFICATION_SERVICE);

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), 0);

        long[] pattern = {1000};
        
        notificationCounter++;
        
        if (messages.size() > 1) {
        	msg = msg + "...";
        }
        
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
        .setVibrate(new long[] { 0, 500, 250, 500, 250 })
        .setLights(Color.RED, 200, 200)
        .setSmallIcon(R.drawable.ic_launcher)
        .setContentTitle("Let's Chat Notification")
        .setStyle(new NotificationCompat.BigTextStyle()
        .bigText(msg))
        .setNumber(notificationCounter)
        .setContentText(msg);
        
        NotificationCompat.InboxStyle inboxStyle =
                new NotificationCompat.InboxStyle();
        // Sets a title for the Inbox style big view
        inboxStyle.setBigContentTitle("Unread messages (" + notificationCounter + "):");
        // Moves events into the big view
//        Collections.reverse(messages);
        int start = messages.size() - 1;
        ArrayList<String> temp = new ArrayList<String>();
        for (int i = 0; i < 5; ++i) {
        	if (i == messages.size()) {
        		break;
        	}
        	temp.add(messages.get(i));
        }
        Collections.reverse(temp);
        messages = temp;
        for (String s: messages) {
            inboxStyle.addLine(s);
        }
        
        mBuilder.setStyle(inboxStyle);
        
        mBuilder.setAutoCancel(true);
        mBuilder.setContentIntent(contentIntent);
//        NOTIFICATION_ID++;
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }
}

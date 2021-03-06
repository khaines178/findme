/*
 *      Copyright (C) 2015 Kevin Haines
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *      You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *      See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */

package com.swiftkaydevelopment.findme.data;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;

public class PushData {
    private static final String TAG = "PushData";

    public String message;
    public String title;
    public int resId;
    public int notificationId;
    public int notificationTypeCount;
    public PendingIntent intent;
    public Bitmap icon = null;

    /**
     * Creates a pending intent from an intent
     *
     * @param intent Intent to create pending intent from
     * @param context context
     * @return new pending intent
     */
    public static PendingIntent createPendingIntent(Intent intent, Context context) {
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_ONE_SHOT);
    }

}

package co.wompwomp.sunshine;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
public class SetupNotifierAlarmBootReceiver extends BroadcastReceiver {
    public SetupNotifierAlarmBootReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
            Intent pushNotificationIntent = new Intent(context, NotifierService.class);
            pushNotificationIntent.setAction(WompWompConstants.INIT_NOTIFICATION_ALARM);
            context.startService(pushNotificationIntent);
        }
    }
}

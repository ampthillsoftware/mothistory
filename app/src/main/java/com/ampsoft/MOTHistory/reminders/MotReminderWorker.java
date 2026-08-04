package com.ampsoft.MOTHistory.reminders;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.ampsoft.MOTHistory.R;
import com.ampsoft.MOTHistory.data.local.ReminderStore;
import com.ampsoft.MOTHistory.data.local.VehicleStore;
import com.ampsoft.MOTHistory.data.model.Vehicle;
import com.ampsoft.MOTHistory.ui.MainActivity;
import com.ampsoft.MOTHistory.util.DateFormatter;

public class MotReminderWorker extends Worker {

    public MotReminderWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        String registration = getInputData().getString(MotReminderScheduler.KEY_REGISTRATION);
        if (registration == null || registration.trim().isEmpty()) {
            return Result.success();
        }

        ReminderStore.ReminderConfig config =
                ReminderStore.getReminder(getApplicationContext(), registration);
        Vehicle vehicle = VehicleStore.getSavedVehicleByRegistration(getApplicationContext(), registration);
        if (config == null || vehicle == null) {
            MotReminderScheduler.cancelReminder(getApplicationContext(), registration);
            return Result.success();
        }

        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_OPEN_RESULT_REGISTRATION, registration);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                getApplicationContext(),
                registration.toUpperCase().hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String expiryDate = DateFormatter.asDisplayDate(config.getExpiryDateIso());
        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                getApplicationContext(),
                MotReminderScheduler.getChannelId()
        )
                .setSmallIcon(R.drawable.ic_notification_reminder_24)
                .setContentTitle(getApplicationContext().getString(R.string.reminder_notification_title))
                .setContentText(getApplicationContext().getString(
                        R.string.reminder_notification_body,
                        vehicle.getRegistration(),
                        expiryDate
                ))
                .setStyle(new NotificationCompat.BigTextStyle().bigText(
                        getApplicationContext().getString(
                                R.string.reminder_notification_body,
                                vehicle.getRegistration(),
                                expiryDate
                        )
                ))
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManagerCompat.from(getApplicationContext())
                .notify(registration.toUpperCase().hashCode(), builder.build());
        return Result.success();
    }
}

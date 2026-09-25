package com.example.deptflow.communication.services;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.example.deptflow.R;
import com.example.deptflow.communication.ChatActivity;
import com.example.deptflow.communication.TaskDiscussionChatActivity;
import com.example.deptflow.communication.models.AppNotification;
import com.google.android.material.button.MaterialButton;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * InAppBannerManager — Displays polished, animated floating popups at the top of the screen
 * when a new chat message, task assignment, or team discussion update arrives while the app is active.
 */
public class InAppBannerManager {

    private static final String TAG = "InAppBannerManager";
    private static final Set<String> PROCESSED_EVENT_IDS = new HashSet<>();
    private static final Handler HANDLER = new Handler(Looper.getMainLooper());

    private static View activeBannerView = null;
    private static Runnable autoDismissRunnable = null;

    /**
     * Displays an in-app banner on the given activity.
     */
    public static void showBanner(Activity activity, AppNotification notification) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed() || notification == null) {
            return;
        }

        String eventId = notification.getId();
        if (eventId != null && !eventId.isEmpty()) {
            if (PROCESSED_EVENT_IDS.contains(eventId)) {
                return; // Prevent duplicate popups
            }
            PROCESSED_EVENT_IDS.add(eventId);
            if (PROCESSED_EVENT_IDS.size() > 200) {
                PROCESSED_EVENT_IDS.clear();
            }
        }

        HANDLER.post(() -> displayBannerInternal(activity, notification));
    }

    private static void displayBannerInternal(Activity activity, AppNotification notification) {
        dismissActiveBanner();

        ViewGroup decor = (ViewGroup) activity.getWindow().getDecorView();
        View banner = LayoutInflater.from(activity).inflate(R.layout.layout_in_app_banner, decor, false);

        TextView tvTitle = banner.findViewById(R.id.tvBannerTitle);
        TextView tvCategory = banner.findViewById(R.id.tvBannerCategory);
        TextView tvMessage = banner.findViewById(R.id.tvBannerMessage);
        TextView tvInitial = banner.findViewById(R.id.tvBannerInitial);
        ImageButton ibDismiss = banner.findViewById(R.id.ibBannerDismiss);
        MaterialButton btnDismiss = banner.findViewById(R.id.btnBannerDismiss);
        MaterialButton btnAction = banner.findViewById(R.id.btnBannerAction);

        String sender = notification.getSender();
        if (sender == null || sender.trim().isEmpty()) {
            sender = "Faculty Member";
        }
        tvTitle.setText(sender);

        String initial = sender.length() > 0 ? sender.substring(0, 1).toUpperCase(Locale.ROOT) : "F";
        tvInitial.setText(initial);

        String msg = notification.getMessage();
        if (msg == null || msg.trim().isEmpty()) {
            msg = notification.getSubtitle();
        }
        tvMessage.setText(msg != null ? msg : "New notification");

        String type = notification.getType();
        if (AppNotification.TYPE_CHAT.equalsIgnoreCase(type)) {
            tvCategory.setText("💬 NEW MESSAGE");
            tvCategory.setTextColor(ContextCompat.getColor(activity, R.color.primary));
            btnAction.setText("Open Chat");
            btnAction.setOnClickListener(v -> {
                dismissActiveBanner();
                Intent intent = new Intent(activity, ChatActivity.class);
                intent.putExtra("facultyName", notification.getSender());
                intent.putExtra("facultyId", notification.getSenderId());
                intent.putExtra("chatId", notification.getChatId());
                activity.startActivity(intent);
            });
        } else if (AppNotification.TYPE_TASK.equalsIgnoreCase(type)) {
            tvCategory.setText("📢 NEW TASK ASSIGNED");
            tvCategory.setTextColor(ContextCompat.getColor(activity, R.color.priority_high_text));
            btnAction.setText("View Task");
            btnAction.setOnClickListener(v -> {
                dismissActiveBanner();
                Intent intent = new Intent(activity, TaskDiscussionChatActivity.class);
                intent.putExtra("EXTRA_TASK_ID", notification.getTaskId());
                intent.putExtra("EXTRA_TASK_TITLE", notification.getSubtitle());
                intent.putExtra("EXTRA_TASK_DEADLINE", notification.getDeadline());
                intent.putExtra("EXTRA_TASK_PRIORITY", notification.getPriority());
                intent.putExtra("taskName", notification.getSubtitle());
                activity.startActivity(intent);
            });
        } else {
            tvCategory.setText("🔔 NOTIFICATION");
            tvCategory.setTextColor(ContextCompat.getColor(activity, R.color.primary));
            btnAction.setText("View");
            btnAction.setOnClickListener(v -> dismissActiveBanner());
        }

        View.OnClickListener dismissListener = v -> dismissActiveBanner();
        ibDismiss.setOnClickListener(dismissListener);
        btnDismiss.setOnClickListener(dismissListener);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = getStatusBarHeight(activity);

        banner.setLayoutParams(params);
        banner.setTranslationY(-300f);
        banner.setAlpha(0f);

        decor.addView(banner);
        activeBannerView = banner;

        banner.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(350)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();

        // Auto dismiss after 6 seconds
        autoDismissRunnable = () -> dismissActiveBanner();
        HANDLER.postDelayed(autoDismissRunnable, 6000);
    }

    public static void dismissActiveBanner() {
        if (autoDismissRunnable != null) {
            HANDLER.removeCallbacks(autoDismissRunnable);
            autoDismissRunnable = null;
        }

        if (activeBannerView != null) {
            final View viewToDismiss = activeBannerView;
            activeBannerView = null;

            viewToDismiss.animate()
                    .translationY(-300f)
                    .alpha(0f)
                    .setDuration(250)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .withEndAction(() -> {
                        if (viewToDismiss.getParent() instanceof ViewGroup) {
                            ((ViewGroup) viewToDismiss.getParent()).removeView(viewToDismiss);
                        }
                    })
                    .start();
        }
    }

    private static int getStatusBarHeight(Context context) {
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        return resourceId > 0 ? context.getResources().getDimensionPixelSize(resourceId) : 48;
    }
}

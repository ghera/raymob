package com.raylib.raymob.bridge;

import android.app.NativeActivity;
import android.util.Log;
import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;

public class ReviewHelper {
  private static final String TAG = "raymobReview";

  private final NativeActivity activity;

  public ReviewHelper(android.content.Context context) {
    activity = (NativeActivity) context;
  }

  public void requestInAppReview() {
    ReviewManager reviewManager = ReviewManagerFactory.create(activity);
    reviewManager
        .requestReviewFlow()
        .addOnCompleteListener(
            task -> {
              if (!task.isSuccessful()) {
                Log.w(TAG, "requestReviewFlow failed", task.getException());
                return;
              }

              ReviewInfo reviewInfo = task.getResult();
              reviewManager
                  .launchReviewFlow(activity, reviewInfo)
                  .addOnCompleteListener(
                      flowTask -> {
                        if (!flowTask.isSuccessful()) {
                          Log.w(TAG, "launchReviewFlow failed", flowTask.getException());
                          return;
                        }

                        Log.i(TAG, "launchReviewFlow completed");
                      });
            });
  }
}

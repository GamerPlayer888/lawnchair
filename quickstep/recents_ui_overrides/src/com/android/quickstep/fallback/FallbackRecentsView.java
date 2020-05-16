/*
 * Copyright (C) 2018 The Android Open Source Project
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
package com.android.quickstep.fallback;

import static com.android.quickstep.fallback.RecentsState.DEFAULT;
import static com.android.quickstep.fallback.RecentsState.MODAL_TASK;
import static com.android.quickstep.util.WindowSizeStrategy.FALLBACK_RECENTS_SIZE_STRATEGY;

import android.annotation.TargetApi;
import android.app.ActivityManager.RunningTaskInfo;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;

import com.android.launcher3.statemanager.StateManager.StateListener;
import com.android.quickstep.RecentsActivity;
import com.android.quickstep.views.OverviewActionsView;
import com.android.quickstep.views.RecentsView;
import com.android.systemui.shared.recents.model.Task;
import com.android.systemui.shared.recents.model.Task.TaskKey;

import java.util.ArrayList;

@TargetApi(Build.VERSION_CODES.R)
public class FallbackRecentsView extends RecentsView<RecentsActivity>
        implements StateListener<RecentsState> {

    private RunningTaskInfo mRunningTaskInfo;

    public FallbackRecentsView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FallbackRecentsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr, FALLBACK_RECENTS_SIZE_STRATEGY);
        mActivity.getStateManager().addStateListener(this);
    }

    @Override
    public void init(OverviewActionsView actionsView) {
        super.init(actionsView);
        setOverviewStateEnabled(true);
        setOverlayEnabled(true);
    }

    @Override
    public void startHome() {
        mActivity.startHome();
    }

    @Override
    public boolean shouldUseMultiWindowTaskSizeStrategy() {
        // Just use the activity task size for multi-window as well.
        return false;
    }

    public void onGestureAnimationStart(RunningTaskInfo runningTaskInfo) {
        mRunningTaskInfo = runningTaskInfo;
        onGestureAnimationStart(runningTaskInfo == null ? -1 : runningTaskInfo.taskId);
    }

    @Override
    public void setCurrentTask(int runningTaskId) {
        super.setCurrentTask(runningTaskId);
        if (mRunningTaskInfo != null && mRunningTaskInfo.taskId != runningTaskId) {
            mRunningTaskInfo = null;
        }
    }

    @Override
    protected void applyLoadPlan(ArrayList<Task> tasks) {
        // When quick-switching on 3p-launcher, we add a "dummy" tile corresponding to Launcher
        // as well. This tile is never shown as we have setCurrentTaskHidden, but allows use to
        // track the index of the next task appropriately, as if we are switching on any other app.
        if (mRunningTaskInfo != null && mRunningTaskInfo.taskId == mRunningTaskId) {
            // Check if the task list has running task
            boolean found = false;
            for (Task t : tasks) {
                if (t.key.id == mRunningTaskId) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                ArrayList<Task> newList = new ArrayList<>(tasks.size() + 1);
                newList.addAll(tasks);
                newList.add(Task.from(new TaskKey(mRunningTaskInfo), mRunningTaskInfo, false));
                tasks = newList;
            }
        }
        super.applyLoadPlan(tasks);
    }

    @Override
    public void setModalStateEnabled(boolean isModalState) {
        super.setModalStateEnabled(isModalState);
        if (isModalState) {
            mActivity.getStateManager().goToState(RecentsState.MODAL_TASK);
        } else {
            if (mActivity.isInState(RecentsState.MODAL_TASK)) {
                mActivity.getStateManager().goToState(DEFAULT);
            }
        }
    }

    @Override
    public void onStateTransitionStart(RecentsState toState) {
        setOverviewStateEnabled(true);
        setFreezeViewVisibility(true);
    }

    @Override
    public void onStateTransitionComplete(RecentsState finalState) {
        setOverlayEnabled(finalState == DEFAULT || finalState == MODAL_TASK);
        setFreezeViewVisibility(false);
    }

    @Override
    public void setOverviewStateEnabled(boolean enabled) {
        super.setOverviewStateEnabled(enabled);
        if (enabled) {
            RecentsState state = mActivity.getStateManager().getState();
            setDisallowScrollToClearAll(!state.hasButtons());
        }
    }
}

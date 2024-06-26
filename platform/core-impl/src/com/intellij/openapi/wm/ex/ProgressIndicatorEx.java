/*
 * Copyright 2000-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.openapi.wm.ex;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.TaskInfo;
import org.jetbrains.annotations.NotNull;

/**
 * <h3>Obsolescence notice</h3>
 * <p>
 * See {@link com.intellij.openapi.progress.ProgressIndicator} notice.
 * </p>
 */
public interface ProgressIndicatorEx extends ProgressIndicator {
  void addStateDelegate(@NotNull ProgressIndicatorEx delegate);

  void finish(@NotNull TaskInfo task);

  boolean isFinished(@NotNull TaskInfo task);

  boolean wasStarted();

  void processFinish();

  void initStateFrom(@NotNull ProgressIndicator indicator);
}

/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.codeInsight.daemon.impl.analysis;

import com.intellij.psi.PsiReference;
import com.intellij.xml.util.AnchorReference;
import org.jetbrains.annotations.NotNull;

public class HtmlUnknownAnchorTargetInspection extends XmlPathReferenceInspection {
  @NotNull
  @Override
  public String getShortName() {
    return "HtmlUnknownAnchorTarget";
  }

  @Override
  protected boolean isForHtml() {
    return true;
  }

  @Override
  protected boolean needToCheckRef(PsiReference reference) {
    return super.needToCheckRef(reference) && reference instanceof AnchorReference && HtmlUnknownTargetInspection.notRemoteBase(reference);
  }
}

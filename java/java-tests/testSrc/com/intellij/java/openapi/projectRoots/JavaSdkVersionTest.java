// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.java.openapi.projectRoots;

import com.intellij.openapi.projectRoots.JavaSdkVersion;
import com.intellij.pom.java.LanguageLevel;
import com.intellij.util.lang.JavaVersion;
import org.jetbrains.jps.model.java.JdkVersionDetector;
import org.junit.Test;

import static org.junit.Assert.*;

public class JavaSdkVersionTest {
  @Test
  public void sdkVersionFromLanguageLevel() {
    assertEquals(JavaSdkVersion.JDK_1_3, JavaSdkVersion.fromLanguageLevel(LanguageLevel.JDK_1_3));
    assertEquals(JavaSdkVersion.JDK_1_6, JavaSdkVersion.fromLanguageLevel(LanguageLevel.JDK_1_6));
    assertEquals(JavaSdkVersion.JDK_1_8, JavaSdkVersion.fromLanguageLevel(LanguageLevel.JDK_1_8));
    assertEquals(JavaSdkVersion.JDK_11, JavaSdkVersion.fromLanguageLevel(LanguageLevel.JDK_11));
  }

  @Test
  public void maxLanguageLevelSanity() {
    for (JavaSdkVersion version : JavaSdkVersion.values()) {
      LanguageLevel languageLevel = version.getMaxLanguageLevel();
      assertFalse("Fails for " + version, languageLevel.isPreview() && languageLevel != LanguageLevel.JDK_X);
    }
  }

  @Test
  public void sdkVersionFromVersionString() {
    assertEquals(JavaSdkVersion.JDK_1_8, JavaSdkVersion.fromVersionString("1.8.0_131"));
    assertEquals(JavaSdkVersion.JDK_1_9, JavaSdkVersion.fromVersionString("9"));
    assertEquals(JavaSdkVersion.JDK_1_9, JavaSdkVersion.fromVersionString("9-ea"));
    assertEquals(JavaSdkVersion.JDK_1_9, JavaSdkVersion.fromVersionString("9.1.2"));

    assertEquals(JavaSdkVersion.JDK_1_8, JavaSdkVersion.fromVersionString("java version \"1.8.0_131\""));
    assertEquals(JavaSdkVersion.JDK_1_9, JavaSdkVersion.fromVersionString("java version \"9\""));
    assertEquals(JavaSdkVersion.JDK_1_9, JavaSdkVersion.fromVersionString("java version \"9-ea\""));
    assertEquals(JavaSdkVersion.JDK_1_9, JavaSdkVersion.fromVersionString("java version \"9.1.2\""));
    assertEquals(JavaSdkVersion.JDK_14, JavaSdkVersion.fromVersionString("java version \"14\""));

    assertEquals(JavaSdkVersion.JDK_21, JavaSdkVersion.fromVersionString("GraalVM 23.1.2 - Java 21.0.2"));
  }

  @Test
  public void versionStringDetectorSanity() {
    assertTrue(JdkVersionDetector.isVersionString("java version \"9\""));
    assertFalse(JdkVersionDetector.isVersionString("java version \"\""));

    assertTrue(JdkVersionDetector.isVersionString(JdkVersionDetector.formatVersionString(JavaVersion.compose(9))));
  }
}
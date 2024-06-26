package de.plushnikov.intellij.plugin.util;

import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.*;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public final class PsiAnnotationSearchUtil {

  @Nullable
  public static PsiAnnotation findAnnotation(@NotNull PsiModifierListOwner psiModifierListOwner, @NotNull String annotationFQN) {
    if (isDumbMode(psiModifierListOwner)) {
      return findAnnotationInDumbMode(psiModifierListOwner, annotationFQN);
    }
    return psiModifierListOwner.getAnnotation(annotationFQN);
  }

  private static @Nullable PsiAnnotation findAnnotationInDumbMode(@NotNull PsiModifierListOwner owner, @NotNull String annotationFQN) {
    for (PsiAnnotation annotation : owner.getAnnotations()) {
      if (hasQualifiedNameInDumbMode(annotation, annotationFQN)) {
        return annotation;
      }
    }
    return null;
  }

  private static boolean hasQualifiedNameInDumbMode(PsiAnnotation annotation, @NotNull String fqn) {
    PsiJavaCodeReferenceElement referenceElement = annotation.getNameReferenceElement();
    if (referenceElement == null) return false;
    String qualifiedName = referenceElement.getReferenceName();
    if (qualifiedName == null) return false;
    if (qualifiedName.equals(fqn)) return true;
    String referenceElementText = referenceElement.getText();
    if (referenceElementText != null && referenceElementText.equals(fqn)) return true;
    if (!fqn.endsWith(qualifiedName)) return false;
    PsiFile containingFile = annotation.getContainingFile();
    if (!(containingFile instanceof PsiJavaFile javaFile)) {
      return false;
    }
    String packageName = StringUtil.getPackageName(fqn);
    PsiImportList importList = javaFile.getImportList();
    if (importList == null) return false;
    int indexMayByOuterClass = fqn.length() - qualifiedName.length() - 1;
    String mayBeOuterClass = indexMayByOuterClass > 0 ? fqn.substring(0, indexMayByOuterClass) : null;
    return importList.findOnDemandImportStatement(packageName) != null ||
           importList.findSingleClassImportStatement(fqn) != null ||
           (mayBeOuterClass!=null && importList.findSingleClassImportStatement(mayBeOuterClass) != null);
  }

  private static boolean isDumbMode(@NotNull PsiElement context) {
    Project project = context.getProject();
    return DumbService.isDumb(project);
  }

  @Nullable
  public static PsiAnnotation findAnnotation(@NotNull PsiModifierListOwner psiModifierListOwner, String @NotNull ... annotationFQNs) {
    boolean isDumbMode = isDumbMode(psiModifierListOwner);
    for (String annotationFQN : annotationFQNs) {
      PsiAnnotation annotation;
      if (isDumbMode) {
        annotation = findAnnotationInDumbMode(psiModifierListOwner, annotationFQN);
      }
      else {
        annotation = psiModifierListOwner.getAnnotation(annotationFQN);
      }
      if (annotation != null) {
        return annotation;
      }
    }
    return null;
  }

  public static boolean isAnnotatedWith(@NotNull PsiModifierListOwner psiModifierListOwner, @NotNull String annotationFQN) {
    if (isDumbMode(psiModifierListOwner)) {
      return findAnnotationInDumbMode(psiModifierListOwner, annotationFQN) != null;
    }
    return psiModifierListOwner.hasAnnotation(annotationFQN);
  }

  public static boolean isNotAnnotatedWith(@NotNull PsiModifierListOwner psiModifierListOwner, String annotationTypeName) {
    return !isAnnotatedWith(psiModifierListOwner, annotationTypeName);
  }

  public static boolean isAnnotatedWith(@NotNull PsiModifierListOwner psiModifierListOwner, String @NotNull ... annotationTypes) {
    return null != findAnnotation(psiModifierListOwner, annotationTypes);
  }

  public static boolean isNotAnnotatedWith(@NotNull PsiModifierListOwner psiModifierListOwner, String @NotNull ... annotationTypes) {
    return !isAnnotatedWith(psiModifierListOwner, annotationTypes);
  }

  @NotNull
  public static String getShortNameOf(@NotNull PsiAnnotation psiAnnotation) {
    PsiJavaCodeReferenceElement referenceElement = psiAnnotation.getNameReferenceElement();
    return StringUtil.notNullize(null == referenceElement ? null : referenceElement.getReferenceName());
  }

  public static boolean checkAnnotationsSimpleNameExistsIn(@NotNull PsiModifierListOwner modifierListOwner,
                                                           @NotNull Collection<String> annotationNames) {
    for (PsiAnnotation psiAnnotation : modifierListOwner.getAnnotations()) {
      final String shortName = getShortNameOf(psiAnnotation);
      if (annotationNames.contains(shortName)) {
        return true;
      }
    }
    return false;
  }

  @Nullable
  public static PsiAnnotation findAnnotationByShortNameOnly(@NotNull PsiModifierListOwner psiModifierListOwner,
                                                            String @NotNull ... annotationFQNs) {
    if (annotationFQNs.length > 0) {
      Collection<String> possibleShortNames = ContainerUtil.map(annotationFQNs, StringUtil::getShortName);

      for (PsiAnnotation psiAnnotation : psiModifierListOwner.getAnnotations()) {
        String shortNameOfAnnotation = getShortNameOf(psiAnnotation);
        if(possibleShortNames.contains(shortNameOfAnnotation)) {
          return psiAnnotation;
        }
      }
    }
    return null;
  }

  public static boolean checkAnnotationHasOneOfFQNs(@NotNull PsiAnnotation psiAnnotation,
                                                    String @NotNull ... annotationFQNs) {
    if (isDumbMode(psiAnnotation)) {
      return ContainerUtil.or(annotationFQNs, fqn-> hasQualifiedNameInDumbMode(psiAnnotation, fqn));
    }
    return ContainerUtil.or(annotationFQNs, psiAnnotation::hasQualifiedName);
  }
}

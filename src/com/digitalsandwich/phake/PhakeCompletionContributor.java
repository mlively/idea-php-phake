package com.digitalsandwich.phake;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.PhpIcons;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import org.jetbrains.annotations.NotNull;
import java.util.Collection;

/**
 * Handles code completion of class names as strings for mock() partialMock() and partMock()
 */
public class PhakeCompletionContributor extends CompletionContributor
{
    public PhakeCompletionContributor()
    {
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(), new CompletionProvider<CompletionParameters>() {
            @Override
            protected void addCompletions(@NotNull CompletionParameters completionParameters, ProcessingContext processingContext, @NotNull CompletionResultSet completionResultSet) {
                MethodReference method = PsiTreeUtil.getContextOfType(completionParameters.getOriginalPosition(), MethodReference.class, true);

                if (method == null)
                {
                    return;
                }

                PsiElement[] parameters = method.getParameters();
                if (parameters.length < 1 || !(parameters[0] instanceof StringLiteralExpression))
                {
                    return;
                }


                if (method.getSignature().equals("#M#C\\Phake.mock") || method.getSignature().equals("#M#C\\Phake.partMock") || method.getSignature().equals("#M#C\\Phake.partialMock"))
                {
                    PhpIndex phpIndex = PhpIndex.getInstance(method.getProject());
                    Collection<String> classNames = phpIndex.getAllClassNames(null);
                    for (String className : classNames)
                    {
                        LookupElementBuilder lookupElement = LookupElementBuilder.create(className)
                                .withTypeText(className)
                                .withIcon(PhpIcons.CLASS_ICON);
                        completionResultSet.addElement(lookupElement);
                    }

                    Collection<String> interfaceNames = phpIndex.getAllInterfaceNames();
                    for (String interfaceName : interfaceNames)
                    {
                        LookupElementBuilder lookupElement = LookupElementBuilder.create(interfaceName)
                                .withTypeText(interfaceName)
                                .withIcon(PhpIcons.INTERFACE_ICON);
                        completionResultSet.addElement(lookupElement);
                    }
                }
            }
        });
    }
}

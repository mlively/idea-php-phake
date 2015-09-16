package com.digitalsandwich.phake;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import com.jetbrains.php.lang.psi.resolve.types.PhpTypeProvider2;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Handles the type translation
 */
public class PhakeMockTypeProvider implements PhpTypeProvider2
{

    public static final String CALLTYPE_MOCK = "<01>";
    public static final String CALLTYPE_VERIFICATION = "<02>";
    public static final String CALLTYPE_STUB = "<03>";
    public static final String CALLTYPE_STUBBED_METHOD = "<04>";

    @Override
    public char getKey() {
        return '\u2002';
    }

    @Nullable
    @Override
    public String getType(PsiElement psiElement) {
        if (psiElement instanceof MethodReference)
        {
            MethodReference methodReference = (MethodReference) psiElement;
            String signature = methodReference.getSignature();
            if (isMockCall(signature))
            {
                PsiElement[] parameters = methodReference.getParameters();

                if (parameters.length > 0)
                {
                    PsiElement parameter = parameters[0];
                    if (parameter instanceof StringLiteralExpression)
                    {
                        String phpClassName = ((StringLiteralExpression)parameter).getContents();

                        if (StringUtil.isNotEmpty(phpClassName))
                        {
                            return CALLTYPE_MOCK + signature + "~" + phpClassName;
                        }
                    }
                }
            }

            else if (isVerifyCall(signature))
            {
                int parameterPosition = 0;
                String typeList = passThruMethodParameterType(methodReference, parameterPosition);
                if (StringUtil.isNotEmpty(typeList))
                {
                    return CALLTYPE_VERIFICATION + typeList;
                }
            }

            else if (isWhenCall(signature))
            {
                int parameterPosition = 0;
                String typeList = passThruMethodParameterType(methodReference, parameterPosition);
                if (StringUtil.isNotEmpty(typeList))
                {
                    return CALLTYPE_STUB + typeList;
                }
            }
            else if (signature.startsWith("#M#") && signature.contains(CALLTYPE_STUB))
            {
                MethodReference previousMethodInChain = PsiTreeUtil.findChildOfType(psiElement, MethodReference.class);

                if (previousMethodInChain != null && (isWhenCall(previousMethodInChain.getSignature())))
                {
                    return CALLTYPE_STUBBED_METHOD;
                }
            }
            else if (signature.startsWith("#M#") && signature.contains(CALLTYPE_STUBBED_METHOD))
            {
                return CALLTYPE_STUBBED_METHOD;
            }
        }
        return null;
    }

    @Nullable
    private String passThruMethodParameterType(MethodReference methodReference, int parameterPosition) {
        String typeList = null;
        PsiElement[] parameters = methodReference.getParameters();
        if (parameters.length > parameterPosition)
        {
            PsiElement parameter = parameters[parameterPosition];
            if (parameter instanceof Variable)
            {
                PhpType type = ((Variable) parameter).getType();
                typeList = StringUtil.join(type.getTypes(), "|");
            }
            else if (parameter instanceof PhpReference)
            {
                typeList = ((PhpReference) parameter).getSignature();
            }
        }
        return typeList;
    }

    @Override
    public Collection<? extends PhpNamedElement> getBySignature(String s, Project project) {

        PhpIndex phpIndex = PhpIndex.getInstance(project);
        Collection<PhpNamedElement> signedClasses = new ArrayList<PhpNamedElement>();
        if (s.substring(0, 4).equals(CALLTYPE_MOCK))
        {
            int separator = s.indexOf("~");
            String phakeSignature = s.substring(4, separator);
            String className = s.substring(separator + 1);

            PhpClass phpClass = phpIndex.getClassByName(className);
            signedClasses.addAll(phpIndex.getBySignature(phakeSignature));
            if (phpClass != null)
            {
                signedClasses.add(phpClass);
            }
            else
            {
                signedClasses.addAll(phpIndex.getInterfacesByName(className));
            }
        }
        else if (s.substring(0, 4).equals(CALLTYPE_VERIFICATION) || s.substring(0, 4).equals(CALLTYPE_STUB))
        {
            for (String signature : StringUtil.split(s.substring(4),"|"))
            {
                Collection<? extends PhpNamedElement> phpNamedElements = phpIndex.getBySignature(signature);
                signedClasses.addAll(phpNamedElements);
            }
        }
        else if (s.substring(0, 4).equals(CALLTYPE_STUBBED_METHOD))
        {
            PhpClass answerBinder = phpIndex.getClassByName("Phake_Proxies_AnswerBinderProxy");
            signedClasses.add(answerBinder);
        }

        return signedClasses.size() == 0 ? null : signedClasses;
    }

    private boolean isWhenCall(String signature) {
        return signature.equals("#M#C\\Phake.when") || signature.equals("#M#C\\Phake.whenStatic");
    }

    private boolean isVerifyCall(String signature) {
        return signature.equals("#M#C\\Phake.verify") || signature.equals("#M#C\\Phake.verifyStatic");
    }

    private boolean isMockCall(String signature) {
        return signature.equals("#M#C\\Phake.mock") || signature.equals("#M#C\\Phake.partialMock") || signature.equals("#M#C\\Phake.partMock");
    }
}

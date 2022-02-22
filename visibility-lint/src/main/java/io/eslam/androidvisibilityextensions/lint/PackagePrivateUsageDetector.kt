package io.eslam.androidvisibilityextensions.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.*
import com.android.tools.lint.detector.api.Detector.UastScanner
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.PsiClassReferenceType
import org.jetbrains.kotlin.asJava.elements.KtLightDeclaration
import org.jetbrains.kotlin.asJava.elements.KtLightMember
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.psiUtil.containingClass
import org.jetbrains.uast.*
import org.jetbrains.uast.kotlin.KotlinUAnnotation

@Suppress("UnstableApiUsage")
class PackagePrivateUsageDetector : Detector(), UastScanner {

    override fun getApplicableUastTypes(): List<Class<out UElement>> =
        listOf(UExpression::class.java, UParameter::class.java, UAnnotation::class.java)


    override fun createUastHandler(context: JavaContext): UElementHandler =
        object : UElementHandler() {

            override fun visitAnnotation(node: UAnnotation) {
                check(node, node.resolve())
            }

            override fun visitParameter(node: UParameter) {
                check(node, (node.typeReference?.type as? PsiClassReferenceType)?.resolve())
            }

            override fun visitExpression(node: UExpression) {
                if (node is UQualifiedReferenceExpression) {
                    // Avoid duplication by handling lower level part of the reference
                    // e.g. instance.value will be handled as just value
                    return
                }
                check(node, node.tryResolve())
            }

            private fun check(node: UElement, resolved: PsiElement?) {
                (resolved as? KtLightDeclaration<*, *>)?.let {
                    if (resolved.isAnnotatedWith(packagePrivateAnnotationName) && areInDifferentPackages(
                            context,
                            node,
                            resolved
                        )
                    ) {
                        context.report(
                            PackagePrivateIssue,
                            node,
                            context.getLocation(node),
                            "Usage of private api"
                        )
                    }
                }
            }
        }

    private fun areInDifferentPackages(
        context: JavaContext,
        node: UElement,
        resolved: KtLightDeclaration<*, *>
    ): Boolean {
        val declarationPackage = context.evaluator.getPackage(resolved)
        val referencePackage = context.evaluator.getPackage(node)
        return declarationPackage?.qualifiedName != referencePackage?.qualifiedName
    }

    private fun KtLightDeclaration<*, *>.isAnnotatedWith(qualifiedName: String): Boolean {
        if (listOf(
                kotlinOrigin,
                kotlinOrigin?.containingKtFile
            ).any { it?.isAnnotatedWith(qualifiedName) == true }
        ) {
            return true
        }

        var node = kotlinOrigin?.containingClass()
        while (node != null) {
            if (node.isAnnotatedWith(qualifiedName)) return true
            node = node.containingClass()
        }

        if ((this as? KtLightMember<*>)?.containingClass?.isAnnotatedWith(qualifiedName) == true) return true

        return false
    }

    private fun KtAnnotated.isAnnotatedWith(qualifiedName: String) =
        annotationEntries.mapNotNull {
            it.toUElement() as? KotlinUAnnotation
        }.any {
            it.qualifiedName == qualifiedName
        }

    companion object {
        private const val packagePrivateAnnotationName =
            "io.eslam.androidvisibilityextensions.PackagePrivate"

        val PackagePrivateIssue = Issue.create(
            "PackagePrivateID",
            "Kotlin package visibility",
            "This check highlights usage of members marked private in package they are declared in",
            Category.CORRECTNESS,
            10,
            Severity.ERROR,
            Implementation(PackagePrivateUsageDetector::class.java, Scope.JAVA_FILE_SCOPE)
        )
    }
}


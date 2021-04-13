package com.pgschaaf.launchurlfromstring

import com.intellij.ide.plugins.PluginManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.StandardPatterns
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.elementType
import com.intellij.util.ProcessingContext
import com.jetbrains.rest.RestElementTypes
import com.jetbrains.rest.RestTokenTypes
import com.pgschaaf.launchurlfromstring.LanguageElementTextReferenceProvider.clickableString
import java.util.stream.Collectors

object LanguageElementRestReferenceContributor: PsiReferenceContributor() {
    private val log = Logger.getInstance(LanguageElementRestReferenceContributor.javaClass)

    init {
        log.info("plugins: " + PluginManager
            .getPlugins()
            .map { it.pluginId to it.name }
            .joinToString())
    }

    private val pluginId = "org.jetbrains.plugins.rest"

    private val classLoaders =
        PluginManager
            .getPlugins()
            .associate {it.pluginId.idString to it.pluginClassLoader}
            .withDefault {javaClass.classLoader} // if no pluginID was provided, map to the normal classloader

    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        val restTokenTypesClass =
            classLoaders.getValue(pluginId).tryToLoad<Class<*>>("com.jetbrains.rest.RestTokenTypes", pluginId).get()
        val restInterpretedTokenType =
            (restTokenTypesClass.getDeclaredField("INTERPRETED").get(restTokenTypesClass)) as IElementType
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(restInterpretedTokenType),
            LanguageElementRestReferenceProvider
        )
    }
}

object LanguageElementRestReferenceProvider: PsiReferenceProvider() {
    private val log = Logger.getInstance(LanguageElementRestReferenceProvider.javaClass)

    private fun findReferencesToJavaClasses(
        element: PsiElement,
        project: Project,
        ref: String
    ): List<PsiReference> {
        return JavaPsiFacade.getInstance(project)
            .findClasses(ref, GlobalSearchScope.allScope(project))
            .map { SingleTargetElementReference(element, it) }
    }

    private val referenceFinders = listOf<(PsiElement, Project, String) -> List<PsiReference>>(
        ::findReferencesToJavaClasses
    )

    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
        val str = element.text.removeSurrounding("`")
        val project = element.project
        val references = mutableListOf<PsiReference>()
        for (find in referenceFinders) {
            for (reference in find(element, project, str)) {
                references.add(reference)
            }
        }
        return references.toTypedArray()
    }
}

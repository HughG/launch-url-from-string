package com.pgschaaf.launchurlfromstring

import com.intellij.ide.plugins.PluginManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.patterns.StandardPatterns
import com.intellij.psi.*
import com.intellij.util.ProcessingContext
import java.util.stream.Collectors

object LanguageElementRestReferenceContributor: PsiReferenceContributor() {
    private val log = Logger.getInstance(LanguageElementRestReferenceContributor.javaClass)

    init {
        log.info("plugins: " + PluginManager
            .getPlugins()
            .map { it.pluginId to it.name }
            .joinToString())
    }

    private val RestPluginPsiElementNames = listOf(
        "RestFieldList",
        "RestInlineBlock",
        "RestLine",
        "RestReference",
        "RestReferenceTarget",
        "RestRole",
        "RestTitle"
    )

    private val pluginId = "org.jetbrains.plugins.rest"

    private val classLoaders =
        PluginManager
            .getPlugins()
            .associate {it.pluginId.idString to it.pluginClassLoader}
            .withDefault {javaClass.classLoader} // if no pluginID was provided, map to the normal classloader

    private val referenceProviders = RestPluginPsiElementNames
        .map { className ->
            classLoaders
                .getValue(pluginId)
                .tryToLoad<PsiElement>(className, pluginId)}
        .filter {it.isPresent}
        .map { StandardPatterns.instanceOf(it.get())}

    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        log.info("referenceProviders: " + referenceProviders.map { it.javaClass.canonicalName }.joinToString())
        referenceProviders.forEach {
            registrar.registerReferenceProvider(it, LanguageElementRestReferenceProvider)
        }
    }
}

object LanguageElementRestReferenceProvider: PsiReferenceProvider() {
    private val log = Logger.getInstance(LanguageElementTextReferenceProvider.javaClass)

    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
        log.info(element.toString())
        return arrayOf()
    }

}
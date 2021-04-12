package com.pgschaaf.launchurlfromstring

import com.intellij.ide.plugins.PluginManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.patterns.StandardPatterns
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiShortNamesCache
import com.intellij.psi.xml.XmlAttributeValue
import com.intellij.psi.xml.XmlTag
import com.intellij.util.ProcessingContext
import java.util.*
import java.util.stream.Collectors

object LanguageElementTextReferenceContributor: PsiReferenceContributor() {
    private val log = Logger.getInstance(LanguageElementTextReferenceContributor.javaClass)

    init {
        log.info("plugins: " + PluginManager
            .getPlugins()
            .map { it.pluginId to it.name }
            .joinToString())
    }

    private val classLoaders =
        PluginManager
            .getPlugins()
            .associate {it.pluginId.idString to it.pluginClassLoader}
            .withDefault {javaClass.classLoader} // if no pluginID was provided, map to the normal classloader

    private val referenceProviders =
        ResourceBundle
            .getBundle(RegexPsiReferenceContributor.ClassMapPropertiesFileName)
            .keysAndValues()
            .map {(className, pluginId)->
                classLoaders
                    .getValue(pluginId)
                    .tryToLoad<PsiElement>(className, pluginId)}
            .filter {it.isPresent}
            .map {StandardPatterns.instanceOf(it.get())}
            .collect(Collectors.toList())

    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        log.info("referenceProviders: " + referenceProviders.map { it.javaClass.canonicalName }.joinToString())
        referenceProviders.forEach {
            registrar.registerReferenceProvider(it, LanguageElementTextReferenceProvider)
        }
    }
}

object LanguageElementTextReferenceProvider: PsiReferenceProvider() {
    private val log = Logger.getInstance(LanguageElementTextReferenceProvider.javaClass)

    /** Return the string portion of this PsiElement that should be treated as a hyperlink **/
    val PsiElement.clickableString: String?
        get() {
            val str = when {
                this is XmlAttributeValue -> value
                this is XmlTag -> value.trimmedText
                text == null              -> null
                text.isEmpty()            -> null
                text.first() == '"'       -> text.removeSurrounding("\"")
                text.first() == '\''      -> text.removeSurrounding("'")
                else                      -> text
            }
            return if (str.isNullOrBlank()) null else str
        }

    private fun findJavaFullClassReferences(project: Project, ref: String) =
        JavaPsiFacade.getInstance(project)
            .findClasses(ref, GlobalSearchScope.allScope(project))
            .mapNotNull { it.reference }

    // This doesn't seem to work so far: just linking to "ExampleClass" doesn't work, you need the package too.
    private fun findJavaShortClassReferences(project: Project, ref: String) =
        PsiShortNamesCache.getInstance(project)
            .getClassesByName(ref, GlobalSearchScope.allScope(project))
            .mapNotNull { it.reference }

    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
        val str = element.clickableString ?: return arrayOf()
        val project = element.project
        val refs = listOf<(Project, String) -> List<PsiReference>>(
            ::findJavaFullClassReferences,
            ::findJavaShortClassReferences
        ).flatMap { it(project, str) }.toTypedArray()
        return refs
    }

}
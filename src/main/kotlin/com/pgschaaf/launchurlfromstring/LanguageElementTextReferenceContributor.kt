package com.pgschaaf.launchurlfromstring

import com.intellij.ide.plugins.PluginManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.patterns.StandardPatterns
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.xml.XmlAttributeValue
import com.intellij.psi.xml.XmlTag
import com.intellij.util.ProcessingContext
import java.util.*
import java.util.stream.Collectors

object LanguageElementTextReferenceContributor: PsiReferenceContributor() {
    private val log = Logger.getInstance(LanguageElementTextReferenceContributor.javaClass)

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
        referenceProviders.forEach {
            registrar.registerReferenceProvider(it, LanguageElementTextReferenceProvider)
        }
    }
}

internal class SingleTargetElementReference(
    element: PsiElement,
    private val target: PsiElement,
    soft: Boolean = false
) : PsiReferenceBase<PsiElement>(element, soft) {
    override fun resolve(): PsiElement = target
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
        val str = element.clickableString ?: return arrayOf()
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
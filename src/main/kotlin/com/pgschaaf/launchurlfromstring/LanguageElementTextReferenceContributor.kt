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
        log.info("referenceProviders: " + referenceProviders.joinToString())
        referenceProviders.forEach {
            registrar.registerReferenceProvider(it, LanguageElementTextReferenceProvider)
        }
    }
}

class MyReference(
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

    // This actually works!  You can click the link to go to the class, and renaming the class renamed the reference
    // text.
    private fun findReferencesToJavaClasses(
        element: PsiElement,
        project: Project,
        elementFactory: PsiElementFactory,
        ref: String
    ): List<PsiReference> {
        return JavaPsiFacade.getInstance(project)
            .findClasses(ref, GlobalSearchScope.allScope(project))
            .map { MyReference(element, it) }
    }

    // This finds references to Java classes, but the getElement of the reference needs to return the
    // MarkdownLinkDestinationImpl object, rather than the targeted class, so some internal assertion fails and we ebd
    // up with no references from the link at all.
    private fun findJavaFQClassNameReferences(
        element: PsiElement,
        project: Project,
        elementFactory: PsiElementFactory,
        ref: String
    ): List<PsiReference> {
        return listOf(elementFactory.createFQClassNameReferenceElement(ref, GlobalSearchScope.allScope(project)))
    }

    // This either doesn't return any references, or returns one with the wrong getElement value as for
    // findJavaFQClassNameReferences above -- I can't remember.
    private fun findJavaFullClassReferences(
        element: PsiElement,
        project: Project,
        elementFactory: PsiElementFactory,
        ref: String
    ): List<PsiReference> {
        return JavaPsiFacade.getInstance(project)
            .findClasses(ref, GlobalSearchScope.allScope(project))
            .map { elementFactory.createClassReferenceElement(it) }
    }

    // This doesn't find any references: just linking to "ExampleClass" doesn't work, you need the package too.
    private fun findJavaShortClassReferences(
        element: PsiElement,
        project: Project,
        elementFactory: PsiElementFactory,
        ref: String
    ): List<PsiReference> {
        return PsiShortNamesCache.getInstance(project)
            .getClassesByName(ref, GlobalSearchScope.allScope(project))
            .map { elementFactory.createClassReferenceElement(it) }
    }

    private val referenceFinders = listOf<(PsiElement, Project, PsiElementFactory, String) -> List<PsiReference>>(
        ::findReferencesToJavaClasses//,
//        ::findJavaFQClassNameReferences//,
//        ::findJavaFullClassReferences,
//        ::findJavaShortClassReferences
    )

    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
        val str = element.clickableString ?: return arrayOf()
        val project = element.project
        val elementFactory = JavaPsiFacade.getElementFactory(project)
        val references = mutableListOf<PsiReference>()
        for (find in referenceFinders) {
            for (reference in find(element, project, elementFactory, str)) {
                references.add(reference)
            }
        }
        return references.toTypedArray()
    }

}
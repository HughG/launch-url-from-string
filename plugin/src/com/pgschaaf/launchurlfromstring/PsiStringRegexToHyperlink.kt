/*
 * Copyright 2018 P.G. Schaaf <paul.schaaf@gmail.com>
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.pgschaaf.launchurlfromstring

import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.ide.IdeBundle
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vcs.IssueNavigationConfiguration
import com.intellij.psi.*
import com.intellij.psi.xml.XmlAttributeValue
import com.intellij.psi.xml.XmlTag
import javax.swing.Icon

class PsiStringRegexToHyperlink<T: PsiElement>(element: T): PsiPolyVariantReferenceBase<T>(element, true) {
   private val project
      get() = ProjectManager.getInstance().openProjects.first()

   private val issueNavigationConfiguration
      get() = IssueNavigationConfiguration.getInstance(project)

   override fun multiResolve(incompleteCode: Boolean) = multiResolve(element.clickableString)

   private fun multiResolve(stringValue: String): Array<ResolveResult?> =
         if (stringValue.isEmpty())
            arrayOfNulls(0)
         else
            issueNavigationConfiguration.links.stream()
                  .map {link-> link.issuePattern.toRegex() to link.linkRegexp}
                  .filter {(pattern, _)-> pattern.containsMatchIn(stringValue)}
                  .map {(pattern, urlPattern)-> stringValue.replace(pattern, urlPattern)}
                  .map {urlString-> element.resolveNavigationTo(urlString)}
                  .limit(1)  // look no further than the first match
                  .toArray {length-> arrayOfNulls<ResolveResult>(length)}

   override fun getVariants() = arrayOfNulls<Any>(0)

   override fun isReferenceTo(element: PsiElement) = false
}

val PsiElement.clickableString
   get() = when (this) {
      is PsiLiteral        -> value as? String ?: ""
      is XmlAttributeValue -> value ?: ""
      is XmlTag            -> value.trimmedText
      else                 -> text
   }.unquoted

val String.unquoted
   get() = if (this.isEmpty())
      this
   else when (first()) {
      '"'  -> removeSurrounding("\"")
      '\'' -> removeSurrounding("'")
      else -> this
   }

fun PsiElement.resolveNavigationTo(url: String) = RegexNavigablePsiElement(this, url).resolveNavigation()

class RegexNavigablePsiElement(element: PsiElement,
                               private val url: String): NavigatablePsiElement, ItemPresentation, PsiElement by element {
   override fun getName(): String? = null

   override fun canNavigate() = true
   override fun canNavigateToSource() = false
   override fun getNavigationElement() = this
   override fun navigate(b: Boolean) = BrowserUtil.browse(url)

   override fun getIcon(p0: Boolean): Icon = AllIcons.General.Web
   override fun getLocationString(): String? = null
   override fun getPresentation() = this
   override fun getPresentableText(): String = IdeBundle.message("open.url.in.browser.tooltip")

   fun resolveNavigation() = PsiElementResolveResult(this)
}
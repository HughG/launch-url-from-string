# Tasks

* DONE: Add reStructuredText plugin (to plugin deps and to my IDE config).
* BLOCKED: Create a new ReferenceContributor which pulls out interpreted text segments
  * I'm not sure interpreted text segments are represented in the version of the plugin I have but,
  even if they are, they don't override PsiElement#getReferences, so my ReferenceContributor is
  never even called for `.rst` files.   
* BLOCKED: Make it create references based on the role.
* DONE: Implement this for Markdown plugin, since it _does_ provide hooks for references.
* DONE: Make those be references to Java classes.
  * BUT: This only allows me to navigate from the class to the reference in the doc, not the other way :-(
* DONE: Explore what variants of Markdown the plugin supports, to see if it has something not too painful
for reference documentation.
  * CommonMark and GitHub-Flavored Markdown.
* DONE: Find a way to navigate the link from the reference to the class.
  * It turns out that navigating from the class to the reference works even without the plugin, somehow!
* Find a way to make references to individual (test) methods.
  * Looks like you could just do this by splitting off the part of the string after `#` and first finding the `PsiClass`
  then calling `PsiClass.findMethodsByName(java.lang.String)`. 
* Maybe: make the reference a "polyvariant" reference, in case there are multiple classes with the same name,
although that should probably trigger an issue/warning.
* Suppress (or _post hoc_ remove?) file references for links from Markdown files, so that IntelliJ doesn't
complain about them.  (Might require forking the Markdown plugin :-\)
  * Looks like this might be do-able with com.intellij.psi.impl.source.resolve.reference.PsiReferenceRegistrarImpl.unregisterReferenceProvider
  and I could wrap the original provider and only return its references if no Java references are found.   
* Try allowing Markdown "autolinks" to link to Java classes as well, if I can work out how those references are made.
This would presumably require making a custom URL scheme.
* Try "host language in string literals" hook for Rest plugin?
  * PsiLanguageInjectionHost is implemented by com.jetbrains.rest.psi.RestLine.
  * Apparently you can inject references into language injection hosts: see
  https://plugins.jetbrains.com/docs/intellij/additional-minor-features.html#reference-injection
* Make it work also for Groovy classes, and maybe Kotlin.

# Notes

Logs for the launched IDE are in `...\launch-url-from-string\build\idea-sandbox\system\log`
and _not_ in the debugger console.

Upgrading to 2021.1 (for the launched IDE) needs JDK 11.

I discovered that, at least in 2021.1, the "Tools" menu in the launched IDE
has a tool to inspect the PSI tree for the current document -- somewhat handy!

Maybe-useful IntelliJ plugins

* https://github.com/paulschaaf/launch-url-from-string -- very simple: extend it to generate other kinds of PsiReference as well?
  * See https://plugins.jetbrains.com/docs/intellij/psi-references.html.
  * https://stackoverflow.com/questions/14215330/make-intellij-aware-of-links-to-java-elements-in-xml-files
* https://plugins.jetbrains.com/plugin/40-hyperlink
How to have an optional dependency on another plugin, in your IntelliJ plugin: https://intellij-support.jetbrains.com/hc/en-us/community/posts/360009392960-Optional-dependency-JetBrains-Markdown-plugin.

Adding references to a Kotlin string literal (probably not applicable to Rest plugin): https://thsaravana.github.io/blog/intellij-kotlin-psi-reference-contributor/

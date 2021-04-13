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
* Explore what variants of Markdown the plugin supports, to see if it has something not too painful
for reference documentation.
* Make those also for Groovy classes, and maybe Kotlin.
* Find a way to navigate the link from the reference to the class.
* Find a way to make references to individual (test) methods.
* Try "host language in string literals" hook for Rest plugin?

# Notes

Logs for the launched IDE are in `...\launch-url-from-string\build\idea-sandbox\system\log`
and _not_ in the debugger console.

Upgrading to 2021.1 (for the launched IDE) needs JDK 11.

I discovered that, at least in 2021.1, the "Tools" menu in the launched IDE
has a tool to inspect the PSI tree for the current document -- somewhat handy!
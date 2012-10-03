package org.scalaide.play2.templateeditor

import scala.tools.eclipse.testsetup.TestProjectSetup
import org.junit._
import org.scalaide.play2.templateeditor.hyperlink.LocalTemplateHyperlinkComputer
import scala.tools.eclipse.ScalaWordFinder
import scala.tools.eclipse.testsetup.SDTTestUtils
import scala.tools.eclipse.util.OSGiUtils
import org.eclipse.core.runtime.Platform
import org.eclipse.jdt.internal.core.ClasspathEntry
import org.eclipse.jdt.core.JavaCore
import org.scalaide.play2.templateeditor.hyperlink.TemplateDeclarationHyperlinkDetector
import org.mockito.Mockito._
import org.mockito.Matchers._
import org.eclipse.ui.texteditor.ITextEditor
import org.eclipse.jface.text.Document
import org.eclipse.ui.texteditor.IDocumentProvider
import org.scalaide.play2.templateeditor.lexical.TemplateDocumentPartitioner

object HyperlinkingTest extends TestProjectSetup("sample-project", srcRoot = "/%s/app", bundleName = "org.scala-ide.play2.tests") {
  /** Add the play libraries to the project classpath */
  @BeforeClass
  def setupClasspath() {
    val bnd = Platform.getBundle("org.scala-ide.play2")
    for (p <- OSGiUtils.allPathsInBundle(bnd, "target/lib", "*.jar")) {
      SDTTestUtils.addToClasspath(project, JavaCore.newLibraryEntry(p, null, null))
    }
  }
}

class HyperlinkingTest {
  import HyperlinkingTest._

  @Test
  def localHyperlink() {
    val unit = TemplateCompilationUnit.fromFile(project.underlying.getFile("app/views/index.scala.html"))
    unit.askReload()

    val localDetector = new LocalTemplateHyperlinkComputer
    val content = unit.getTemplateContents
    val wordRegion = ScalaWordFinder.findWord(content, 85) // `message`
    println("hyperlinking " + content.slice(wordRegion.getOffset(), wordRegion.getOffset + wordRegion.getLength()))
    val links = localDetector.findHyperlinks(unit, wordRegion)
    println(links.toList)
    Assert.assertTrue("No hyperlinks found", links.length > 0)
  }

  
  @Test
  def libraryHyperlink() {
    val unit = TemplateCompilationUnit.fromFile(project.underlying.getFile("app/views/index.scala.html"))
    unit.askReload()

    val localDetector = new TemplateDeclarationHyperlinkDetector
    val content = unit.getTemplateContents
    val wordRegion = ScalaWordFinder.findWord(content, 94) // `.length()`
    val doc = new Document
    unit.connect(doc)
    val partitioner = new TemplateDocumentPartitioner(true)
    partitioner.connect(doc)
    doc.setDocumentPartitioner(partitioner)

    
    val docProvider = mock(classOf[IDocumentProvider])
    when(docProvider.getDocument(any)).thenReturn(doc)
    
    val editor= mock(classOf[ITextEditor])
    when(editor.getEditorInput()).thenReturn(null)
    when(editor.getDocumentProvider()).thenReturn(docProvider)
    
    println("hyperlinking " + content.slice(wordRegion.getOffset(), wordRegion.getOffset + wordRegion.getLength()))
    val links = localDetector.runDetectionStrategy(unit, editor, wordRegion)
    println(links.toList)
    Assert.assertTrue("No hyperlinks found", links.length > 0)
  }

}
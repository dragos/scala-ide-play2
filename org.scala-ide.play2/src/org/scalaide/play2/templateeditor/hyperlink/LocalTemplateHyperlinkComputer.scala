package org.scalaide.play2.templateeditor.hyperlink

import org.eclipse.jface.text.hyperlink.AbstractHyperlinkDetector
import scala.tools.eclipse.util.EditorUtils
import org.eclipse.jface.text.hyperlink.IHyperlink
import org.eclipse.jface.text.IRegion
import org.scalaide.play2.templateeditor._
import org.scalaide.play2.templateeditor.lexical.TemplatePartitions
import scala.tools.eclipse.ScalaWordFinder
import scala.tools.eclipse.hyperlink.text.Hyperlink
import org.eclipse.jface.text.ITextViewer
import org.eclipse.ui.texteditor.ITextEditor
import org.scalaide.play2.templateeditor.compiler.PositionHelper

/** A hyperlink detector that only looks for local definitions.
 *
 *  Template source files are not handled properly by the Scala IDE default hyperlink detectors because they
 *  have different filenames, and `LocateSymbol` wouldn't be able to find the template compilation unit.
 *
 *  This detector only handles symbols that are defined in the *same* compilation unit.
 */
class LocalTemplateHyperlinkComputer extends AbstractHyperlinkDetector {

  final override def detectHyperlinks(viewer: ITextViewer, currentSelection: IRegion, canShowMultipleHyperlinks: Boolean): Array[IHyperlink] = {
    val textEditor = getAdapter(classOf[ITextEditor]).asInstanceOf[ITextEditor]
    detectHyperlinks(textEditor, currentSelection, canShowMultipleHyperlinks)
  }

  final def detectHyperlinks(textEditor: ITextEditor, currentSelection: IRegion, canShowMultipleHyperlinks: Boolean): Array[IHyperlink] = {
    if (textEditor == null) null // can be null if generated through ScalaPreviewerFactory
    else {
      val input = textEditor.getEditorInput
      val doc = textEditor.getDocumentProvider.getDocument(input)
      if (!doc.getContentType(currentSelection.getOffset()).equals(TemplatePartitions.TEMPLATE_SCALA)
          || (doc.getChar(currentSelection.getOffset()) == '.')) // otherwise it will generate an error
        return null

      EditorUtils.getEditorCompilationUnit(textEditor) match {
        case Some(scu: TemplateCompilationUnit) =>

          val wordRegion = ScalaWordFinder.findWord(doc.get, currentSelection.getOffset).asInstanceOf[IRegion]
          findHyperlinks(scu, wordRegion) match {
            // I know you will be tempted to remove this, but don't do it, JDT expects null when no hyperlinks are found.
            case Nil => null
            case links =>
              if (canShowMultipleHyperlinks) links.toArray
              else Array(links.head)
          }

        case _ => null
      }
    }
  }

  def findHyperlinks(icu: TemplateCompilationUnit, wordRegion: IRegion): List[IHyperlink] = {
    val mappedRegion = icu.mapTemplateToScalaRegion(wordRegion)
    icu.withSourceFile { (source, compiler) =>
      import compiler._
      def localSymbol(sym: compiler.Symbol): Boolean = (
        (sym ne null) &&
        (sym ne NoSymbol) &&
        sym.pos.isDefined &&
        sym.pos.source == source)

      val pos = compiler.rangePos(source, mappedRegion.getOffset(), mappedRegion.getOffset(), mappedRegion.getOffset() + mappedRegion.getLength())
      val response = new Response[Tree]
      compiler.askTypeAt(pos, response)
      response.get match {
        case Left(tree: Tree) if localSymbol(tree.symbol) =>
          val sym = tree.symbol
          val offset = icu.templateOffset(sym.pos.startOrPoint)
          val hyper = Hyperlink.withText(sym.name.toString)(icu, offset, sym.name.length, sym.kindString + sym.nameString, wordRegion)
          List(hyper)
        case _ => Nil
      }
    }(Nil)
  }
}
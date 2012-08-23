package org.scalaide.play2.templateeditor

import scala.tools.eclipse.lexical.ScalaCodeScanner
import scala.tools.eclipse.lexical.SingleTokenScanner
import org.eclipse.jdt.internal.ui.text.JavaColorManager
import org.eclipse.jface.preference.IPreferenceStore
import org.eclipse.jface.text.presentation.PresentationReconciler
import org.eclipse.jface.text.reconciler.IReconciler
import org.eclipse.jface.text.reconciler.MonoReconciler
import org.eclipse.jface.text.rules.DefaultDamagerRepairer
import org.eclipse.jface.text.rules.ITokenScanner
import org.eclipse.jface.text.source.ISourceViewer
import org.eclipse.jface.util.PropertyChangeEvent
import org.eclipse.ui.editors.text.TextSourceViewerConfiguration
import org.scalaide.play2.routeeditor.RouteDoubleClickStrategy
import org.scalaide.play2.templateeditor.reconciler.TemplateReconcilingStrategy
import org.scalaide.play2.templateeditor.scanners.HtmlTagScanner
import org.scalaide.play2.templateeditor.scanners.TemplatePartitions
import scalariform.ScalaVersions
import org.eclipse.jface.text.source.SourceViewerConfiguration
import org.eclipse.jface.text.source.IAnnotationHover
import org.eclipse.jface.text.source.DefaultAnnotationHover
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector
import scala.tools.eclipse.hyperlink.text.detector.DeclarationHyperlinkDetector
import org.scalaide.play2.templateeditor.hyperlink.TemplateDeclarationHyperlinkDetector

class TemplateConfiguration(prefStore: IPreferenceStore, templateEditor: TemplateEditor) extends SourceViewerConfiguration {

  val colorManager = new JavaColorManager()
  private val templateDoubleClickStrategy: RouteDoubleClickStrategy =
    new RouteDoubleClickStrategy()

  private val plainScanner: SingleTokenScanner = {
    val result = new SingleTokenScanner(TemplateSyntaxClasses.PLAIN, colorManager, prefStore)
    result
  }
  private val scalaScanner: ScalaCodeScanner = {
    val result = new ScalaCodeScanner(colorManager, prefStore, ScalaVersions.DEFAULT)
    result
  }
  private val commentScanner: SingleTokenScanner = {
    val result = new SingleTokenScanner(TemplateSyntaxClasses.COMMENT, colorManager, prefStore)
    result
  }
  private val tagScanner: HtmlTagScanner = {
    val result = new HtmlTagScanner(colorManager, prefStore)
    result
  }

  override def getDoubleClickStrategy(sourceViewer: ISourceViewer, contentType: String) = {
    templateDoubleClickStrategy
  }

  override def getConfiguredContentTypes(sourceViewer: ISourceViewer) = {
    TemplatePartitions.getTypes()
  }
  
  override def getAnnotationHover(viewer: ISourceViewer): IAnnotationHover = {
    new DefaultAnnotationHover(true) 

  }

  //  override def getHyperlinkDetectors(sourceViewer: ISourceViewer) = { TODO
  //    Array(new RouteHyperlinkDetector(routeEditor));
  //  }

  override def getPresentationReconciler(
    sourceViewer: ISourceViewer) = {
    val reconciler = super.getPresentationReconciler(sourceViewer).asInstanceOf[PresentationReconciler]
    def handlePartition(scan: ITokenScanner, token: String) = {

      val dr = new DefaultDamagerRepairer(scan);
      reconciler.setDamager(dr, token);
      reconciler.setRepairer(dr, token);
    }
    handlePartition(plainScanner, TemplatePartitions.TEMPLATE_PLAIN)
    handlePartition(scalaScanner, TemplatePartitions.TEMPLATE_SCALA)
    handlePartition(commentScanner, TemplatePartitions.TEMPLATE_COMMENT)
    handlePartition(tagScanner, TemplatePartitions.TEMPLATE_TAG)

    reconciler
  }

  lazy val strategy = new TemplateReconcilingStrategy(templateEditor)

  override def getReconciler(sourceViewer: ISourceViewer): IReconciler = {
    val reconciler = new MonoReconciler(strategy, /*isIncremental = */ false)
    reconciler.install(sourceViewer)
    reconciler
  }
  
  override def getHyperlinkDetectors(sv: ISourceViewer): Array[IHyperlinkDetector] = {
    val detector = TemplateDeclarationHyperlinkDetector()
    detector.setContext(templateEditor)
    Array(detector)
  }

  def handlePropertyChangeEvent(event: PropertyChangeEvent) {
    plainScanner.adaptToPreferenceChange(event)
    scalaScanner.adaptToPreferenceChange(event)
    commentScanner.adaptToPreferenceChange(event)
  }

}
package com.example.demo.image;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.Artifact;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowNode;
import org.flowable.bpmn.model.GraphicInfo;
import org.flowable.bpmn.model.Lane;
import org.flowable.bpmn.model.Pool;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.SubProcess;
import org.flowable.image.exception.FlowableImageException;
import org.flowable.image.impl.DefaultProcessDiagramCanvas;
import org.flowable.image.impl.DefaultProcessDiagramGenerator;

/**
 * Extended process diagram generator
 * 
 * @author d.farafonov
 *
 */
public class CustomProcessDiagramGenerator extends DefaultProcessDiagramGenerator {

	private static final Color DIAGRAM_HEADER_BG_COLOR = new Color(249, 249, 249);
	private static final Color DIAGRAM_HEADER_TEXT_COLOR = new Color(110, 110, 110);

	private static final Font DIAGRAM_HEADER_FONT = new Font("Arial", Font.PLAIN, 12);

	/**
	 * Generates a diagram of the given process definition, using the diagram interchange information of the process.
	 * 
	 * @see org.flowable.image.impl.DefaultProcessDiagramGenerator#generateDiagram(org.flowable.bpmn.model.BpmnModel,
	 *      java.lang.String, java.util.List, java.util.List, java.lang.String, java.lang.String, java.lang.String,
	 *      java.lang.ClassLoader, double, boolean)
	 */
	@Override
	public InputStream generateDiagram(BpmnModel bpmnModel, String imageType, List<String> highLightedActivities,
			List<String> highLightedFlows, String activityFontName, String labelFontName, String annotationFontName,
			ClassLoader customClassLoader, double scaleFactor, boolean drawSequenceFlowNameWithNoLabelDI) {

		String processDefinitionId = bpmnModel.getDefinitionsAttributeValue("meridian", "processDefinitionId");

		DefaultProcessDiagramCanvas processDiagramCanvas = generateProcessDiagram(bpmnModel, imageType,
				highLightedActivities, highLightedFlows, activityFontName, labelFontName, annotationFontName,
				customClassLoader, scaleFactor, drawSequenceFlowNameWithNoLabelDI);

		if (StringUtils.isEmpty(processDefinitionId)) {
			return processDiagramCanvas.generateImage(imageType);
		}

		BufferedImage processBufferedImage = processDiagramCanvas.generateBufferedImage(imageType);

		int canvasWidth = processBufferedImage.getWidth();
		int metaHeight = 30;

		BufferedImage metaImage = new BufferedImage(canvasWidth, metaHeight, processBufferedImage.getType());

		Graphics2D g = metaImage.createGraphics();
		if (metaImage.getType() != BufferedImage.TYPE_INT_ARGB) {
			g.clearRect(0, 0, canvasWidth, metaHeight);
		}

		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setPaint(Color.black);

		g.setFont(DIAGRAM_HEADER_FONT);
		g.setColor(DIAGRAM_HEADER_BG_COLOR);
		g.fillRect(0, 0, canvasWidth - 1, metaHeight - 1);
		g.setColor(DIAGRAM_HEADER_TEXT_COLOR);
		g.drawString(processDefinitionId, 10, 19);

		BufferedImage image = appendImage(metaImage, processBufferedImage);

		return writeImage(image, imageType);
	}

	public BufferedImage appendImage(BufferedImage img1, BufferedImage img2) {
		int width = Math.max(img1.getWidth(), img2.getWidth());
		int height = img1.getHeight() + img2.getHeight();
		BufferedImage resultImage = new BufferedImage(width, height, img1.getType());
		Graphics2D g = resultImage.createGraphics();
		if (img1.getType() != BufferedImage.TYPE_INT_ARGB) {
			g.clearRect(0, 0, width, height);
		}
		g.drawImage(img1, null, 0, 0);
		g.drawImage(img2, null, 0, img1.getHeight());
		g.dispose();
		return resultImage;
	}

	/**
	 * Generates an image of what currently is drawn on the canvas.
	 * 
	 * Throws an {@link FlowableImageException} when {@link #close()} is already called.
	 */
	public InputStream writeImage(BufferedImage img, String imageType) {

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			ImageIO.write(img, imageType, out);

		} catch (IOException e) {
			throw new FlowableImageException("Error while generating process image", e);
		} finally {
			try {
				if (out != null) {
					out.close();
				}
			} catch (IOException ignore) {
				// Exception is silently ignored
			}
		}
		return new ByteArrayInputStream(out.toByteArray());
	}

	@Override
	protected DefaultProcessDiagramCanvas generateProcessDiagram(BpmnModel bpmnModel, String imageType,
			List<String> highLightedActivities, List<String> highLightedFlows, String activityFontName,
			String labelFontName, String annotationFontName, ClassLoader customClassLoader, double scaleFactor,
			boolean drawSequenceFlowNameWithNoLabelDI) {

		prepareBpmnModel(bpmnModel);

		DefaultProcessDiagramCanvas processDiagramCanvas = initProcessDiagramCanvas(bpmnModel, imageType,
				activityFontName, labelFontName, annotationFontName, customClassLoader);

		// Draw pool shape, if process is participant in collaboration
		for (Pool pool : bpmnModel.getPools()) {
			GraphicInfo graphicInfo = bpmnModel.getGraphicInfo(pool.getId());
			processDiagramCanvas.drawPoolOrLane(pool.getName(), graphicInfo, scaleFactor);
		}

		// Draw lanes
		for (Process process : bpmnModel.getProcesses()) {
			for (Lane lane : process.getLanes()) {
				GraphicInfo graphicInfo = bpmnModel.getGraphicInfo(lane.getId());
				processDiagramCanvas.drawPoolOrLane(lane.getName(), graphicInfo, scaleFactor);
			}
		}

		// Draw activities and their sequence-flows
		for (Process process : bpmnModel.getProcesses()) {
			for (FlowNode flowNode : process.findFlowElementsOfType(FlowNode.class)) {
				if (!isPartOfCollapsedSubProcess(flowNode, bpmnModel)) {
					drawActivity(processDiagramCanvas, bpmnModel, flowNode, highLightedActivities, highLightedFlows,
							scaleFactor, drawSequenceFlowNameWithNoLabelDI);
				}
			}
		}

		// Draw artifacts
		for (Process process : bpmnModel.getProcesses()) {

			for (Artifact artifact : process.getArtifacts()) {
				drawArtifact(processDiagramCanvas, bpmnModel, artifact);
			}

			List<SubProcess> subProcesses = process.findFlowElementsOfType(SubProcess.class, true);
			if (subProcesses != null) {
				for (SubProcess subProcess : subProcesses) {

					GraphicInfo graphicInfo = bpmnModel.getGraphicInfo(subProcess.getId());
					if (graphicInfo != null && graphicInfo.getExpanded() != null && !graphicInfo.getExpanded()) {
						continue;
					}

					if (!isPartOfCollapsedSubProcess(subProcess, bpmnModel)) {
						for (Artifact subProcessArtifact : subProcess.getArtifacts()) {
							drawArtifact(processDiagramCanvas, bpmnModel, subProcessArtifact);
						}
					}
				}
			}
		}

		return processDiagramCanvas;
	}

	protected static DefaultProcessDiagramCanvas initProcessDiagramCanvas(BpmnModel bpmnModel, String imageType,
			String activityFontName, String labelFontName, String annotationFontName, ClassLoader customClassLoader) {
		DefaultProcessDiagramCanvas canvas = DefaultProcessDiagramGenerator.initProcessDiagramCanvas(bpmnModel,
				imageType, activityFontName, labelFontName, annotationFontName, customClassLoader);
		CustomProcessDiagramCanvas meridianProcessDiagramCanvas = CustomProcessDiagramCanvas.create(canvas,
				imageType);
		return meridianProcessDiagramCanvas;
	}
}

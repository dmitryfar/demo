package com.example.demo.image;

import java.awt.Color;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.lang.reflect.Field;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.util.ArrayList;
import java.util.List;

import org.flowable.image.impl.DefaultProcessDiagramCanvas;

/**
 * Customized Process Diagram Canvas to correct new lines in Annotations
 * 
 * @author d.farafonov
 *
 */
public class CustomProcessDiagramCanvas extends DefaultProcessDiagramCanvas {

	public CustomProcessDiagramCanvas(int width, int height, int minX, int minY, String imageType) {
		super(width, height, minX, minY, imageType);
	}

	public CustomProcessDiagramCanvas(int width, int height, int minX, int minY, String imageType,
			String activityFontName, String labelFontName, String annotationFontName, ClassLoader customClassLoader) {
		super(width, height, minX, minY, imageType, activityFontName, labelFontName, annotationFontName,
				customClassLoader);
	}

	public static CustomProcessDiagramCanvas create(DefaultProcessDiagramCanvas canvas, String imageType) {
		int canvasWidth = getFieldValue(canvas, "canvasWidth");
		int canvasHeight = getFieldValue(canvas, "canvasHeight");
		int minX = getFieldValue(canvas, "minX");
		int minY = getFieldValue(canvas, "minY");
		String activityFontName = getFieldValue(canvas, "activityFontName");
		String labelFontName = getFieldValue(canvas, "labelFontName");
		String annotationFontName = getFieldValue(canvas, "annotationFontName");
		ClassLoader customClassLoader = getFieldValue(canvas, "customClassLoader");
		return new CustomProcessDiagramCanvas(canvasWidth, canvasHeight, minX, minY, imageType, activityFontName,
				labelFontName, annotationFontName, customClassLoader);
	}

	@SuppressWarnings("unchecked")
	private static <T> T getFieldValue(DefaultProcessDiagramCanvas canvas, String fieldName) {
		try {
			Field field = canvas.getClass().getDeclaredField(fieldName);
			field.setAccessible(true);
			return (T) field.get(canvas);
		} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
			// FIXME:
		    throw new RuntimeException(e);
		}
	}

	@Override
	protected void drawMultilineText(String text, int x, int y, int boxWidth, int boxHeight, boolean centered) {
		// Create an attributed string based in input text
		AttributedString attributedString = new AttributedString(text);
		attributedString.addAttribute(TextAttribute.FONT, g.getFont());
		attributedString.addAttribute(TextAttribute.FOREGROUND, Color.black);

		AttributedCharacterIterator characterIterator = attributedString.getIterator();

		int currentHeight = 0;
		// Prepare a list of lines of text we'll be drawing
		List<TextLayout> layouts = new ArrayList<>();
		String lastLine = null;

		LineBreakMeasurer measurer = new LineBreakMeasurer(characterIterator, g.getFontRenderContext());

		TextLayout layout = null;
		while (measurer.getPosition() < characterIterator.getEndIndex() && currentHeight <= boxHeight) {

			int previousPosition = measurer.getPosition();

			// set limit for new lines in text
			int next = measurer.nextOffset(boxWidth);
			int limit = next;
			int newLinePosition = text.indexOf('\n', previousPosition);
			if (next > (newLinePosition - previousPosition) && newLinePosition != -1) {
				limit = newLinePosition - previousPosition + 1;
			}

			// Request next layout
			layout = measurer.nextLayout(boxWidth, previousPosition + limit, false);

			int height = ((Float) (layout.getDescent() + layout.getAscent() + layout.getLeading())).intValue();

			if (currentHeight + height > boxHeight) {
				// The line we're about to add should NOT be added anymore, append three dots to previous one instead
				// to indicate more text is truncated
				if (!layouts.isEmpty() && lastLine != null) {
					layouts.remove(layouts.size() - 1);

					if (lastLine.length() >= 4) {
						lastLine = lastLine.substring(0, lastLine.length() - 4) + "...";
					}
					layouts.add(new TextLayout(lastLine, g.getFont(), g.getFontRenderContext()));
				}
				break;
			} else {
				layouts.add(layout);
				lastLine = text.substring(previousPosition, previousPosition + layout.getCharacterCount());
				currentHeight += height;
			}
		}

		int currentY = y + (centered ? ((boxHeight - currentHeight) / 2) : 0);
		int currentX = 0;

		// Actually draw the lines
		for (TextLayout textLayout : layouts) {

			currentY += textLayout.getAscent();
			currentX = x + (centered ? ((boxWidth - ((Double) textLayout.getBounds().getWidth()).intValue()) / 2) : 0);

			textLayout.draw(g, currentX, currentY);
			currentY += textLayout.getDescent() + textLayout.getLeading();
		}

	}
}

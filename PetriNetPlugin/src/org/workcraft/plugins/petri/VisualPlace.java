/*
*
* Copyright 2008,2009 Newcastle University
*
* This file is part of Workcraft.
*
* Workcraft is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* Workcraft is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with Workcraft.  If not, see <http://www.gnu.org/licenses/>.
*
*/

package org.workcraft.plugins.petri;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.event.KeyEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import org.workcraft.annotations.DisplayName;
import org.workcraft.annotations.Hotkey;
import org.workcraft.annotations.SVGIcon;
import org.workcraft.dom.visual.DrawRequest;
import org.workcraft.dom.visual.VisualComponent;
import org.workcraft.gui.Coloriser;
import org.workcraft.gui.propertyeditor.PropertyDeclaration;
import org.workcraft.plugins.shared.CommonVisualSettings;
import org.workcraft.serialisation.xml.NoAutoSerialisation;

@DisplayName("Place")
@Hotkey(KeyEvent.VK_P)
@SVGIcon("images/icons/svg/place.svg")
public class VisualPlace extends VisualComponent {

	protected static double singleTokenSize = CommonVisualSettings.getSize() / 1.9;
	protected static double multipleTokenSeparation = CommonVisualSettings.getStrokeWidth() / 8;
	private Color tokenColor = CommonVisualSettings.getForegroundColor();

	public VisualPlace(Place place) {
		super(place);
		addPropertyDeclarations();
	}

	private void addPropertyDeclarations() {
		addPropertyDeclaration(new PropertyDeclaration (this, "Tokens", "getTokens", "setTokens", int.class));
		addPropertyDeclaration(new PropertyDeclaration (this, "Token color", "getTokenColor", "setTokenColor", Color.class));
		addPropertyDeclaration(new PropertyDeclaration (this, "Capacity", "getCapacity", "setCapacity", int.class));
	}

	public static void drawTokens(int tokens, double singleTokenSize, double multipleTokenSeparation,
			double diameter, double borderWidth, Color tokenColor,	Graphics2D g) {
		Shape shape;
		if (tokens == 1)
		{
			shape = new Ellipse2D.Double(
					-singleTokenSize / 2,
					-singleTokenSize / 2,
					singleTokenSize,
					singleTokenSize);

			g.setColor(tokenColor);
			g.fill(shape);
		} else {
			if (tokens > 1 && tokens < 8) {
				double al = Math.PI / tokens;
				if (tokens == 7) al = Math.PI / 6;
				double r = (diameter / 2 - borderWidth - multipleTokenSeparation) / (1 + 1 / Math.sin(al));
				double R = r / Math.sin(al);
				r -= multipleTokenSeparation;
				for(int i = 0; i < tokens; i++) 	{
					if (i == 6) {
						shape = new Ellipse2D.Double( -r, -r, r * 2, r * 2);
					} else {
						shape = new Ellipse2D.Double(
								-R * Math.sin(i * al * 2) - r,
								-R * Math.cos(i * al * 2) - r,
								r * 2,
								r * 2);
					}
					g.setColor(tokenColor);
					g.fill(shape);
				}
			} else if (tokens > 7)	{
				String token_str = Integer.toString(tokens);
				Font superFont = g.getFont().deriveFont((float)CommonVisualSettings.getSize()/2);
				Rectangle2D rect = superFont.getStringBounds(token_str, g.getFontRenderContext());
				g.setFont(superFont);
				g.setColor(tokenColor);
				g.drawString(token_str, (float)(-rect.getCenterX()), (float)(-rect.getCenterY()));
			}
		}
	}


	@Override
	public void draw(DrawRequest r)	{
		Graphics2D g = r.getGraphics();
		Color colorisation = r.getDecoration().getColorisation();
		double xy = -size / 2 + strokeWidth / 2;
		double wh = size - strokeWidth;
		Shape shape = new Ellipse2D.Double(xy, xy, wh, wh);
		g.setColor(Coloriser.colorise(getFillColor(), colorisation));
		g.fill(shape);
		g.setColor(Coloriser.colorise(getForegroundColor(), colorisation));
		g.setStroke(new BasicStroke((float)strokeWidth));
		g.draw(shape);

		Place p = (Place)getReferencedComponent();
		if (p.getCapacity() != 1) {
			String capacity_str = Integer.toString(p.getCapacity());
			Font superFont = g.getFont().deriveFont((float)CommonVisualSettings.getSize()/2);
			Rectangle2D rect = superFont.getStringBounds(capacity_str, g.getFontRenderContext());
			g.setFont(superFont);
			g.setColor(tokenColor);
			g.drawString(capacity_str, (float)(size/3), (float)(size/3 + rect.getHeight()));
		}

		drawTokens(p.getTokens(), singleTokenSize, multipleTokenSeparation, size, strokeWidth,
				Coloriser.colorise(getTokenColor(), colorisation), g);

		drawLabelInLocalSpace(r);
	}

	@Override
	public boolean hitTestInLocalSpace(Point2D pointInLocalSpace) {
		return pointInLocalSpace.distanceSq(0, 0) < size * size / 4;
	}


	public Place getPlace() {
		return (Place)getReferencedComponent();
	}

	@NoAutoSerialisation
	public int getTokens() {
		return getPlace().getTokens();
	}

	@NoAutoSerialisation
	public void setTokens(int tokens) {
		getPlace().setTokens(tokens);
	}

	@NoAutoSerialisation
	public int getCapacity() {
		return getPlace().getCapacity();
	}

	@NoAutoSerialisation
	public void setCapacity(int capacity) {
		getPlace().setCapacity(capacity);
	}

	public Place getReferencedPlace() {
		return (Place)getReferencedComponent();
	}

	public Color getTokenColor() {
		return tokenColor;
	}

	public void setTokenColor(Color tokenColor) {
		this.tokenColor = tokenColor;
	}
}

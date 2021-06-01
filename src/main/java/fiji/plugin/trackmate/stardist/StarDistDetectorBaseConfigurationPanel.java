/*-
 * #%L
 * Fiji distribution of ImageJ for the life sciences.
 * %%
 * Copyright (C) 2020 - 2021 The Institut Pasteur.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
package fiji.plugin.trackmate.stardist;

import java.net.URL;

import javax.swing.ImageIcon;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.detection.SpotDetectorFactory;
import fiji.plugin.trackmate.gui.components.ConfigurationPanel;

public abstract class StarDistDetectorBaseConfigurationPanel extends ConfigurationPanel
{

	private static final long serialVersionUID = 1L;

	protected static final ImageIcon ICON = new ImageIcon( getResource( "images/TrackMateStarDist-logo100x100.png" ) );

	protected final Settings settings;

	protected final Model model;

	public StarDistDetectorBaseConfigurationPanel( final Settings settings, final Model model )
	{
		this.settings = settings;
		this.model = model;
	}

	protected abstract SpotDetectorFactory< ? > getDetectorFactory();

	protected static URL getResource( final String name )
	{
		return StarDistDetectorFactory.class.getClassLoader().getResource( name );
	}
}

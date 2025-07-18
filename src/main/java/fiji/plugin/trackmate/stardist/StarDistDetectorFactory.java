/*-
 * #%L
 * TrackMate: your buddy for everyday tracking.
 * %%
 * Copyright (C) 2020 - 2023 TrackMate developers.
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

import static fiji.plugin.trackmate.detection.DetectorKeys.DEFAULT_TARGET_CHANNEL;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_TARGET_CHANNEL;

import java.util.HashMap;
import java.util.Map;

import javax.swing.ImageIcon;

import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.detection.SpotDetector;
import fiji.plugin.trackmate.detection.SpotDetectorFactory;
import fiji.plugin.trackmate.gui.GuiUtils;
import fiji.plugin.trackmate.gui.components.ConfigurationPanel;
import fiji.plugin.trackmate.util.TMUtils;
import net.imagej.ImgPlus;
import net.imglib2.Interval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

@Plugin( type = SpotDetectorFactory.class )
public class StarDistDetectorFactory< T extends RealType< T > & NativeType< T > > implements SpotDetectorFactory< T >
{

	/** A string key identifying this factory. */
	public static final String DETECTOR_KEY = "STARDIST_DETECTOR";

	/** The pretty name of the target detector. */
	public static final String NAME = "StarDist detector";

	/** An html information text. */
	public static final String INFO_TEXT = "<html>"
			+ "This detector relies on StarDist to detect the nuclei of cells."
			+ "<p>"
			+ "It only works for 2D images."
			+ "And for this detector to work, the update sites StartDist and CSBDeep "
			+ "must be activated in your Fiji installation."
			+ "<p>"
			+ "The StarDist versatile model for fluorescent nuclei is used, "
			+ "along with the default parameters. Spots are created from the nuclei "
			+ "segmentation results, with a radius set from the cell area, and a "
			+ "quality equal to the maximal value of the probability image in the nuclei. "
			+ "<p>"
			+ "Documentation for this module "
			+ "<a href=\"https://imagej.net/plugins/trackmate/trackmate-stardist\">on the ImageJ Wiki</a>."
			+ "<p>"
			+ "If you use this detector for your work, please be so kind as to "
			+ "also cite the StarDist IJ paper: "
			+ "<a href=\"http://doi.org/10.1007/978-3-030-00934-2_30\">"
			+ "Uwe Schmidt, Martin Weigert, Coleman Broaddus, and Gene Myers. "
			+ "Cell Detection with Star-convex Polygons. MICCAI, Granada, Spain, September 2018.</a>"
			+ "</html>";

	public static final String DOC_URL = "https://imagej.net/plugins/trackmate/trackmate-stardist";

	public static final ImageIcon ICON = new ImageIcon( GuiUtils.getResource( "images/TrackMateStarDist-logo100x100.png", StarDistDetectorFactory.class ) );

	@Override
	public SpotDetector< T > getDetector( final ImgPlus< T > img, final Map< String, Object > settings, final Interval interval, final int frame )
	{
		final StarDistRunner starDistRunner = new StarDistRunner();
		if ( !starDistRunner.initialize() )
		{
			System.err.println( starDistRunner.getErrorMessage() );
			return null;
		}

		final double[] calibration = TMUtils.getSpatialCalibration( img );
		final int channel = ( Integer ) settings.get( KEY_TARGET_CHANNEL ) - 1;
		final ImgPlus< T > imFrame = TMUtils.hyperSlice( img, channel, frame );
		final StarDistDetector< T > detector = new StarDistDetector<>( starDistRunner, imFrame, interval, calibration );
		return detector;
	}

	@Override
	public boolean forbidMultithreading()
	{
		/*
		 * We want to run one frame after another, because the inference for one
		 * frame takes all the resources anyway.
		 */
		return true;
	}

	@Override
	public boolean has2Dsegmentation()
	{
		return true;
	}

	@Override
	public ConfigurationPanel getDetectorConfigurationPanel( final Settings settings, final Model model )
	{
		return new StarDistDetectorConfigurationPanel( settings, model );
	}

	@Override
	public Map< String, Object > getDefaultSettings()
	{
		final Map< String, Object > settings = new HashMap<>();
		settings.put( KEY_TARGET_CHANNEL, DEFAULT_TARGET_CHANNEL );
		return settings;
	}

	@Override
	public String getInfoText()
	{
		return INFO_TEXT;
	}

	@Override
	public ImageIcon getIcon()
	{
		return ICON;
	}

	@Override
	public String getUrl()
	{
		return DOC_URL;
	}

	@Override
	public String getKey()
	{
		return DETECTOR_KEY;
	}

	@Override
	public String getName()
	{
		return NAME;
	}
}

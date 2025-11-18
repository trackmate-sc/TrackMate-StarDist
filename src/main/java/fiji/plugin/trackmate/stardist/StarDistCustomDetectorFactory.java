/*-
 * #%L
 * TrackMate: your buddy for everyday tracking.
 * %%
 * Copyright (C) 2020 - 2025 TrackMate developers.
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

import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_TARGET_CHANNEL;

import java.io.File;
import java.util.Map;

import org.scijava.Priority;
import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.detection.SpotDetector;
import fiji.plugin.trackmate.detection.SpotDetectorFactory;
import fiji.plugin.trackmate.gui.components.ConfigurationPanel;
import fiji.plugin.trackmate.util.TMUtils;
import net.imagej.ImgPlus;
import net.imglib2.Interval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

@Plugin( type = SpotDetectorFactory.class, priority = Priority.NORMAL - 1. )
public class StarDistCustomDetectorFactory< T extends RealType< T > & NativeType< T > > extends StarDistDetectorFactory< T >
{

	/** The key to the parameter that stores the model file path. */
	public static final String KEY_MODEL_FILEPATH = "MODEL_FILEPATH";

	/**
	 * The key to the parameter that stores the probability / score threshold.
	 * Values are {@link Double}s from 0 to 1.
	 */
	public static final String KEY_SCORE_THRESHOLD = "SCORE_THRESHOLD";

	public static final Double DEFAULT_SCORE_THRESHOLD = Double.valueOf( 0.41 );

	/**
	 * The key to the parameter that stores the overlap threshold. Values are
	 * {@link Double}s from 0 to 1.
	 */
	public static final String KEY_OVERLAP_THRESHOLD = "OVERLAP_THRESHOLD";

	public static final Double DEFAULT_OVERLAP_THRESHOLD = Double.valueOf( 0.5 );

	/** A string key identifying this factory. */
	public static final String DETECTOR_KEY = "STARDIST_CUSTOM_DETECTOR";

	/** The pretty name of the target detector. */
	public static final String NAME = "StarDist detector custom model";

	/** An html information text. */
	public static final String INFO_TEXT = "<html>"
			+ "This detector relies on StarDist to detect cells, using a custom model stored in a file."
			+ "<p>"
			+ "It only works for 2D images."
			+ "And for this detector to work, the update sites StartDist and CSBDeep "
			+ "must be activated in your Fiji installation."
			+ "<p>"
			+ "Documentation for this module "
			+ "<a href=\"https://imagej.net/plugins/trackmate/trackmate-stardist\">on the ImageJ Wiki</a>."
			+ "<p>"
			+ "If you use this detector for your work, please be so kind as to "
			+ "also cite the StarDist IJ paper: <a href=\"http://doi.org/10.1007/978-3-030-00934-2_30\">"
			+ "Uwe Schmidt, Martin Weigert, Coleman Broaddus, and Gene Myers. "
			+ "Cell Detection with Star-convex Polygons. MICCAI, Granada, Spain, September 2018.</a>"
			+ "</html>";

	@Override
	public SpotDetector< T > getDetector( final ImgPlus< T > img, final Map< String, Object > settings, final Interval interval, final int frame )
	{
		final String modelFilePath = ( String ) settings.get( KEY_MODEL_FILEPATH );
		final File modelFile = new File( modelFilePath );
		final double probThresh = ( double ) settings.get( KEY_SCORE_THRESHOLD );
		final double nmsThresh = ( double ) settings.get( KEY_OVERLAP_THRESHOLD );
		final StarDistRunnerCustom starDistRunner = new StarDistRunnerCustom( modelFile, probThresh, nmsThresh );
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
	public ConfigurationPanel getDetectorConfigurationPanel( final Settings settings, final Model model )
	{
		return new StarDistCustomDetectorConfigurationPanel( settings, model );
	}

	@Override
	public Map< String, Object > getDefaultSettings()
	{
		final Map< String, Object > dfs = super.getDefaultSettings();
		dfs.put( KEY_MODEL_FILEPATH, "" );
		dfs.put( KEY_SCORE_THRESHOLD, DEFAULT_SCORE_THRESHOLD );
		dfs.put( KEY_OVERLAP_THRESHOLD, DEFAULT_OVERLAP_THRESHOLD );
		return dfs;
	}

	@Override
	public String checkSettings( final Map< String, Object > settings )
	{
		final String errorMessage = super.checkSettings( settings );
		if ( null != errorMessage )
			return errorMessage;

		final String modelFilePath = ( String ) settings.get( KEY_MODEL_FILEPATH );
		if ( null == modelFilePath )
			return "Model file path is not set.";

		final File file = new File( modelFilePath );
		if ( !file.exists() )
			return "Model file " + modelFilePath + " does not exist.";

		if ( !file.canRead() )
			return "Model file " + modelFilePath + " exists but cannot be read.";

		return null;
	}

	@Override
	public String getInfoText()
	{
		return INFO_TEXT;
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

	@Override
	public boolean has2Dsegmentation()
	{
		return true;
	}
}

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

import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_TARGET_CHANNEL;
import static fiji.plugin.trackmate.io.IOUtils.readDoubleAttribute;
import static fiji.plugin.trackmate.io.IOUtils.readStringAttribute;
import static fiji.plugin.trackmate.io.IOUtils.writeAttribute;
import static fiji.plugin.trackmate.util.TMUtils.checkMapKeys;
import static fiji.plugin.trackmate.util.TMUtils.checkParameter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jdom2.Element;
import org.scijava.Priority;
import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.detection.SpotDetectorFactory;
import fiji.plugin.trackmate.gui.components.ConfigurationPanel;
import net.imagej.ImgPlus;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

@Plugin( type = SpotDetectorFactory.class, priority = Priority.NORMAL - 1. )
public class StarDistCustomDetectorFactory< T extends RealType< T > & NativeType< T > > extends StarDistDetectorFactory< T >
{

	/**
	 * The key to the parameter that stores the model file path.
	 */
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
	public boolean setTarget( final ImgPlus< T > img, final Map< String, Object > settings )
	{
		this.img = img;
		this.settings = settings;
		final boolean ok = checkSettings( settings );
		if ( !ok )
			return false;

		final String modelFilePath = ( String ) settings.get( KEY_MODEL_FILEPATH );
		final File modelFile = new File( modelFilePath );
		final double probThresh = ( double ) settings.get( KEY_SCORE_THRESHOLD );
		final double nmsThresh = ( double ) settings.get( KEY_OVERLAP_THRESHOLD );
		this.starDistRunner = new StarDistRunnerCustom( modelFile, probThresh, nmsThresh );
		if ( !starDistRunner.initialize() )
		{
			errorMessage = starDistRunner.getErrorMessage();
			return false;
		}
		return true;
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
	public boolean checkSettings( final Map< String, Object > settings )
	{
		boolean ok = true;
		final StringBuilder errorHolder = new StringBuilder();
		ok = ok & checkParameter( settings, KEY_TARGET_CHANNEL, Integer.class, errorHolder );
		ok = ok & checkParameter( settings, KEY_SCORE_THRESHOLD, Double.class, errorHolder );
		ok = ok & checkParameter( settings, KEY_OVERLAP_THRESHOLD, Double.class, errorHolder );
		ok = ok & checkParameter( settings, KEY_MODEL_FILEPATH, String.class, errorHolder );

		final List< String > mandatoryKeys = new ArrayList<>();
		mandatoryKeys.add( KEY_TARGET_CHANNEL );
		mandatoryKeys.add( KEY_SCORE_THRESHOLD );
		mandatoryKeys.add( KEY_OVERLAP_THRESHOLD );
		mandatoryKeys.add( KEY_MODEL_FILEPATH );
		ok = ok & checkMapKeys( settings, mandatoryKeys, null, errorHolder );

		if ( !ok )
			errorMessage = errorHolder.toString();

		final String modelFilePath = ( String ) settings.get( KEY_MODEL_FILEPATH );
		if ( null == modelFilePath )
		{
			errorMessage = "Model file path is not set.";
			return false;
		}
		final File file = new File( modelFilePath );
		if ( !file.exists() )
		{
			errorMessage = "Model file " + modelFilePath + " does not exist.";
			return false;
		}
		if ( !file.canRead() )
		{
			errorMessage = "Model file " + modelFilePath + " exists but cannot be read.";
			return false;
		}
		return true;
	}

	@Override
	public boolean marshall( final Map< String, Object > settings, final Element element )
	{
		if ( !super.marshall( settings, element ) )
			return false;

		final StringBuilder errorHolder = new StringBuilder();
		final boolean ok = writeAttribute( settings, element, KEY_MODEL_FILEPATH, String.class, errorHolder )
				&& writeAttribute( settings, element, KEY_SCORE_THRESHOLD, Double.class, errorHolder )
				&& writeAttribute( settings, element, KEY_OVERLAP_THRESHOLD, Double.class, errorHolder );

		if ( !ok )
			errorMessage = errorHolder.toString();

		return ok;
	}

	@Override
	public boolean unmarshall( final Element element, final Map< String, Object > settings )
	{
		settings.clear();
		if ( !super.unmarshall( element, settings ) )
			return false;

		final StringBuilder errorHolder = new StringBuilder();
		boolean ok = readStringAttribute( element, settings, KEY_MODEL_FILEPATH, errorHolder );
		ok = ok & readDoubleAttribute( element, settings, KEY_SCORE_THRESHOLD, errorHolder );
		ok = ok & readDoubleAttribute( element, settings, KEY_OVERLAP_THRESHOLD, errorHolder );

		if ( !ok )
		{
			errorMessage = errorHolder.toString();
			return false;
		}
		return checkSettings( settings );
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

	@Override
	public StarDistCustomDetectorFactory< T > copy()
	{
		return new StarDistCustomDetectorFactory<>();
	}
}

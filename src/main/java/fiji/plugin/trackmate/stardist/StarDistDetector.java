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

import java.awt.Polygon;
import java.util.ArrayList;
import java.util.List;

import de.csbdresden.stardist.Candidates;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotRoi;
import fiji.plugin.trackmate.detection.DetectionUtils;
import fiji.plugin.trackmate.detection.SpotDetector;
import ij.ImagePlus;
import ij.gui.PolygonRoi;
import ij.measure.Measurements;
import net.imglib2.Interval;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Pair;
import net.imglib2.view.Views;

public class StarDistDetector< T extends RealType< T > & NativeType< T > > implements SpotDetector< T >
{

	private final static String BASE_ERROR_MESSAGE = "StarDistDetector: ";

	protected final RandomAccessible< T > img;

	protected final Interval interval;

	protected final double[] calibration;

	protected final List< Spot > spots = new ArrayList<>();

	protected String baseErrorMessage;

	protected String errorMessage;

	protected long processingTime;

	protected final StarDistRunnerBase stardistRunner;

	public StarDistDetector(
			final StarDistRunnerBase stardistRunner,
			final RandomAccessible< T > img,
			final Interval interval,
			final double[] calibration )
	{
		this.stardistRunner = stardistRunner;
		this.img = img;
		this.interval = DetectionUtils.squeeze( interval );
		this.calibration = calibration;
		this.baseErrorMessage = BASE_ERROR_MESSAGE;
	}

	@Override
	public boolean checkInput()
	{
		if ( null == img )
		{
			errorMessage = baseErrorMessage + "Image is null.";
			return false;
		}
		if ( img.numDimensions() != 2 )
		{
			errorMessage = baseErrorMessage + "Image must be 2D, got " + img.numDimensions() + "D.";
			return false;
		}
		return true;
	}

	@Override
	public boolean process()
	{
		final long start = System.currentTimeMillis();
		spots.clear();

		// Properly set the image to process.
		final RandomAccessibleInterval< T > crop = Views.interval( img, interval );
		final long[] min = new long[ interval.numDimensions() ];
		interval.min( min );
		final RandomAccessibleInterval< T > input = Views.zeroMin( crop );

		// Launch StarDist.
		final Pair< Candidates, RandomAccessibleInterval< FloatType > > output = stardistRunner.run( input );

		if ( null == output )
		{
			/*
			 * Most likely we got interrupted by the user. Don't mind it and
			 * quit quietly.
			 */
			errorMessage = "Detector interrupted.\n";
			return false;
		}

		final Candidates polygons = output.getA();
		final RandomAccessibleInterval< FloatType > probaImg = output.getB();
		final ImagePlus proba = ImageJFunctions.wrap( probaImg, "proba" );

		// Create spots from output.
		for ( final Integer polygonID : polygons.getWinner() )
		{
			// Collect quality = max of proba.
			final PolygonRoi roi = polygons.getPolygonRoi( polygonID );
			proba.setRoi( roi );
			final double quality = proba.getStatistics( Measurements.MIN_MAX ).max;

			// Create ROI.
			final Polygon polygon = roi.getPolygon();
			final double[] xpoly = new double[ polygon.npoints ];
			final double[] ypoly = new double[ polygon.npoints ];
			for ( int i = 0; i < polygon.npoints; i++ )
			{
				xpoly[ i ] = calibration[ 0 ] * ( interval.min( 0 ) + polygon.xpoints[ i ] );
				ypoly[ i ] = calibration[ 1 ] * ( interval.min( 1 ) + polygon.ypoints[ i ] );
			}
			spots.add( SpotRoi.createSpot( xpoly, ypoly, quality ) );
		}

		final long end = System.currentTimeMillis();
		this.processingTime = end - start;

		return true;
	}

	@Override
	public List< Spot > getResult()
	{
		return spots;
	}

	@Override
	public String getErrorMessage()
	{
		return errorMessage;
	}

	@Override
	public long getProcessingTime()
	{
		return processingTime;
	}
}

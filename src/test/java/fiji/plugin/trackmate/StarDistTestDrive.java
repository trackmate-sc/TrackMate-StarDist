/*-
 * #%L
 * TrackMate: your buddy for everyday tracking.
 * %%
 * Copyright (C) 2020 - 2022 TrackMate developers.
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
package fiji.plugin.trackmate;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import de.csbdresden.stardist.Candidates;
import fiji.plugin.trackmate.stardist.StarDistRunner;
import ij.ImagePlus;
import ij.gui.PolygonRoi;
import ij.measure.Measurements;
import ij.process.ImageStatistics;
import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Pair;

public class StarDistTestDrive
{

	public static < T extends Type< T > > void main( final String[] args ) throws IOException, InterruptedException, ExecutionException
	{
		final ImageJ ij = new ImageJ();
		ij.launch( args );
		final Dataset dataset = ( Dataset ) ij.io().open( "samples/P31-crop-2.tif" );
		ij.ui().show( dataset );

		@SuppressWarnings( "unchecked" )
		final RandomAccessibleInterval< T > img = ( RandomAccessibleInterval< T > ) dataset.getImgPlus().getImg();
		final Pair< Candidates, RandomAccessibleInterval< FloatType > > output = new StarDistRunner().run( img );

		final Candidates polygons = output.getA();

		final RandomAccessibleInterval< FloatType > probaImg = output.getB();
		final ImagePlus proba = ImageJFunctions.wrap( probaImg, "proba" );
		int id = 0;
		for ( final Integer polygonID : polygons.getWinner() )
		{
			final PolygonRoi roi = polygons.getPolygonRoi( polygonID );
			proba.setRoi( roi );
			final ImageStatistics stats = proba.getStatistics(
					Measurements.AREA + Measurements.MIN_MAX + Measurements.CENTER_OF_MASS );
			// Units are pixels even for calibrated images.
			System.out.println( String.format( " - roi %3d @ ( %.1f, %.1f ), area = %.1f, quality = %.2f",
					id++, stats.xCenterOfMass, stats.yCenterOfMass, stats.area, stats.max ) );

		}

		System.out.println( "Finished!" );
	}
}

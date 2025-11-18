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
package fiji.plugin.trackmate;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import org.scijava.command.CommandModule;

import de.csbdresden.stardist.Candidates;
import de.csbdresden.stardist.StarDist2DModel;
import de.csbdresden.stardist.StarDist2DNMS;
import de.csbdresden.stardist.Utils;
import ij.ImagePlus;
import ij.gui.PolygonRoi;
import ij.measure.Measurements;
import ij.process.ImageStatistics;
import net.imagej.Dataset;
import net.imagej.DatasetService;
import net.imagej.ImageJ;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Pair;
import net.imglib2.util.ValuePair;
import net.imglib2.view.Views;

public class StarDistTestDrive2
{

	/*
	 * Constants stolen from Opt (otherwise they have default visibility).
	 */

	private static final String PROB_IMAGE = "Probability/Score Image";

	private static final String DIST_IMAGE = "Distance Image";

	private static final String OUTPUT_POLYGONS = "Polygons";

	public static void main( final String[] args ) throws IOException, InterruptedException, ExecutionException
	{
		final ImageJ ij = new ImageJ();
		ij.launch( args );
		final Dataset dataset = ( Dataset ) ij.io().open( "samples/P31-crop-1.tif" );
		ij.ui().show( dataset );

		/*
		 * Default parameters.
		 */

		final StarDist2DModel model = new StarDist2DModel(
				StarDist2DModel.class.getClassLoader().getResource( "models/2D/dsb2018_heavy_augment.zip" ),
				0.479071, 0.3, 16, 96 );
		final boolean normalizeInput = true;
		final double percentileBottom = 1.0;
		final double percentileTop = 99.8;
		final int nTiles = 1;
		final boolean showCsbdeepProgress = false;
		final int excludeBoundary = 2;
		final boolean verbose = false;
		final String roiPosition = "Hyperstack";

		/*
		 * CNN parameters. Defaults are good.
		 */
		final HashMap< String, Object > paramsCNN = new HashMap<>();
		paramsCNN.put( "input", dataset );
		paramsCNN.put( "normalizeInput", normalizeInput );
		paramsCNN.put( "percentileBottom", percentileBottom );
		paramsCNN.put( "percentileTop", percentileTop );
		paramsCNN.put( "clip", false );
		paramsCNN.put( "nTiles", nTiles );
		paramsCNN.put( "batchSize", 1 );
		paramsCNN.put( "blockMultiple", model.sizeDivBy );
		paramsCNN.put( "overlap", model.tileOverlap );
		paramsCNN.put( "showProgressDialog", showCsbdeepProgress );
		paramsCNN.put( "modelFile", model.getFile() );

		/*
		 * Running the CNN.
		 */
		System.out.println( "Running the inference." );
		final Future< CommandModule > futureCNN = ij.command().run( de.csbdresden.csbdeep.commands.GenericNetwork.class, false, paramsCNN );
		final Dataset prediction = ( Dataset ) futureCNN.get().getOutput( "output" );

		/*
		 * Post-processing parameters. Defaults are good too.
		 */
		final HashMap< String, Object > paramsNMS = new HashMap<>();
		paramsNMS.put( "probThresh", model.probThresh );
		paramsNMS.put( "nmsThresh", model.nmsThresh );
		paramsNMS.put( "excludeBoundary", excludeBoundary );
		paramsNMS.put( "roiPosition", roiPosition );
		paramsNMS.put( "verbose", verbose );

		/*
		 * Running post-processing.
		 */
		System.out.println( "Running post-processing." );
		final Pair< Dataset, Dataset > probAndDist = splitPrediction( prediction, ij.dataset() );
		final Dataset probDS = probAndDist.getA();
		final Dataset distDS = probAndDist.getB();
		paramsNMS.put( "prob", probDS );
		paramsNMS.put( "dist", distDS );
		paramsNMS.put( "outputType", OUTPUT_POLYGONS );

		final Future< CommandModule > futureNMS = ij.command().run( StarDist2DNMS.class, false, paramsNMS );
		final Candidates polygons = ( Candidates ) futureNMS.get().getOutput( "polygons" );

		@SuppressWarnings( "rawtypes" )
		final Img probaImg = probDS.getImgPlus().getImg();
		@SuppressWarnings( "unchecked" )
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

	/**
	 * Copied from Star-dist source code.
	 */
	private static Pair< Dataset, Dataset > splitPrediction( final Dataset prediction, final DatasetService dataset )
	{
		@SuppressWarnings( "unchecked" )
		final RandomAccessibleInterval< FloatType > predictionRAI = ( RandomAccessibleInterval< FloatType > ) prediction.getImgPlus();
		final LinkedHashSet< AxisType > predAxes = Utils.orderedAxesSet( prediction );

		final int predChannelDim = IntStream.range( 0, predAxes.size() ).filter( d -> prediction.axis( d ).type() == Axes.CHANNEL ).findFirst().getAsInt();
		final long[] predStart = predAxes.stream().mapToLong( axis -> {
			return axis == Axes.CHANNEL ? 1 : 0;
		} ).toArray();
		final long[] predSize = predAxes.stream().mapToLong( axis -> {
			return axis == Axes.CHANNEL ? prediction.dimension( axis ) - 1 : prediction.dimension( axis );
		} ).toArray();

		final RandomAccessibleInterval< FloatType > probRAI = Views.hyperSlice( predictionRAI, predChannelDim, 0 );
		final RandomAccessibleInterval< FloatType > distRAI = Views.offsetInterval( predictionRAI, predStart, predSize );

		final Dataset probDS = Utils.raiToDataset( dataset, PROB_IMAGE, probRAI, predAxes.stream().filter( axis -> axis != Axes.CHANNEL ) );
		final Dataset distDS = Utils.raiToDataset( dataset, DIST_IMAGE, distRAI, predAxes );

		return new ValuePair<>( probDS, distDS );
	}

}

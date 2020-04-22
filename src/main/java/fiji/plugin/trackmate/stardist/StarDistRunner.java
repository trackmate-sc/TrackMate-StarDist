package fiji.plugin.trackmate.stardist;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import org.scijava.Context;
import org.scijava.command.CommandModule;
import org.scijava.command.CommandService;

import de.csbdresden.stardist.Candidates;
import de.csbdresden.stardist.StarDist2DModel;
import de.csbdresden.stardist.StarDist2DNMS;
import de.csbdresden.stardist.Utils;
import fiji.plugin.trackmate.util.TMUtils;
import net.imagej.Dataset;
import net.imagej.DatasetService;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Pair;
import net.imglib2.util.ValuePair;
import net.imglib2.view.Views;

public class StarDistRunner
{

	/*
	 * Constants stolen from Opt (otherwise they have default visibility).
	 */

	private static final String PROB_IMAGE = "Probability/Score Image";

	private static final String DIST_IMAGE = "Distance Image";

	private static final String OUTPUT_POLYGONS = "Polygons";

	/*
	 * Default parameters.
	 */

	private final static Map< String, Object > PARAMS_CNN = new HashMap<>();

	private final static Map< String, Object > PARAMS_NMS = new HashMap<>();

	static
	{
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
		PARAMS_CNN.put( "normalizeInput", normalizeInput );
		PARAMS_CNN.put( "percentileBottom", percentileBottom );
		PARAMS_CNN.put( "percentileTop", percentileTop );
		PARAMS_CNN.put( "clip", false );
		PARAMS_CNN.put( "nTiles", nTiles );
		PARAMS_CNN.put( "batchSize", 1 );
		PARAMS_CNN.put( "blockMultiple", model.sizeDivBy );
		PARAMS_CNN.put( "overlap", model.tileOverlap );
		PARAMS_CNN.put( "showProgressDialog", showCsbdeepProgress );
		try
		{
			PARAMS_CNN.put( "modelFile", model.getFile() );
		}
		catch ( final IOException e )
		{
			e.printStackTrace();
		}

		/*
		 * Post-processing parameters. Defaults are good too.
		 */
		PARAMS_NMS.put( "probThresh", model.probThresh );
		PARAMS_NMS.put( "nmsThresh", model.nmsThresh );
		PARAMS_NMS.put( "excludeBoundary", excludeBoundary );
		PARAMS_NMS.put( "roiPosition", roiPosition );
		PARAMS_NMS.put( "verbose", verbose );
	}

	/*
	 * Static fields.
	 */

	private final static Context context = TMUtils.getContext();

	public static < T extends Type< T > > Pair< Candidates, RandomAccessibleInterval< FloatType > > run( final RandomAccessibleInterval< T > input )
	{

		try
		{
			/*
			 * Get required services.
			 */
			final DatasetService datasetService = context.getService( DatasetService.class );
			final CommandService commandService = context.getService( CommandService.class );

			/*
			 * Make a dataset from the input.
			 */
			final Dataset dataset = datasetService.create( input );
			final Map< String, Object > paramsCNN = new HashMap<>( PARAMS_CNN );
			paramsCNN.put( "input", dataset );

			/*
			 * Running the CNN.
			 */
			final Future< CommandModule > futureCNN = commandService.run( de.csbdresden.csbdeep.commands.GenericNetwork.class, false, paramsCNN );
			final Dataset prediction = ( Dataset ) futureCNN.get().getOutput( "output" );

			/*
			 * Running post-processing.
			 */
			final Pair< Dataset, Dataset > probAndDist = splitPrediction( prediction, datasetService );
			final Dataset probDS = probAndDist.getA();
			final Dataset distDS = probAndDist.getB();

			final Map< String, Object > paramsNMS = new HashMap<>( PARAMS_NMS );
			paramsNMS.put( "prob", probDS );
			paramsNMS.put( "dist", distDS );
			paramsNMS.put( "outputType", OUTPUT_POLYGONS );

			final Future< CommandModule > futureNMS = commandService.run( StarDist2DNMS.class, false, paramsNMS );
			final Candidates polygons = ( Candidates ) futureNMS.get().getOutput( "polygons" );
			
			@SuppressWarnings( "unchecked" )
			final RandomAccessibleInterval< FloatType > proba = ( RandomAccessibleInterval< FloatType > ) probDS.getImgPlus().getImg();
			return new ValuePair<>( polygons, proba );

		}
		catch ( InterruptedException | ExecutionException e )
		{
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Copied from Star-dist source code.
	 * 
	 * @param prediction
	 * @param dataset
	 * @return
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

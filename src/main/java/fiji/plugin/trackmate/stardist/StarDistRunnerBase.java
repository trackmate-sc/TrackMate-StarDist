package fiji.plugin.trackmate.stardist;

import java.io.File;
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

import de.csbdresden.csbdeep.commands.GenericNetwork;
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

public abstract class StarDistRunnerBase
{

	/*
	 * Constants stolen from Opt (otherwise they have default visibility).
	 */

	private static final String PROB_IMAGE = "Probability/Score Image";

	private static final String DIST_IMAGE = "Distance Image";

	private static final String OUTPUT_POLYGONS = "Polygons";

	/*
	 * Static fields.
	 */

	private final static Context context = TMUtils.getContext();

	/*
	 * Default parameters.
	 */

	private final static Map< String, Object > PARAMS_CNN = new HashMap<>();

	private final static Map< String, Object > PARAMS_NMS = new HashMap<>();

	static
	{
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
		PARAMS_CNN.put( "showProgressDialog", showCsbdeepProgress );

		/*
		 * Post-processing parameters. Defaults are good too.
		 */
		PARAMS_NMS.put( "outputType", OUTPUT_POLYGONS );
		PARAMS_NMS.put( "excludeBoundary", excludeBoundary );
		PARAMS_NMS.put( "roiPosition", roiPosition );
		PARAMS_NMS.put( "verbose", verbose );
	}

	private StarDist2DModel model;

	protected String errorMessage;

	private File modelFile;

	protected abstract StarDist2DModel getModel() throws Exception;

	/**
	 * Initializes this StarDist runner. Must be called before
	 * {@link #run(RandomAccessibleInterval)}. Returns <code>true</code> if the
	 * initialization was successful. If <code>false</code>, an error message
	 * can be obtained via {@link #getErrorMessage()}
	 * 
	 * @return <code>true</code> if the initialization was successful.
	 */
	public boolean initialize()
	{
		this.errorMessage = null;
		try
		{
			this.model = getModel();
		}
		catch ( final Exception e )
		{
			errorMessage = "Could not generate StarDist model: " + e.getMessage();
			return false;
		}

		if ( !model.canGetFile() )
		{
			errorMessage = "Invalid model file. Make sure the model is packaged as a zip or as a jar.";
			return false;
		}
		try
		{
			this.modelFile = model.getFile();
		}
		catch ( final IOException e )
		{
			errorMessage = "Could not load model file: " + e.getMessage();
			return false;
		}
		return true;
	}

	public String getErrorMessage()
	{
		return errorMessage;
	}

	public < T extends Type< T > > Pair< Candidates, RandomAccessibleInterval< FloatType > > run( final RandomAccessibleInterval< T > input )
	{
		this.errorMessage = null;

		/*
		 * Adapt parameters for specific model.
		 */

		final Map< String, Object > paramsCNN = new HashMap<>( PARAMS_CNN );
		final Map< String, Object > paramsNMS = new HashMap<>( PARAMS_NMS );

		paramsCNN.put( "blockMultiple", model.sizeDivBy );
		paramsCNN.put( "overlap", model.tileOverlap );
		paramsCNN.put( "modelFile", modelFile );
		paramsNMS.put( "probThresh", model.probThresh );
		paramsNMS.put( "nmsThresh", model.nmsThresh );

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
			paramsCNN.put( "input", dataset );

			/*
			 * Running the CNN.
			 */
			final Future< CommandModule > futureCNN = commandService.run( GenericNetwork.class, false, paramsCNN );
			final Dataset prediction = ( Dataset ) futureCNN.get().getOutput( "output" );

			/*
			 * Running post-processing.
			 */
			final Pair< Dataset, Dataset > probAndDist = splitPrediction( prediction, datasetService );
			final Dataset probDS = probAndDist.getA();
			final Dataset distDS = probAndDist.getB();

			paramsNMS.put( "prob", probDS );
			paramsNMS.put( "dist", distDS );

			final Future< CommandModule > futureNMS = commandService.run( StarDist2DNMS.class, false, paramsNMS );
			final Candidates polygons = ( Candidates ) futureNMS.get().getOutput( "polygons" );

			@SuppressWarnings( "unchecked" )
			final RandomAccessibleInterval< FloatType > proba = ( RandomAccessibleInterval< FloatType > ) probDS.getImgPlus().getImg();
			return new ValuePair<>( polygons, proba );

		}
		catch ( InterruptedException | ExecutionException e )
		{
			errorMessage = e.getMessage();
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

package fiji.plugin.trackmate.stardist;

import java.awt.Polygon;
import java.util.ArrayList;
import java.util.List;

import de.csbdresden.stardist.Candidates;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotRoi;
import fiji.plugin.trackmate.detection.SpotDetector;
import ij.ImagePlus;
import ij.gui.PolygonRoi;
import ij.measure.Measurements;
import ij.process.ImageStatistics;
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

	protected final double threshold;

	protected List< Spot > spots = new ArrayList<>();

	protected String baseErrorMessage;

	protected String errorMessage;

	protected long processingTime;

	public StarDistDetector( final RandomAccessible< T > img, final Interval interval, final double[] calibration, final double threshold )
	{
		this.img = img;
		this.interval = interval;
		this.calibration = calibration;
		this.threshold = threshold;
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
		final Pair< Candidates, RandomAccessibleInterval< FloatType > > output = StarDistRunner.run( input );
		if ( null == output )
		{
			// Most likely we got interrupted by the user. Don't mind it and
			// quit quietly.
			return true;
		}

		final Candidates polygons = output.getA();
		final RandomAccessibleInterval< FloatType > probaImg = output.getB();
		final ImagePlus proba = ImageJFunctions.wrap( probaImg, "proba" );

		// Create spots from output.
		for ( final Integer polygonID : polygons.getWinner() )
		{
			final PolygonRoi roi = polygons.getPolygonRoi( polygonID );
			proba.setRoi( roi );
			final ImageStatistics stats = proba.getStatistics(
					Measurements.AREA + Measurements.MIN_MAX + Measurements.CENTER_OF_MASS );
			
			final double x = calibration[ 0 ] * ( interval.min( 0 ) + stats.xCenterOfMass );
			final double y = calibration[ 1 ] * ( interval.min( 1 ) + stats.yCenterOfMass );
			final double z = 0.;
			final double radius = Math.sqrt( stats.area * calibration[ 0 ] * calibration[ 1 ] / Math.PI );
			final double quality = stats.max;
			final Spot spot = new Spot( x, y, z, radius, quality );
			spots.add( spot );

			// Store the ROI in the spot.
			final Polygon polygon = roi.getPolygon();
			final double[] xpoly = new double[ polygon.npoints ];
			final double[] ypoly = new double[ polygon.npoints ];
			for ( int i = 0; i < polygon.npoints; i++ )
			{
				xpoly[ i ] = calibration[ 0 ] * ( interval.min( 0 ) + polygon.xpoints[ i ] ) - x;
				ypoly[ i ] = calibration[ 1 ] * ( interval.min( 1 ) + polygon.ypoints[ i ] ) - y;
			}
			spot.setRoi( new SpotRoi( xpoly, ypoly ) );
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

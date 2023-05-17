package fiji.plugin.trackmate;

import java.util.ArrayList;
import java.util.List;

import org.scijava.log.LogLevel;

import fiji.plugin.trackmate.stardist.StarDistDetector;
import fiji.plugin.trackmate.stardist.StarDistRunner;
import fiji.plugin.trackmate.util.TMUtils;
import ij.IJ;
import ij.ImagePlus;
import net.imagej.ImageJ;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

public class StarDist2DZPlayground
{

	public static < T extends RealType< T > & NativeType< T > > void main( final String[] args )
	{
		final ImageJ ij = new ImageJ();
		ij.launch( args );

		ij.log().setLevel( "de.csbdresden.csbdeep.task", LogLevel.ERROR );

		// Open image. It is 3D and has a single time-point.
		final String filePath = "samples/6Feb14FGFonCD1_3_Control_3-2-1.tif";
		final ImagePlus imp = IJ.openImage( filePath );
		imp.show();

		// Run StarDist 2D.
		final double[] calibration = TMUtils.getSpatialCalibration( imp );
		final ImgPlus< T > img = TMUtils.rawWraps( imp );
		final StarDistRunner starDistRunner = new StarDistRunner();
		if ( !starDistRunner.initialize() )
		{
			System.err.println( starDistRunner.getErrorMessage() );
			return;
		}

		System.out.println( "Running detection on " + imp );
		final long start = System.currentTimeMillis();
		final List< List< Spot > > spots = new ArrayList<>( ( int ) img.dimension( img.dimensionIndex( Axes.Z ) ) );
		for ( int z = 0; z < img.dimension( img.dimensionIndex( Axes.Z ) ); z++ )
		{
			final RandomAccessibleInterval< T > slice = Views.hyperSlice( img, img.dimensionIndex( Axes.Z ), z );
			final StarDistDetector< T > detector = new StarDistDetector<>( starDistRunner, slice, slice, calibration );
			if ( !detector.checkInput() || !detector.process() )
			{
				System.err.println( detector.getErrorMessage() );
				return;
			}
			final List< Spot > s = detector.getResult();
			System.out.println( "Slice " + z + " - Found " + s.size() + " 2D spots." );
			spots.add( s );
		}
		final long end = System.currentTimeMillis();


		System.out.println( String.format( "Detection finished in %.1f s", ( end - start ) / 1000. ) );
		System.out.println( "Got " + spots.stream().mapToInt( List::size ).sum() + " 2D spots." );
	}

}

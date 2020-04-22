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
		final Pair< Candidates, RandomAccessibleInterval< FloatType > > output = StarDistRunner.run( img );

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

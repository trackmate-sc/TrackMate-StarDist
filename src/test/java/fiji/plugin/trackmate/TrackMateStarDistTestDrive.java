package fiji.plugin.trackmate;

import java.io.IOException;

import net.imagej.Dataset;
import net.imagej.ImageJ;

public class TrackMateStarDistTestDrive
{

	public static void main( final String[] args ) throws IOException
	{
		final ImageJ ij = new ImageJ();
		ij.launch( args );
		final Dataset dataset = ( Dataset ) ij.io().open( "samples/P31-crop.tif" );
//		final Dataset dataset = ( Dataset ) ij.io().open( "C:/Users/tinevez/Desktop/48hpi_DMSO_H3K9me3488_H4K20me3647_20x_3-Image Export-08_16bit_c1.tif" );
		ij.ui().show( dataset );

		new TrackMatePlugIn().run( null );
	}

}

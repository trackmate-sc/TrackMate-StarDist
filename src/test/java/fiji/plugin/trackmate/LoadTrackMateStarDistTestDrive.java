package fiji.plugin.trackmate;

import java.io.IOException;

import net.imagej.ImageJ;

public class LoadTrackMateStarDistTestDrive
{

	public static void main( final String[] args ) throws IOException
	{
		final ImageJ ij = new ImageJ();
		ij.launch( args );
//		new LoadTrackMatePlugIn().run( "samples/P31-crop.xml" );
		new LoadTrackMatePlugIn().run( "../TrackMate/samples/CellMigration.xml" );
	}
}

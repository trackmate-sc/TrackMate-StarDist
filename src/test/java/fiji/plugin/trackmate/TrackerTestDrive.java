package fiji.plugin.trackmate;

import net.imagej.ImageJ;

public class TrackerTestDrive
{

	/*
	 * MAIN METHOD
	 */

	public static void main( final String[] args )
	{
		final ImageJ ij = new ImageJ();
		ij.launch( args );
		final LoadTrackMatePlugIn_ plugIn = new LoadTrackMatePlugIn_();
		plugIn.run( "samples/P31-crop.xml" );
	}
}

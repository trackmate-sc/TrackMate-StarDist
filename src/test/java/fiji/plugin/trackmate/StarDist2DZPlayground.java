package fiji.plugin.trackmate;

import java.io.IOException;

import org.scijava.log.LogLevel;

import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettingsIO;
import fiji.plugin.trackmate.stardist.StarDist2DZDetectorFactory;
import fiji.plugin.trackmate.tracking.jaqaman.SimpleSparseLAPTrackerFactory;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;
import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

public class StarDist2DZPlayground
{

	public static < T extends RealType< T > & NativeType< T > > void main( final String[] args ) throws IOException
	{
		final net.imagej.ImageJ ij = new net.imagej.ImageJ();
		ij.launch( args );
		ij.log().setLevel( "de.csbdresden.csbdeep.task", LogLevel.ERROR );

		// Open image. It is 3D and has a single time-point.
		final String filePath = "samples/6Feb14FGFonCD1_3_Control_3-2-1.tif";
		final ImagePlus imp = IJ.openImage( filePath );
		imp.show();

		final Settings settings = new Settings( imp );
		settings.detectorFactory = new StarDist2DZDetectorFactory<>();
		settings.detectorSettings = settings.detectorFactory.getDefaultSettings();
		settings.trackerFactory = new SimpleSparseLAPTrackerFactory();
		settings.trackerSettings = settings.trackerFactory.getDefaultSettings();

		final TrackMate trackmate = new TrackMate( settings );
		trackmate.setNumThreads( Prefs.getThreads() );
		if ( !trackmate.checkInput() || !trackmate.process() )
		{
			System.err.println( trackmate.getErrorMessage() );
			return;
		}

		final Model model = trackmate.getModel();
		final DisplaySettings ds = DisplaySettingsIO.readUserDefault();
		final SelectionModel selectionModel = new SelectionModel( model );
		final HyperStackDisplayer view = new HyperStackDisplayer( model, selectionModel, imp, ds );
		view.render();
	}
}

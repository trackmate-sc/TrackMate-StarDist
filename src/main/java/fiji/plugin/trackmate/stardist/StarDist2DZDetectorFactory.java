package fiji.plugin.trackmate.stardist;

import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_TARGET_CHANNEL;
import static fiji.plugin.trackmate.io.IOUtils.readDoubleAttribute;
import static fiji.plugin.trackmate.io.IOUtils.writeAttribute;
import static fiji.plugin.trackmate.tracking.overlap.OverlapTrackerFactory.DEFAULT_MIN_IOU;
import static fiji.plugin.trackmate.tracking.overlap.OverlapTrackerFactory.DEFAULT_SCALE_FACTOR;
import static fiji.plugin.trackmate.tracking.overlap.OverlapTrackerFactory.KEY_MIN_IOU;
import static fiji.plugin.trackmate.tracking.overlap.OverlapTrackerFactory.KEY_SCALE_FACTOR;
import static fiji.plugin.trackmate.util.TMUtils.checkParameter;

import java.util.Map;

import org.jdom2.Element;
import org.scijava.Priority;
import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.detection.Process2DZ;
import fiji.plugin.trackmate.detection.SpotDetector;
import fiji.plugin.trackmate.detection.SpotDetectorFactory;
import fiji.plugin.trackmate.gui.components.ConfigurationPanel;
import fiji.plugin.trackmate.tracking.overlap.OverlapTrackerFactory;
import fiji.plugin.trackmate.util.TMUtils;
import net.imagej.ImgPlus;
import net.imglib2.Interval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

@Plugin( type = SpotDetectorFactory.class, priority = Priority.NORMAL - 2. )
public class StarDist2DZDetectorFactory< T extends RealType< T > & NativeType< T > > extends StarDistDetectorFactory< T >
{

	/** A string key identifying this factory. */
	public static final String DETECTOR_KEY = "STARDIST_2DZ_DETECTOR";

	/** The pretty name of the target detector. */
	public static final String NAME = "StarDist 2D+Z detector";

	/** An html information text. */
	public static final String INFO_TEXT = "<html>"
			+ "This detector relies on StarDist to detect the nuclei in 3D images. "
			+ "It processes each 2D slice of the source image with StarDist 2D and "
			+ "merges the resulting overlapping 2D contours in one 3D mesh per nucleus."
			+ "<p>"
			+ "It only works for 3D images."
			+ "And for this detector to work, the update sites StartDist and CSBDeep "
			+ "must be activated in your Fiji installation."
			+ "<p>"
			+ "The StarDist versatile model for fluorescent nuclei is used, "
			+ "along with the default parameters. Spots are created from the 2D nuclei "
			+ "segmentation results, merged in 3D objects, and with a "
			+ "quality equal to the maximal value of the probability image in the nuclei. "
			+ "<p>"
			+ "If you use this detector for your work, please be so kind as to "
			+ "also cite the StarDist IJ paper: "
			+ "<a href=\"http://doi.org/10.1007/978-3-030-00934-2_30\">"
			+ "Uwe Schmidt, Martin Weigert, Coleman Broaddus, and Gene Myers. "
			+ "Cell Detection with Star-convex Polygons. MICCAI, Granada, Spain, September 2018.</a>"
			+ "</html>";

	private ImgPlus< T > img;

	private Map< String, Object > settings;

	@Override
	public boolean setTarget( final ImgPlus< T > img, final Map< String, Object > settings )
	{
		this.img = img;
		this.settings = settings;
		return checkSettings( settings );
	}

	@Override
	public ConfigurationPanel getDetectorConfigurationPanel( final Settings settings, final Model model )
	{
		return new StarDistDetectorConfigurationPanel( settings, model, StarDist2DZDetectorFactory.NAME, StarDist2DZDetectorFactory.INFO_TEXT )
		{
			private static final long serialVersionUID = 1L;

			@Override
			public Map< String, Object > getSettings()
			{
				final Map< String, Object > s = super.getSettings();
				final Map< String, Object > ds = getDefaultSettings();
				ds.put( KEY_TARGET_CHANNEL, s.get( KEY_TARGET_CHANNEL ) );
				return ds;
			}

			@Override
			protected SpotDetectorFactory< ? > getDetectorFactory()
			{
				return StarDist2DZDetectorFactory.this.copy();
			}
		};
	}

	@Override
	public StarDist2DZDetectorFactory< T > copy()
	{
		return new StarDist2DZDetectorFactory<>();
	}

	@Override
	public Map< String, Object > getDefaultSettings()
	{
		final Map< String, Object > s = super.getDefaultSettings();
		s.put( KEY_MIN_IOU, DEFAULT_MIN_IOU );
		s.put( KEY_SCALE_FACTOR, DEFAULT_SCALE_FACTOR );
		return s;
	}

	@Override
	public boolean checkSettings( final Map< String, Object > settings )
	{
		boolean ok = true;
		final StringBuilder errorHolder = new StringBuilder();
		ok = ok & checkParameter( settings, KEY_TARGET_CHANNEL, Integer.class, errorHolder );
		ok = ok & checkParameter( settings, KEY_MIN_IOU, Double.class, errorHolder );
		ok = ok & checkParameter( settings, KEY_SCALE_FACTOR, Double.class, errorHolder );
		if ( !ok )
			errorMessage = errorHolder.toString();

		return ok;
	}

	@Override
	public boolean marshall( final Map< String, Object > settings, final Element element )
	{
		boolean ok = super.marshall( settings, element );
		final StringBuilder errorHolder = new StringBuilder();
		ok = ok & writeAttribute( settings, element, KEY_SCALE_FACTOR, Double.class, errorHolder );
		ok = ok & writeAttribute( settings, element, KEY_MIN_IOU, Double.class, errorHolder );
		if ( !ok )
			errorMessage = "[" + getKey() + "] " + errorHolder.toString();

		return ok;
	}

	@Override
	public boolean unmarshall( final Element element, final Map< String, Object > settings )
	{
		boolean ok = super.unmarshall( element, settings );
		final StringBuilder errorHolder = new StringBuilder();
		ok = ok & readDoubleAttribute( element, settings, KEY_SCALE_FACTOR, errorHolder );
		ok = ok & readDoubleAttribute( element, settings, KEY_MIN_IOU, errorHolder );
		if ( !ok )
		{
			errorMessage = "[" + getKey() + "] " + errorHolder.toString();
			return false;
		}
		return checkSettings( settings );
	}

	@Override
	public String getInfoText()
	{
		return INFO_TEXT;
	}

	@Override
	public String getKey()
	{
		return DETECTOR_KEY;
	}

	@Override
	public String getName()
	{
		return NAME;
	}
	@Override
	public SpotDetector< T > getDetector( final Interval interval, final int frame )
	{
		// Configure 2D detector.
		final StarDistDetectorFactory< T > detectorFactory = new StarDistDetectorFactory<>();

		// Configure Overlap tracker as a 2D merger.
		final OverlapTrackerFactory trackerFactory = new OverlapTrackerFactory();
		final Map< String, Object > trackerSettings = trackerFactory.getDefaultSettings();
		trackerSettings.put( OverlapTrackerFactory.KEY_IOU_CALCULATION, OverlapTrackerFactory.PRECISE_CALCULATION );
		trackerSettings.put( OverlapTrackerFactory.KEY_MIN_IOU, settings.get( KEY_MIN_IOU ) );
		trackerSettings.put( OverlapTrackerFactory.KEY_SCALE_FACTOR, settings.get( KEY_SCALE_FACTOR ) );

		final Settings s = new Settings();
		s.detectorFactory = detectorFactory;
		s.detectorSettings = detectorFactory.getDefaultSettings();
		s.detectorSettings.put( KEY_TARGET_CHANNEL, 0 );
		s.trackerFactory = trackerFactory;
		s.trackerSettings = trackerSettings;

		final int channel = ( ( Number ) settings.get( KEY_TARGET_CHANNEL ) ).intValue();
		final ImgPlus< T > hs = TMUtils.hyperSlice( img, channel, frame );
		return new Process2DZ<>( hs, s, true );
	}
}

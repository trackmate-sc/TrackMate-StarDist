package fiji.plugin.trackmate.stardist;

import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.ImageIcon;
import javax.swing.JButton;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.detection.SpotDetectorFactory;
import fiji.plugin.trackmate.gui.components.ConfigurationPanel;

public abstract class StarDistDetectorBaseConfigurationPanel extends ConfigurationPanel
{

	private static final long serialVersionUID = 1L;

	protected static final ImageIcon ICON = new ImageIcon( getResource( "images/TrackMateStarDist-logo100x100.png" ) );

	protected final Settings settings;

	protected final Model model;

	public StarDistDetectorBaseConfigurationPanel( final Settings settings, final Model model )
	{
		this.settings = settings;
		this.model = model;
	}

	protected abstract SpotDetectorFactory< ? > getDetectorFactory();

	/**
	 * Launch detection on the current frame.
	 */
	protected void preview( final JButton btnPreview, final Logger localLogger )
	{
		btnPreview.setEnabled( false );
		new Thread( "TrackMate preview detection thread" )
		{
			@Override
			public void run()
			{
				final Settings lSettings = new Settings();
				lSettings.setFrom( settings.imp );
				final int frame = settings.imp.getFrame() - 1;
				lSettings.tstart = frame;
				lSettings.tend = frame;
				lSettings.roi = settings.roi;
				lSettings.xstart = settings.xstart;
				lSettings.xend = settings.xend;
				lSettings.ystart = settings.ystart;
				lSettings.yend = settings.yend;
				lSettings.zstart = settings.zstart;
				lSettings.zend = settings.zend;

				lSettings.detectorFactory = getDetectorFactory();
				lSettings.detectorSettings = getSettings();

				final TrackMate trackmate = new TrackMate( lSettings );
				trackmate.getModel().setLogger( localLogger );

				final boolean detectionOk = trackmate.execDetection();
				if ( !detectionOk )
				{
					localLogger.error( trackmate.getErrorMessage() );
					return;
				}
				localLogger.log( "Found " + trackmate.getModel().getSpots().getNSpots( false ) + " spots." );

				// Wrap new spots in a list.
				final SpotCollection newspots = trackmate.getModel().getSpots();
				final Iterator< Spot > it = newspots.iterator( frame, false );
				final ArrayList< Spot > spotsToCopy = new ArrayList<>( newspots.getNSpots( frame, false ) );
				while ( it.hasNext() )
					spotsToCopy.add( it.next() );

				// Pass new spot list to model.
				model.getSpots().put( frame, spotsToCopy );
				// Make them visible
				for ( final Spot spot : spotsToCopy )
					spot.putFeature( SpotCollection.VISIBILITY, SpotCollection.ONE );

				// Generate event for listener to reflect changes.
				model.setSpots( model.getSpots(), true );

				btnPreview.setEnabled( true );

			}
		}.start();
	}

	protected static URL getResource( final String name )
	{
		return StarDistDetectorFactory.class.getClassLoader().getResource( name );
	}
}

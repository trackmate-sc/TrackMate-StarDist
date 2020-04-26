package fiji.plugin.trackmate.stardist;

import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_TARGET_CHANNEL;
import static fiji.plugin.trackmate.gui.TrackMateWizard.BIG_FONT;
import static fiji.plugin.trackmate.gui.TrackMateWizard.FONT;
import static fiji.plugin.trackmate.gui.TrackMateWizard.SMALL_FONT;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.detection.SpotDetectorFactory;
import fiji.plugin.trackmate.gui.ConfigurationPanel;
import fiji.plugin.trackmate.gui.TrackMateGUIController;
import fiji.plugin.trackmate.util.JLabelLogger;

public class StarDistDetectorConfigurationPanel extends ConfigurationPanel
{

	private static final long serialVersionUID = 1L;

	private static final String TITLE = "StarDist detector";

	protected static final ImageIcon ICON = new ImageIcon( getResource( "images/TrackMateStarDist-logo100x100.png" ) );

	private static final ImageIcon ICON_PREVIEW = new ImageIcon( TrackMateGUIController.class.getResource( "images/flag_checked.png" ) );

	private final JSlider sliderChannel;

	private final JButton btnPreview;

	private final Logger localLogger;

	private final Model model;

	private final Settings settings;

	/**
	 * Create the panel.
	 */
	public StarDistDetectorConfigurationPanel( final Settings settings, final Model model )
	{
		this.settings = settings;
		this.model = model;

		final GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[] { 200, 0, 32 };
		gridBagLayout.rowHeights = new int[] { 0, 0, 145, 0, 0, 55, 23 };
		gridBagLayout.columnWeights = new double[] { 0.0, 1.0, 0.0 };
		gridBagLayout.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 };
		setLayout( gridBagLayout );

		final JLabel lblSettingsForDetector = new JLabel( "Settings for detector:" );
		lblSettingsForDetector.setFont( FONT );
		final GridBagConstraints gbc_lblSettingsForDetector = new GridBagConstraints();
		gbc_lblSettingsForDetector.gridwidth = 3;
		gbc_lblSettingsForDetector.insets = new Insets( 5, 5, 5, 5 );
		gbc_lblSettingsForDetector.fill = GridBagConstraints.HORIZONTAL;
		gbc_lblSettingsForDetector.gridx = 0;
		gbc_lblSettingsForDetector.gridy = 0;
		add( lblSettingsForDetector, gbc_lblSettingsForDetector );

		final JLabel lblStardistDetector = new JLabel( TITLE, ICON, JLabel.RIGHT );
		lblStardistDetector.setFont( BIG_FONT );
		lblStardistDetector.setHorizontalAlignment( SwingConstants.CENTER );
		final GridBagConstraints gbc_lblStardistDetector = new GridBagConstraints();
		gbc_lblStardistDetector.gridwidth = 3;
		gbc_lblStardistDetector.insets = new Insets( 5, 5, 5, 5 );
		gbc_lblStardistDetector.fill = GridBagConstraints.HORIZONTAL;
		gbc_lblStardistDetector.gridx = 0;
		gbc_lblStardistDetector.gridy = 1;
		add( lblStardistDetector, gbc_lblStardistDetector );

		/*
		 * Help text.
		 */
		final JLabel lblHelptext = new JLabel( StarDistDetectorFactory.INFO_TEXT
				.replace( "<br>", "" )
				.replace( "<p>", "<p align=\"justify\">" )
				.replace( "<html>", "<html><p align=\"justify\">" ) );
		lblHelptext.setFont( FONT.deriveFont( Font.ITALIC ) );
		final GridBagConstraints gbc_lblHelptext = new GridBagConstraints();
		gbc_lblHelptext.anchor = GridBagConstraints.NORTH;
		gbc_lblHelptext.fill = GridBagConstraints.HORIZONTAL;
		gbc_lblHelptext.gridwidth = 3;
		gbc_lblHelptext.insets = new Insets( 5, 5, 5, 5 );
		gbc_lblHelptext.gridx = 0;
		gbc_lblHelptext.gridy = 2;
		add( lblHelptext, gbc_lblHelptext );

		/*
		 * Channel selector.
		 */


		final JLabel lblSegmentInChannel = new JLabel( "Segment in channel:" );
		lblSegmentInChannel.setFont( SMALL_FONT );
		final GridBagConstraints gbc_lblSegmentInChannel = new GridBagConstraints();
		gbc_lblSegmentInChannel.anchor = GridBagConstraints.EAST;
		gbc_lblSegmentInChannel.insets = new Insets( 5, 5, 5, 5 );
		gbc_lblSegmentInChannel.gridx = 0;
		gbc_lblSegmentInChannel.gridy = 3;
		add( lblSegmentInChannel, gbc_lblSegmentInChannel );

		sliderChannel = new JSlider();
		final JLabel labelChannel = new JLabel( "1" );
		sliderChannel.addChangeListener( new ChangeListener()
		{
			@Override
			public void stateChanged( final ChangeEvent e )
			{
				labelChannel.setText( "" + sliderChannel.getValue() );
			}
		} );
		final GridBagConstraints gbc_sliderChannel = new GridBagConstraints();
		gbc_sliderChannel.fill = GridBagConstraints.HORIZONTAL;
		gbc_sliderChannel.insets = new Insets( 5, 5, 5, 5 );
		gbc_sliderChannel.gridx = 1;
		gbc_sliderChannel.gridy = 3;
		add( sliderChannel, gbc_sliderChannel );

		labelChannel.setHorizontalAlignment( SwingConstants.CENTER );
		labelChannel.setFont( SMALL_FONT );
		final GridBagConstraints gbc_labelChannel = new GridBagConstraints();
		gbc_labelChannel.insets = new Insets( 5, 5, 5, 5 );
		gbc_labelChannel.gridx = 2;
		gbc_labelChannel.gridy = 3;
		add( labelChannel, gbc_labelChannel );

		/*
		 * Preview.
		 */

		btnPreview = new JButton( "Preview", ICON_PREVIEW );
		btnPreview.setFont( FONT );
		final GridBagConstraints gbc_btnPreview = new GridBagConstraints();
		gbc_btnPreview.gridwidth = 2;
		gbc_btnPreview.fill = GridBagConstraints.HORIZONTAL;
		gbc_btnPreview.anchor = GridBagConstraints.SOUTH;
		gbc_btnPreview.insets = new Insets( 5, 5, 5, 5 );
		gbc_btnPreview.gridx = 1;
		gbc_btnPreview.gridy = 5;
		add( btnPreview, gbc_btnPreview );

		/*
		 * Logger.
		 */

		final JLabelLogger labelLogger = new JLabelLogger();
		final GridBagConstraints gbc_labelLogger = new GridBagConstraints();
		gbc_labelLogger.anchor = GridBagConstraints.EAST;
		gbc_labelLogger.gridwidth = 3;
		gbc_labelLogger.insets = new Insets( 0, 0, 0, 5 );
		gbc_labelLogger.gridx = 0;
		gbc_labelLogger.gridy = 6;
		add( labelLogger, gbc_labelLogger );
		localLogger = labelLogger.getLogger();

		/*
		 * Listeners and specificities.
		 */

		/*
		 * Deal with channels: the slider and channel labels are only visible if
		 * we find more than one channel.
		 */
		if ( null != settings.imp )
		{
			final int n_channels = settings.imp.getNChannels();
			sliderChannel.setMaximum( n_channels );
			sliderChannel.setMinimum( 1 );
			sliderChannel.setValue( settings.imp.getChannel() );

			if ( n_channels <= 1 )
			{
				labelChannel.setVisible( false );
				lblSegmentInChannel.setVisible( false );
				sliderChannel.setVisible( false );
			}
			else
			{
				labelChannel.setVisible( true );
				lblSegmentInChannel.setVisible( true );
				sliderChannel.setVisible( true );
			}
		}

		btnPreview.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( final ActionEvent e )
			{
				preview();
			}
		} );
	}

	@Override
	public Map< String, Object > getSettings()
	{
		final HashMap< String, Object > settings = new HashMap<>( 2 );
		final int targetChannel = sliderChannel.getValue();
		settings.put( KEY_TARGET_CHANNEL, targetChannel );
		return settings;
	}

	@Override
	public void setSettings( final Map< String, Object > settings )
	{
		sliderChannel.setValue( ( Integer ) settings.get( KEY_TARGET_CHANNEL ) );
	}

	@Override
	public void clean()
	{}

	@SuppressWarnings( "rawtypes" )
	protected SpotDetectorFactory< ? > getDetectorFactory()
	{
		return new StarDistDetectorFactory();
	}

	/**
	 * Launch detection on the current frame.
	 */
	protected void preview()
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
				{
					spotsToCopy.add( it.next() );
				}
				// Pass new spot list to model.
				model.getSpots().put( frame, spotsToCopy );
				// Make them visible
				for ( final Spot spot : spotsToCopy )
				{
					spot.putFeature( SpotCollection.VISIBLITY, SpotCollection.ONE );
				}
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

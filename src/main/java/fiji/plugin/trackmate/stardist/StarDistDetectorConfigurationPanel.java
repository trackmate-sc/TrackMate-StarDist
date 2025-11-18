/*-
 * #%L
 * TrackMate: your buddy for everyday tracking.
 * %%
 * Copyright (C) 2020 - 2025 TrackMate developers.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
package fiji.plugin.trackmate.stardist;

import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_TARGET_CHANNEL;
import static fiji.plugin.trackmate.gui.Fonts.BIG_FONT;
import static fiji.plugin.trackmate.gui.Fonts.SMALL_FONT;
import static fiji.plugin.trackmate.stardist.StarDistDetectorFactory.ICON;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.detection.SpotDetectorFactory;
import fiji.plugin.trackmate.gui.GuiUtils;
import fiji.plugin.trackmate.gui.components.ConfigurationPanel;
import fiji.plugin.trackmate.util.DetectionPreview;

public class StarDistDetectorConfigurationPanel extends ConfigurationPanel
{

	private static final long serialVersionUID = 1L;

	private static final String TITLE = "StarDist detector";

	private final JSlider sliderChannel;

	public StarDistDetectorConfigurationPanel( final Settings settings, final Model model )
	{
		final GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[] { 200, 0, 32 };
		gridBagLayout.rowHeights = new int[] { 0, 145, 0, 0, 150 };
		gridBagLayout.columnWeights = new double[] { 0., 1., 0. };
		setLayout( gridBagLayout );

		final JLabel lblStardistDetector = new JLabel( TITLE, ICON, JLabel.RIGHT );
		lblStardistDetector.setFont( BIG_FONT );
		lblStardistDetector.setHorizontalAlignment( SwingConstants.CENTER );
		final GridBagConstraints gbcLblStardistDetector = new GridBagConstraints();
		gbcLblStardistDetector.gridwidth = 3;
		gbcLblStardistDetector.insets = new Insets( 5, 5, 5, 5 );
		gbcLblStardistDetector.fill = GridBagConstraints.HORIZONTAL;
		gbcLblStardistDetector.gridx = 0;
		gbcLblStardistDetector.gridy = 0;
		add( lblStardistDetector, gbcLblStardistDetector );

		/*
		 * Help text.
		 */
		final GridBagConstraints gbcLblHelptext = new GridBagConstraints();
		gbcLblHelptext.anchor = GridBagConstraints.NORTH;
		gbcLblHelptext.fill = GridBagConstraints.BOTH;
		gbcLblHelptext.gridwidth = 3;
		gbcLblHelptext.insets = new Insets( 5, 5, 5, 5 );
		gbcLblHelptext.gridx = 0;
		gbcLblHelptext.gridy = 1;
		gbcLblHelptext.weighty = 1.;
		add( GuiUtils.textInScrollPanel( GuiUtils.infoDisplay(
				StarDistDetectorFactory.INFO_TEXT.replace( "</html>", "" )
						+ "<html>Online documentation: <br/>"
						+ "<a href='" + StarDistDetectorFactory.DOC_URL + "'>"
						+ StarDistDetectorFactory.DOC_URL
						+ "</a></html>" ) ),
				gbcLblHelptext );
		/*
		 * Channel selector.
		 */

		final JLabel lblSegmentInChannel = new JLabel( "Segment in channel:" );
		lblSegmentInChannel.setFont( SMALL_FONT );
		final GridBagConstraints gbcLblSegmentInChannel = new GridBagConstraints();
		gbcLblSegmentInChannel.anchor = GridBagConstraints.EAST;
		gbcLblSegmentInChannel.insets = new Insets( 5, 5, 5, 5 );
		gbcLblSegmentInChannel.gridx = 0;
		gbcLblSegmentInChannel.gridy = 2;
		add( lblSegmentInChannel, gbcLblSegmentInChannel );

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
		final GridBagConstraints gbcSliderChannel = new GridBagConstraints();
		gbcSliderChannel.fill = GridBagConstraints.HORIZONTAL;
		gbcSliderChannel.insets = new Insets( 5, 5, 5, 5 );
		gbcSliderChannel.gridx = 1;
		gbcSliderChannel.gridy = 2;
		add( sliderChannel, gbcSliderChannel );

		labelChannel.setHorizontalAlignment( SwingConstants.CENTER );
		labelChannel.setFont( SMALL_FONT );
		final GridBagConstraints gbcLabelChannel = new GridBagConstraints();
		gbcLabelChannel.insets = new Insets( 5, 5, 5, 5 );
		gbcLabelChannel.gridx = 2;
		gbcLabelChannel.gridy = 2;
		add( labelChannel, gbcLabelChannel );

		/*
		 * Preview.
		 */

		final GridBagConstraints gbcBtnPreview = new GridBagConstraints();
		gbcBtnPreview.gridwidth = 3;
		gbcBtnPreview.fill = GridBagConstraints.BOTH;
		gbcBtnPreview.anchor = GridBagConstraints.SOUTH;
		gbcBtnPreview.insets = new Insets( 5, 5, 5, 5 );
		gbcBtnPreview.gridx = 0;
		gbcBtnPreview.gridy = 4;

		final DetectionPreview detectionPreview = DetectionPreview.create()
				.model( model )
				.settings( settings )
				.detectorFactory( getDetectorFactory() )
				.detectionSettingsSupplier( () -> getSettings() )
				.get();
		add( detectionPreview.getPanel(), gbcBtnPreview );

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
	private SpotDetectorFactory< ? > getDetectorFactory()
	{
		return new StarDistDetectorFactory();
	}
}

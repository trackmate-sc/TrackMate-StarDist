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
import static fiji.plugin.trackmate.gui.Fonts.FONT;
import static fiji.plugin.trackmate.gui.Fonts.SMALL_FONT;
import static fiji.plugin.trackmate.stardist.StarDistCustomDetectorFactory.KEY_MODEL_FILEPATH;
import static fiji.plugin.trackmate.stardist.StarDistCustomDetectorFactory.KEY_OVERLAP_THRESHOLD;
import static fiji.plugin.trackmate.stardist.StarDistCustomDetectorFactory.KEY_SCORE_THRESHOLD;
import static fiji.plugin.trackmate.stardist.StarDistDetectorFactory.ICON;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.detection.SpotDetectorFactory;
import fiji.plugin.trackmate.gui.GuiUtils;
import fiji.plugin.trackmate.gui.components.ConfigurationPanel;
import fiji.plugin.trackmate.stardist.util.FileChooser;
import fiji.plugin.trackmate.stardist.util.FileChooser.DialogType;
import fiji.plugin.trackmate.util.DetectionPreview;

public class StarDistCustomDetectorConfigurationPanel extends ConfigurationPanel
{

	private static final long serialVersionUID = 1L;

	private static final NumberFormat THRESHOLD_FORMAT = new DecimalFormat( "#.##" );

	private static final String TITLE = StarDistCustomDetectorFactory.NAME;

	private static final FileFilter fileFilter = new FileNameExtensionFilter( "Models stored as zip files.", "zip" );

	private final JSlider sliderChannel;

	private final JTextField modelFileTextField;

	private final JButton btnBrowse;

	private final JFormattedTextField ftfScoreThreshold;

	private final JFormattedTextField ftfOverlapThreshold;

	public StarDistCustomDetectorConfigurationPanel( final Settings settings, final Model model )
	{
		final GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[] { 200, 0, 32 };
		gridBagLayout.rowHeights = new int[] { 0, 84, 0, 27, 0, 0, 0, 150 };
		gridBagLayout.columnWeights = new double[] { 1.0, 1.0, 0.0 };
		setLayout( gridBagLayout );

		final JLabel lblStardistDetector = new JLabel( TITLE, ICON, JLabel.RIGHT );
		lblStardistDetector.setFont( BIG_FONT );
		lblStardistDetector.setHorizontalAlignment( SwingConstants.CENTER );
		final GridBagConstraints gbcLblStardistDetector = new GridBagConstraints();
		gbcLblStardistDetector.gridwidth = 3;
		gbcLblStardistDetector.insets = new Insets( 5, 5, 5, 0 );
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
		add( GuiUtils.textInScrollPanel( GuiUtils.infoDisplay( StarDistCustomDetectorFactory.INFO_TEXT ) ),
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
		final GridBagConstraints gbcSliderChannel = new GridBagConstraints();
		gbcSliderChannel.fill = GridBagConstraints.HORIZONTAL;
		gbcSliderChannel.insets = new Insets( 5, 5, 5, 5 );
		gbcSliderChannel.gridx = 1;
		gbcSliderChannel.gridy = 2;
		add( sliderChannel, gbcSliderChannel );

		final JLabel labelChannel = new JLabel( "1" );
		labelChannel.setHorizontalAlignment( SwingConstants.CENTER );
		labelChannel.setFont( SMALL_FONT );
		final GridBagConstraints gbcLabelChannel = new GridBagConstraints();
		gbcLabelChannel.insets = new Insets( 5, 5, 5, 5 );
		gbcLabelChannel.gridx = 2;
		gbcLabelChannel.gridy = 2;
		add( labelChannel, gbcLabelChannel );

		sliderChannel.addChangeListener( l -> labelChannel.setText( "" + sliderChannel.getValue() ) );

		/*
		 * Model file.
		 */

		final JLabel lblCusstomModelFile = new JLabel( "Custom model file:" );
		lblCusstomModelFile.setFont( FONT );
		final GridBagConstraints gbcLblCusstomModelFile = new GridBagConstraints();
		gbcLblCusstomModelFile.anchor = GridBagConstraints.SOUTHWEST;
		gbcLblCusstomModelFile.insets = new Insets( 0, 5, 0, 5 );
		gbcLblCusstomModelFile.gridx = 0;
		gbcLblCusstomModelFile.gridy = 3;
		add( lblCusstomModelFile, gbcLblCusstomModelFile );

		btnBrowse = new JButton( "Browse" );
		btnBrowse.setFont( FONT );
		final GridBagConstraints gbcBtnBrowse = new GridBagConstraints();
		gbcBtnBrowse.insets = new Insets( 5, 0, 0, 5 );
		gbcBtnBrowse.anchor = GridBagConstraints.SOUTHEAST;
		gbcBtnBrowse.gridwidth = 2;
		gbcBtnBrowse.gridx = 1;
		gbcBtnBrowse.gridy = 3;
		add( btnBrowse, gbcBtnBrowse );

		modelFileTextField = new JTextField( "" );
		modelFileTextField.setFont( SMALL_FONT );
		final GridBagConstraints gbc_textField = new GridBagConstraints();
		gbc_textField.gridwidth = 3;
		gbc_textField.insets = new Insets( 0, 5, 5, 5 );
		gbc_textField.fill = GridBagConstraints.BOTH;
		gbc_textField.gridx = 0;
		gbc_textField.gridy = 4;
		add( modelFileTextField, gbc_textField );
		modelFileTextField.setColumns( 10 );

		/*
		 * Score threshold.
		 */

		final JLabel lblScoreTreshold = new JLabel( "Score threshold:" );
		lblScoreTreshold.setFont( SMALL_FONT );
		final GridBagConstraints gbcLblScoreTreshold = new GridBagConstraints();
		gbcLblScoreTreshold.anchor = GridBagConstraints.EAST;
		gbcLblScoreTreshold.insets = new Insets( 5, 5, 5, 5 );
		gbcLblScoreTreshold.gridx = 0;
		gbcLblScoreTreshold.gridy = 5;
		add( lblScoreTreshold, gbcLblScoreTreshold );

		ftfScoreThreshold = new JFormattedTextField( THRESHOLD_FORMAT );
		ftfScoreThreshold.setFont( FONT );
		ftfScoreThreshold.setMinimumSize( new Dimension( 60, 26 ) );
		ftfScoreThreshold.setHorizontalAlignment( SwingConstants.CENTER );
		final GridBagConstraints gbcScore = new GridBagConstraints();
		gbcScore.gridwidth = 2;
		gbcScore.insets = new Insets( 5, 5, 5, 5 );
		gbcScore.gridx = 1;
		gbcScore.gridy = 5;
		add( ftfScoreThreshold, gbcScore );

		/*
		 * Overlap threshold.
		 */

		final JLabel lblOverlapThreshold = new JLabel( "Overlap threshold:" );
		lblOverlapThreshold.setFont( SMALL_FONT );
		final GridBagConstraints gbcLblOverlapThreshold = new GridBagConstraints();
		gbcLblOverlapThreshold.anchor = GridBagConstraints.EAST;
		gbcLblOverlapThreshold.insets = new Insets( 5, 5, 5, 5 );
		gbcLblOverlapThreshold.gridx = 0;
		gbcLblOverlapThreshold.gridy = 6;
		add( lblOverlapThreshold, gbcLblOverlapThreshold );

		ftfOverlapThreshold = new JFormattedTextField( THRESHOLD_FORMAT );
		ftfOverlapThreshold.setFont( FONT );
		ftfOverlapThreshold.setMinimumSize( new Dimension( 60, 26 ) );
		ftfOverlapThreshold.setHorizontalAlignment( SwingConstants.CENTER );
		final GridBagConstraints gbcOverlap = new GridBagConstraints();
		gbcOverlap.gridwidth = 2;
		gbcOverlap.insets = new Insets( 5, 5, 5, 5 );
		gbcOverlap.gridx = 1;
		gbcOverlap.gridy = 6;
		add( ftfOverlapThreshold, gbcOverlap );

		/*
		 * Preview.
		 */

		final GridBagConstraints gbcBtnPreview = new GridBagConstraints();
		gbcBtnPreview.gridwidth = 3;
		gbcBtnPreview.fill = GridBagConstraints.BOTH;
		gbcBtnPreview.anchor = GridBagConstraints.SOUTH;
		gbcBtnPreview.insets = new Insets( 5, 5, 5, 5 );
		gbcBtnPreview.gridx = 0;
		gbcBtnPreview.gridy = 7;

		final DetectionPreview detectionPreview = DetectionPreview.create()
				.model( model )
				.settings( settings )
				.detectorFactory( getDetectorFactory() )
				.detectionSettingsSupplier( () -> getSettings() )
				.thresholdTextField( ftfScoreThreshold )
				.thresholdKey( StarDistCustomDetectorFactory.KEY_SCORE_THRESHOLD )
				.get();
		add( detectionPreview.getPanel(), gbcBtnPreview );

		/*
		 * Listeners and specificities.
		 */

		/*
		 * Deal with channels: the slider and channel labels are only visible if
		 * we find more than one channel.
		 */
		if ( null != settings.imp )
		{
			final int nChannels = settings.imp.getNChannels();
			sliderChannel.setMaximum( nChannels );
			sliderChannel.setMinimum( 1 );
			sliderChannel.setValue( settings.imp.getChannel() );

			if ( nChannels <= 1 )
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

		btnBrowse.addActionListener( l -> browse() );
	}

	@Override
	public Map< String, Object > getSettings()
	{
		final HashMap< String, Object > settings = new HashMap<>( 2 );
		final int targetChannel = sliderChannel.getValue();
		settings.put( KEY_TARGET_CHANNEL, targetChannel );
		settings.put( KEY_MODEL_FILEPATH, modelFileTextField.getText() );
		final double scoreThreshold = ( ( Number ) ftfScoreThreshold.getValue() ).doubleValue();
		settings.put( KEY_SCORE_THRESHOLD, scoreThreshold );
		final double overlapThreshold = ( ( Number ) ftfOverlapThreshold.getValue() ).doubleValue();
		settings.put( KEY_OVERLAP_THRESHOLD, overlapThreshold );
		return settings;
	}

	@Override
	public void setSettings( final Map< String, Object > settings )
	{
		sliderChannel.setValue( ( Integer ) settings.get( KEY_TARGET_CHANNEL ) );
		modelFileTextField.setText( ( String ) settings.get( KEY_MODEL_FILEPATH ) );
		ftfScoreThreshold.setValue( settings.get( KEY_SCORE_THRESHOLD ) );
		ftfOverlapThreshold.setValue( settings.get( KEY_OVERLAP_THRESHOLD ) );
	}

	@Override
	public void clean()
	{}

	@SuppressWarnings( "rawtypes" )
	private SpotDetectorFactory< ? > getDetectorFactory()
	{
		return new StarDistCustomDetectorFactory();
	}

	protected void browse()
	{
		btnBrowse.setEnabled( false );
		try
		{
			final File file = FileChooser.chooseFile( this, modelFileTextField.getText(), fileFilter, "Select a model file", DialogType.LOAD );
			if ( file != null )
				modelFileTextField.setText( file.getAbsolutePath() );
		}
		finally
		{
			btnBrowse.setEnabled( true );
		}
	}
}

package fiji.plugin.trackmate.stardist;

import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_TARGET_CHANNEL;
import static fiji.plugin.trackmate.detection.ThresholdDetectorFactory.KEY_SMOOTHING_SCALE;

import java.util.Map;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;

public class StarDist2DZDetectorConfigurationPanel extends StarDistDetectorConfigurationPanel
{

	private static final long serialVersionUID = 1L;

	public StarDist2DZDetectorConfigurationPanel( final Settings settings, final Model model )
	{
		super( settings, model, StarDist2DZDetectorFactory.NAME, StarDist2DZDetectorFactory.INFO_TEXT );
		this.panelSmoothContour.setVisible( true );
	}

	@Override
	public Map< String, Object > getSettings()
	{
		final Map< String, Object > s = super.getSettings();
		final Map< String, Object > ds = getDetectorFactory().getDefaultSettings();
		ds.put( KEY_TARGET_CHANNEL, s.get( KEY_TARGET_CHANNEL ) );
		ds.put( KEY_SMOOTHING_SCALE, panelSmoothContour.getScale() );
		return ds;
	}

	@Override
	public void setSettings( final Map< String, Object > settings )
	{
		super.setSettings( settings );
		final Object smoothObj = settings.get( KEY_SMOOTHING_SCALE );
		if ( null != smoothObj )
		{
			final double scale = ( ( Number ) smoothObj ).doubleValue();
			panelSmoothContour.setScale( scale );
		}
	}

	@Override
	protected StarDist2DZDetectorFactory< ? > getDetectorFactory()
	{
		return new StarDist2DZDetectorFactory<>();
	}

}

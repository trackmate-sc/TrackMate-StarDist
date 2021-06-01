package fiji.plugin.trackmate.stardist;

import java.net.URL;

import javax.swing.ImageIcon;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
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

	protected static URL getResource( final String name )
	{
		return StarDistDetectorFactory.class.getClassLoader().getResource( name );
	}
}

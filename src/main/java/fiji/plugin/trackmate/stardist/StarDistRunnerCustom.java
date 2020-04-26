package fiji.plugin.trackmate.stardist;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import de.csbdresden.stardist.StarDist2DModel;

public class StarDistRunnerCustom extends StarDistRunnerBase
{

	private final double probThresh;

	private final double nmsThresh;

	private final File modelFile;

	public StarDistRunnerCustom( final File modelFile, final double probThresh, final double nmsThresh )
	{
		this.modelFile = modelFile;
		this.probThresh = probThresh;
		this.nmsThresh = nmsThresh;
	}

	@Override
	protected StarDist2DModel getModel() throws MalformedURLException
	{
		final URL url = modelFile.toURI().toURL();
		return new StarDist2DModel( url, probThresh, nmsThresh, 16, 96 );
	}

}

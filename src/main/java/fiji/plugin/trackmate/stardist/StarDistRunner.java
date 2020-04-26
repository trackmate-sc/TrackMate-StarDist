package fiji.plugin.trackmate.stardist;

import de.csbdresden.stardist.StarDist2DModel;

public class StarDistRunner extends StarDistRunnerBase
{

	@Override
	protected StarDist2DModel getModel()
	{
		return new StarDist2DModel(
				StarDist2DModel.class.getClassLoader().getResource( "models/2D/dsb2018_heavy_augment.zip" ),
				0.479071, 0.3, 16, 96 );
	}
}

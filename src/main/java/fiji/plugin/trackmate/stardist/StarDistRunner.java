/*-
 * #%L
 * TrackMate: your buddy for everyday tracking.
 * %%
 * Copyright (C) 2020 - 2023 TrackMate developers.
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

/*-
 * #%L
 * Fiji distribution of ImageJ for the life sciences.
 * %%
 * Copyright (C) 2020 - 2022 The Institut Pasteur.
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

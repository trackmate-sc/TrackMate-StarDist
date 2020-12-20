from fiji.plugin.trackmate import Model
from fiji.plugin.trackmate import Settings
from fiji.plugin.trackmate import TrackMate
from fiji.plugin.trackmate import Logger
from fiji.plugin.trackmate.io import TmXmlWriter
from fiji.plugin.trackmate.util import LogRecorder;
from fiji.plugin.trackmate.tracking.sparselap import SparseLAPTrackerFactory
from fiji.plugin.trackmate.tracking import LAPUtils
from fiji.plugin.trackmate.util import TMUtils
from fiji.plugin.trackmate.visualization.hyperstack import HyperStackDisplayer
from fiji.plugin.trackmate.visualization import SpotColorGeneratorPerTrackFeature
from fiji.plugin.trackmate.visualization import PerTrackFeatureColorGenerator
from fiji.plugin.trackmate import SelectionModel
from fiji.plugin.trackmate.stardist import StarDistCustomDetectorFactory
from ij import IJ
from datetime import datetime as dt


# ------------------------------------------------------
# 	EDIT FILE PATHS BELOW.
# ------------------------------------------------------

# Shall we display the results each time?
show_output = False

# Where is the model file located?
model_file = '/Users/tinevez/Development/TrackMateWS/TrackMate-StarDist/models/TF_Saved_Stardist_GJ.zip'

# Image files to analyse.
file_paths = []
file_paths.append( '/Users/tinevez/Development/TrackMateWS/TrackMate-StarDist/samples/P31-crop-2.tif' )
file_paths.append( '/Users/tinevez/Development/TrackMateWS/TrackMate-StarDist/samples/P31-crop.tif' )


# ------------------------------------------------------
# 	ACTUAL CODE.
# ------------------------------------------------------
def run( image_file ):

	# Open image.
	imp  = IJ.openImage( image_file )

	# Logger -> content will be saved in the XML file.
	logger = LogRecorder( Logger.VOID_LOGGER )

	logger.log( 'TrackMate-StarDist analysis script\n' )

	dt_string = dt.now().strftime("%d/%m/%Y %H:%M:%S")
	logger.log( dt_string + '\n\n' )
	
	#------------------------
	# Prepare settings object
	#------------------------

	settings = Settings()
	settings.setFrom( imp )
	       
	# Configure StarDist custom detector.
	settings.detectorFactory = StarDistCustomDetectorFactory()
	settings.detectorSettings = { 
	    'SCORE_THRESHOLD' : 0.41,
	    'OVERLAP_THRESHOLD' : 0.5,
	    'TARGET_CHANNEL' : 1,
	    'MODEL_FILEPATH' : model_file,
	}

	# Configure tracker.
	settings.trackerFactory = SparseLAPTrackerFactory()
	settings.trackerSettings = LAPUtils.getDefaultLAPSettingsMap()

	# Declare all features.
	TMUtils.declareAllFeatures( settings )
		
	#-------------------
	# Instantiate plugin
	#-------------------
	    
	trackmate = TrackMate( settings )
	trackmate.getModel().setLogger( logger )
	       
	#--------
	# Process
	#--------
	    
	ok = trackmate.checkInput()
	if not ok:
	    print( str( trackmate.getErrorMessage() ) )
	    return
	    
	ok = trackmate.process()
	if not ok:
	    print( str( trackmate.getErrorMessage() ) )
	    return
	    
	#----------------
	# Save results
	#----------------

	saveFile = TMUtils.proposeTrackMateSaveFile( settings, logger )

	writer = TmXmlWriter( saveFile, logger )
	writer.appendLog( logger.toString() )
	writer.appendModel( trackmate.getModel() )
	writer.appendSettings( trackmate.getSettings() )
	writer.writeToFile();
	print( "Results saved to: " + saveFile.toString() + '\n' );
		   
	#----------------
	# Display results
	#----------------

	if show_output:
		model = trackmate.getModel()
		selectionModel = SelectionModel( model )
		displayer =  HyperStackDisplayer( model, selectionModel, imp )
		displayer.setDisplaySettings( 'DisplaySpotsAsRois', True )
		displayer.setDisplaySettings( 'TrackDisplaymode', 2 )
		
		spot_color = SpotColorGeneratorPerTrackFeature( model, 'TRACK_INDEX' )
		displayer.setDisplaySettings( 'SpotColoring', spot_color )
		
		track_color = PerTrackFeatureColorGenerator( model, 'TRACK_INDEX' )
		displayer.setDisplaySettings( 'TrackColoring', track_color )
		displayer.render()
		displayer.refresh()

# ------------------------------------------------------


for file_path in file_paths:
	
	dt_string = dt.now().strftime("%d/%m/%Y %H:%M:%S")
	print( '\nRunning analysis on %s - %s' % ( file_path, dt_string ) )
	
	run( file_path )

print( 'Finished!' )



	
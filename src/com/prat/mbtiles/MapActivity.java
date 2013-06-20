package com.prat.mbtiles;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.RelativeLayout;
import android.widget.ZoomControls;

import com.nutiteq.MapView;
import com.nutiteq.components.Components;
import com.nutiteq.components.MapPos;
import com.nutiteq.components.Options;
import com.nutiteq.geometry.Marker;
import com.nutiteq.projections.EPSG3857;
import com.nutiteq.style.MarkerStyle;
import com.nutiteq.ui.Label;
import com.nutiteq.ui.ViewLabel;
import com.nutiteq.utils.UiUtils;
import com.nutiteq.utils.UnscaledBitmapLoader;
import com.nutiteq.vectorlayers.MarkerLayer;

//MBTiles: European countries with UTFGrid interaction: http://a.tiles.mapbox.com/v3/nutiteq.geography-class.mbtiles . 
//Or you can download free TileMill http://mapbox.com/tilemill/ and create a package yourself.

public class MapActivity extends Activity {

	private MapView mapView;
	private Marker clickMarker;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		// 1. Get the MapView from the Layout xml - mandatory
		mapView = (MapView) findViewById(R.id.mapView);
		// 2. create and set MapView components - mandatory
		Components components = new Components();
		mapView.setComponents(components);

		// 3. Define map layer for basemap - mandatory
		// MBTiles supports only EPSG3857 projection

		try {
//			File file = new File(Environment.getExternalStorageDirectory()
//                    + File.separator
//                    + "geography-class.mbtiles" //folder name
//                );
			File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath()
                    + File.separator
                    + "test.mbtiles" //folder name
                );
			Log.v("MapActivity", file.getAbsolutePath());
            //file path
            String filePath = file.getAbsolutePath();
			MBTilesMapLayer dbLayer = new MBTilesMapLayer(new EPSG3857(), 0,
					19, filePath.hashCode(), filePath, this);
			mapView.getLayers().setBaseLayer(dbLayer);

			HashMap<String, String> dbMetaData = dbLayer.getDatabase()
					.getMetadata();
			String legend = dbMetaData.get("legend");
			if (legend != null && !legend.equals("")) {
				UiUtils.addWebView(
						(RelativeLayout) findViewById(R.id.mainView), this,
						legend);
			}

			String center = dbMetaData.get("center");
			String bounds = dbMetaData.get("bounds");
			if (center != null) {
				// format: long,lat,zoom
				String[] centerParams = center.split(",");
				mapView.setFocusPoint(mapView
						.getLayers()
						.getBaseLayer()
						.getProjection()
						.fromWgs84(Double.parseDouble(centerParams[0]),
								Double.parseDouble(centerParams[1])));
				mapView.setZoom(Float.parseFloat(centerParams[2]));
			} else if (bounds != null) {
				// format: longMin,latMin,longMax,latMax
				String[] boundsParams = bounds.split(",");
				double xCenter = (Double.parseDouble(boundsParams[0]) + Double
						.parseDouble(boundsParams[2])) / 2;
				double yCenter = (Double.parseDouble(boundsParams[1]) + Double
						.parseDouble(boundsParams[3])) / 2;
				mapView.setFocusPoint(xCenter, yCenter);
				// TODO: calculate and set zoom from bounds
			} else {
				// bulgaria
				mapView.setFocusPoint(mapView.getLayers().getBaseLayer()
						.getProjection()
						.fromWgs84(26.483230800000037, 42.550218000000044));
				// zoom - 0 = world, like on most web maps
				mapView.setZoom(5.0f);

			}

			// add a layer and marker for click labels
			// define small invisible Marker, as Label requires some Marker
			Bitmap pointMarker = UnscaledBitmapLoader.decodeResource(
					getResources(), R.drawable.point);
			MarkerStyle markerStyle = MarkerStyle.builder()
					.setBitmap(pointMarker).setSize(0.01f).setColor(0).build();

			// define label as WebView to show HTML
			WebView labelView = new WebView(this);

			// force to recalculate size
			labelView.setWebViewClient(new WebViewClient() {

				@Override
				public void onPageFinished(final WebView view, final String url) {
					super.onPageFinished(view, url);
					view.invalidate();
				}
			});

			// It is important to set size, exception will come otherwise
			labelView.layout(0, 0, 150, 150);
			Label label = new ViewLabel("", labelView, new Handler());

			clickMarker = new Marker(new MapPos(0, 0), label, markerStyle, null);

			MarkerLayer clickMarkerLayer = new MarkerLayer(new EPSG3857());
			clickMarkerLayer.add(clickMarker);
			mapView.getLayers().addLayer(clickMarkerLayer);

			// add event listener for clicks
			UtfGridLayerEventListener mapListener = new UtfGridLayerEventListener(
					this, mapView, dbLayer, clickMarker);
			mapView.getOptions().setMapListener(mapListener);

		} catch (IOException e) {
			e.printStackTrace();
		}

		// set initial map view camera - optional. "World view" is default
		// rotation - 0 = north-up
		mapView.setRotation(0f);
		// tilt means perspective view. Default is 90 degrees for "normal" 2D
		// map view, minimum allowed is 30 degrees.
		mapView.setTilt(90.0f);

		// Activate some mapview options to make it smoother - optional
		mapView.getOptions().setPreloading(false);
		mapView.getOptions().setSeamlessHorizontalPan(true);
		mapView.getOptions().setTileFading(false);
		mapView.getOptions().setKineticPanning(true);
		mapView.getOptions().setDoubleClickZoomIn(true);
		mapView.getOptions().setDualClickZoomOut(true);

		// set sky bitmap - optional, default - white
		mapView.getOptions().setSkyDrawMode(Options.DRAW_BITMAP);
		mapView.getOptions().setSkyOffset(4.86f);
		mapView.getOptions().setSkyBitmap(
				UnscaledBitmapLoader.decodeResource(getResources(),
						R.drawable.sky_small));

		// Map background, visible if no map tiles loaded - optional, default -
		// white
		mapView.getOptions().setBackgroundPlaneDrawMode(Options.DRAW_BITMAP);
		mapView.getOptions().setBackgroundPlaneBitmap(
				UnscaledBitmapLoader.decodeResource(getResources(),
						R.drawable.background_plane));
		mapView.getOptions().setClearColor(Color.WHITE);

		// configure texture caching - optional, suggested
		mapView.getOptions().setTextureMemoryCacheSize(20 * 1024 * 1024);
		mapView.getOptions().setCompressedMemoryCacheSize(8 * 1024 * 1024);

		// 4. Start the map - mandatory
		mapView.startMapping();

		// 5. zoom buttons using Android widgets - optional
		// get the zoomcontrols that was defined in main.xml
		ZoomControls zoomControls = (ZoomControls) findViewById(R.id.zoomcontrols);
		// set zoomcontrols listeners to enable zooming
		zoomControls.setOnZoomInClickListener(new View.OnClickListener() {
			public void onClick(final View v) {
				mapView.zoomIn();
			}
		});
		zoomControls.setOnZoomOutClickListener(new View.OnClickListener() {
			public void onClick(final View v) {
				mapView.zoomOut();
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}

package bgmap.core.entity;

import java.awt.Dimension;
import java.awt.Image;
import java.awt.Point;

import bgmap.core.AppConfig;

public class Map {	
	
	public static final short MAX_MAPID = 91;
	public static final int partMapWidth = 320;
	public static final int partMapHeight = 480;
	public static final byte COL_COUNT=(byte) (AppConfig.appWidth*3 / Map.partMapWidth+2);
	public static final byte ROW_COUNT=(byte) (AppConfig.appHeight*3 / Map.partMapHeight+2);
		
	private static Image image;  	
	private static byte startRow = 1;	
	private static byte startCol = 1;    
	private static byte pngScale = 1;
	private static Point mapOffset;
	private static Dimension size;
	private static Point mapPos;
		
	public static Image getImage() {
		return image;
	}

	public static void setImage(Image im) {
		Map.image = im;
	}

	public Map() {	
	}
	
	public Map(Image image) {
		Map.image = image;
	}
	
	public static short getStartRow() {
		return startRow;
	}

	public static void setStartRow(byte startRow) {
		Map.startRow = startRow;
	}

	public static short getStartCol() {
		return startCol;
	}

	public static void setStartCol(byte startCol) {
		Map.startCol = startCol;
	}

	public static byte getPngScale() {
		return pngScale;
	}

	public static void setPngScale(byte pngScale) {
		Map.pngScale = pngScale;
	}

	public static Point getMapOffset() {
		return mapOffset;
	}

	public static void setMapOffset(Point mapOffset) {
		Map.mapOffset = mapOffset;
	}

	public static boolean isScrollable(Point point) {
		//ViewMapTest.impanel.offset=new Point(0,0);
		return true;
	}

	
	public static Dimension getSize() {
		return new Dimension(Map.partMapWidth * Map.COL_COUNT, Map.partMapHeight * Map.ROW_COUNT);
	}

	public static Point getMapPos() {
		return mapPos;
	}

	public static void setMapPos(Point mapPos) {
		Map.mapPos = mapPos;
	}

}

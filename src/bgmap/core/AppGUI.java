package bgmap.core;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashSet;

import javax.swing.*;
import javax.swing.event.*;

import bgmap.core.entity.*;

public class AppGUI {		
	static final byte MAX_scale = 127;
	static final byte MIN_scale = 4;
	static final byte START_scale = 100;	
	static final byte CAPTION_HEIGHT=25;
	protected static MapPanel impanel = null;
	final protected static JSlider slider = new JSlider(JSlider.VERTICAL, MIN_scale, MAX_scale, START_scale);;
	final protected static JFrame mainFrame = new JFrame("bgmap");
	final protected static JLayeredPane workPanel = new JLayeredPane();
	protected static JPanel adminPanel = null;	
	protected static HashSet<Maf> mafs = new HashSet<Maf>(); 
	protected static HashSet<MafPanel> mafPanels = new HashSet<MafPanel>(); 
	
    MapMouseAdapter movingAdapt = new MapMouseAdapter();
	
	/**
	 * @param y
	 * @param x
	 * @return url to png file with part of map
	 */
	public static String getPartMapUrl(byte y, byte x){
		return AppConfig.mapsPath+Map.getPngScale() + "_" + y + "_" + x + ".png";
	}
	/**
	 * Load map from parts with size (column,row)
	 * with coordinates from (startColumn, startRow)
	 * and scale.
	 * @return 
	 * @return Image
	 */
	private static void loadPartsMap(byte scale, byte startColumn, byte startRow, byte columnCount, byte rowCount, int drawX, int drawY){
				byte dx;
	    		byte dy=startRow;    		
	            for(byte y=0; y < rowCount; y++ ) {        
	            	dx=startColumn;
	            	for(byte x=0; x < columnCount; x ++ ) { 
	            		Runnable r = new ThreadMapPart(dx, dy, x, y, drawX, drawY);
	            		Thread t = new Thread(r);
	            		t.setPriority(Thread.MAX_PRIORITY);
	            		t.start();	      
	            		dx++;
	                }
	            	dy++;
	            }        	  	
	}
	
	/**
	 * Create map with size (column,row)
	 * with coordinates from (startColumn, startRow)
	 * and scale.
	 * Save link to the image in Map.class
	 * @return Image
	 */	
	protected static Image createMap(byte scale, byte startColumn, byte startRow) {	
		
		startColumn-=Map.COL_COUNT;
		startRow-=Map.ROW_COUNT;
		Image im = new BufferedImage(Map.partMapWidth * Map.COL_COUNT,Map.partMapHeight * Map.ROW_COUNT, BufferedImage.TYPE_INT_RGB);
		Graphics g = im.getGraphics();
		g.setColor(Color.BLACK);
		g.setFont(new Font(g.getFont().getFontName(), Font.PLAIN, 40));
		     
			    try {
					mafs = DBManager.selectMafs(startColumn, (byte) (startColumn+Map.COL_COUNT), startRow, (byte)(startRow+Map.ROW_COUNT));				
				} catch (SQLException e) {
		    		AppConfig.lgTRACE.error(e);
		            AppConfig.lgWARN.error(e);
		            System.exit(1);	 
				}
	        	Image image = null;		        	
	    		byte dx;
	    		byte dy=startRow;    		
	            for(byte y=0; y < Map.ROW_COUNT; y++ ) {        
	            	dx=startColumn;
	            	for(byte x=0; x < Map.COL_COUNT; x ++ ) { 		            	
	            		image = new ImageIcon(AppGUI.getPartMapUrl(dy,dx)).getImage();            		
	                    g.drawImage(image, x * Map.partMapWidth, y * Map.partMapHeight, null);
	            		g.drawRect(x * Map.partMapWidth, y * Map.partMapHeight, Map.partMapWidth,Map.partMapHeight);	            	
	            		g.drawString(dy+" "+dx, x* Map.partMapWidth+150, y* Map.partMapHeight+200);
	            		image.flush();
	            		dx++;
	                }
	            	dy++;
	            }        	     
		
	            g.dispose();
	        	Map.setImage(im);     
	    		Map.setPngScale(scale);
	            Map.setStartCol(startColumn);
	            Map.setStartRow(startRow); 

	
		return im; 
    }
	/**
	 * Redraw map and scroll it 
	 */
	public static void paintMap(){
		if ((impanel.offset.x!=0)||(impanel.offset.y!=0)){
			Image newImage = new BufferedImage(Map.getSize().width,Map.getSize().height, BufferedImage.TYPE_INT_RGB);			
			Image subImage = ((BufferedImage) Map.getImage()).getSubimage(0, 0, Map.getSize().width,Map.getSize().height);
			Graphics g = newImage.getGraphics();
			g.drawImage(subImage, (int)(impanel.offset.x), (int)(impanel.offset.y), null);
			Map.setImage(newImage);
			
			// absolute new coordinates for lefttop full cell 
			int x = impanel.offset.x + Map.getMapOffset().x;	
			int y = impanel.offset.y + Map.getMapOffset().y;
			
			//  signX, signY use when we need calculate absolute left or right side from result position
			//  signMoveX, signMoveY use when we need calculate just mouse move
			byte signX = (byte) (x > 0 ? 1 : 0);	
			byte signMoveX = (byte) (impanel.offset.x > 0 ? 1 : 0);	
			byte signY = (byte) (y > 0 ? 1 : 0);	
			byte signMoveY = (byte) (impanel.offset.y > 0 ? 1 : 0);	
			
			/* Calculate coordinates and parts of one cell at axes
			 * right/down
			 * dx = 0, ax = 0 => Map.partMapWidth
			 * dx = -, ax = 0 => Map.partMapWidth - abs(dx)
			 * dx = +. ax = 1 => dx
			 * left/up
			 * dx = 0, ax = 0 => 0
			 * dx = -, ax = 0 => - abs(dx)
			 * dx = +. ax = 1 => dx - Map.partMapWidth
			 */								
			int dx = x % Map.partMapWidth;		
			int rightPartCellWidth =(Map.partMapWidth) * (1 - signX) + dx ;
			int leftPartCellWidth = rightPartCellWidth - Map.partMapWidth;
			
			int dy =  y % Map.partMapHeight;
			int downPartCellHeight = (Map.partMapHeight) * (1 - signY ) + dy;
			int upPartCellHeight = downPartCellHeight - Map.partMapHeight;
			
			//extra used for change picture when x/y < one cell
			byte extraCols = (byte) (dx > 0 ? 1 : dx < 0 ? -1 : 0);
			byte extraRows = (byte) (dy > 0 ? 1 : dy < 0 ? -1 : 0);
			
			// rightCol downRow used for correct col/row count on right/down sides
			byte rightCol = (byte) (signX - 1);
			byte downRow = (byte) (signY - 1);			
			
			byte addColCount = (byte) (x / Map.partMapWidth + extraCols + rightCol);
			byte startCol = (byte) (Map.getStartCol() - addColCount);
			int wCols = Math.abs(Map.partMapWidth * addColCount);			
			byte addRowCount = (byte) (y / Map.partMapHeight + extraRows + downRow); 
			byte startRow = (byte) (Map.getStartRow() - addRowCount);					
			int hRows = Math.abs(Map.partMapHeight * addRowCount);
			
			//not full left top corner
			byte leftTopCol = (byte) (startCol + extraCols - 1);
			byte topLeftRow = (byte) (startRow + extraRows - 1);
			
			/*if (AppConfig.isDEBUG()){
				System.out.println(impanel.offset);
	    		System.out.println("Map.getStart "+Map.getStartCol()+","+Map.getStartRow());
	    		System.out.println("Map.getMapoffset "+Map.getMapOffset()); 
	    		System.out.println("add "+addColCount+","+addRowCount); 	  	   
	    		System.out.println("start "+startCol+","+startRow);
	    		System.out.println("CellWidth=" +  leftPartCellWidth+ "," + rightPartCellWidth);
	    		System.out.println("cellHeight=" +  upPartCellHeight+ ", " + downPartCellHeight);	    		
	    		System.out.println("wn=" +  Map.getSize().width+ " hn" + Map.getSize().height);
	    		System.out.println("dx=" +  dx+ " dy=" + dy);	
	    		System.out.println("x=" +  x+ " y=" + y);
	    		System.out.println("wCols=" +  wCols+ " hRows=" + hRows);	
	    		System.out.println("signX=" +  signX+ " signY=" + signY);	
	    		System.out.println("signMoveX=" +  signMoveX+ " signMoveY=" + signMoveY);	
	    		System.out.println("extra=" +  extraCols+ "," + extraRows);
	    		System.out.println("rightCol=" +  rightCol+ " downRow=" + downRow);	
	    		System.out.println("COUNT" +  Map.COL_COUNT+ "," + Map.ROW_COUNT);
	    		System.out.println("lefttopCorner = " +  leftTopCol+ "," + topLeftRow);
			}*/
		//	Image pimage  = null;
			g.setColor(new Color(200,0,0,50));			
			//paint left side
			if (signMoveX > 0){		
				loadPartsMap(Map.getPngScale(), 
						(byte) (leftTopCol), (byte) (topLeftRow + addRowCount*signMoveY), (byte) (Math.abs(addColCount)), (byte) (Map.ROW_COUNT), 
						leftPartCellWidth,upPartCellHeight+hRows*(signMoveY));				
			} 
			//paint right side
			else {										
				loadPartsMap(Map.getPngScale(), 
						(byte) (leftTopCol + Map.COL_COUNT - (1  + addColCount)*rightCol), (byte) (topLeftRow  + addRowCount*signMoveY), (byte) (Math.abs(addColCount)), (byte) (Map.ROW_COUNT), 
						Map.getSize().width - wCols + rightPartCellWidth, upPartCellHeight + hRows * (signMoveY));
			}  
			//paint top side
			if (signMoveY > 0){					
				loadPartsMap(Map.getPngScale(), 
						(byte) (leftTopCol), (byte) (topLeftRow),(byte) (Map.COL_COUNT + 1),  (byte) (Math.abs(addRowCount)), 
						leftPartCellWidth, upPartCellHeight);				
			}
			//paint down side
			else{		
				loadPartsMap(Map.getPngScale(), 
						(byte) (leftTopCol), (byte) (topLeftRow + Map.ROW_COUNT - (1 + addRowCount)*downRow), (byte) (Map.COL_COUNT + 1), (byte) (Math.abs(addRowCount)), 
						leftPartCellWidth, Map.getSize().height - hRows + downPartCellHeight);										
			}
			//+1 becouse start col/row must be full cell
			Map.setStartCol((byte) (leftTopCol + 1));
			Map.setStartRow((byte) (topLeftRow + 1));
		/*	if (AppConfig.isDEBUG())
				AppConfig.lgTRACE.debug("Map.getStart after "+Map.getStartCol()+","+Map.getStartRow()); 	*/
			Map.setMapOffset(new Point(rightPartCellWidth,downPartCellHeight));
		}
	}	
	
	/**
	 * create slidebar
	 */
	public static void createSlider() {                 
        slider.setMinorTickSpacing(12);  
        slider.setMajorTickSpacing(25);  
        slider.setPaintTicks(true);  
        slider.setPaintLabels(true);
        slider.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        slider.setDoubleBuffered(false);     
    }  
	/**
	 * create adminpanel
	 */
	public static void creadeAdminPanel() {          
		adminPanel = new JPanel();  		
		JCheckBox addMAFButton = new JCheckBox("Add object");		
		addMAFButton.addItemListener(new ItemListener() {
			
			@Override
			public void itemStateChanged(ItemEvent e) {
				System.out.println(e.getStateChange());
				switch (e.getStateChange()) {
				case ItemEvent.SELECTED:AdminPanelStatus.setAddMaf(true);break;
				case ItemEvent.DESELECTED:AdminPanelStatus.setAddMaf(false);break;
			}
			}
		});
		adminPanel.add(addMAFButton,BorderLayout.CENTER);
    }  
}
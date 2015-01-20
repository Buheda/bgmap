package bgmap.core.model;

import bgmap.core.AppConfig;

public class MafHashValue{
	
	public MafHashValue(short x, short y) {		
		this.x = x;
		this.y = y;
	}

	private short x;
	
	private short y;
	
	public int getX() {
		return x;
	}

	public void setX(short x) {
		this.x = x;
	}

	public int getY() {
		return y;
	}

	public void setY(short y) {
		this.y = y;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + x;
		result = prime * result + y;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof MafHashValue))
			return false;
		MafHashValue other = (MafHashValue) obj;

		if (Math.abs(x - other.x) > AppConfig.sign.getWidth(null)/2 )
			return false;
		if (( other.y - y > AppConfig.sign.getHeight(null))||(other.y - y < 0))
			return false;
		return true;
	}	
}
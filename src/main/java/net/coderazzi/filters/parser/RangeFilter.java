package net.coderazzi.filters.parser;

import javax.swing.RowFilter;

public class RangeFilter extends RowFilter{
	
	private double min,max;
	private int modelIndex;
	public RangeFilter(double min, double max,int modelIndex) {
		super();
		this.min = min;
		this.max = max;
		this.modelIndex = modelIndex;
	}



	@Override
	public boolean include(
			Entry entry) {
		Number value  = (Number)entry.getValue(modelIndex);
		if(value == null){
			return false;
		}
		if(value instanceof Double){
			double d = value.doubleValue();
			return (d >= min)&&(d < max);
		}
		int i = value.intValue();
		return (i >= min)&&(i < max);
	}
}

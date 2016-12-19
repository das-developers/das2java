/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.das2.dataset;

import org.das2.datum.Units;
import org.virbo.dataset.QDataSet;
import org.virbo.dataset.WritableDataSet;
import org.virbo.dsops.Ops;
import org.das2.datum.Datum;

/** 
 *
 * @author leiffert
 */
public class ScatterRebinner implements DataSetRebinner {
	
	@Override
	public QDataSet rebin(QDataSet zds, RebinDescriptor rebinDescX, RebinDescriptor rebinDescY){
		// throws IllegalArgumentException, DasException {

		WritableDataSet result = Ops.zeros( rebinDescX.numberOfBins(), rebinDescY.numberOfBins() );
		
		QDataSet xds = null, yds = null;
		if(!(zds.property("DEPEND_0") == null)){
			xds = (QDataSet) zds.property("DEPEND_0");
		}
		if(!(zds.property("DEPEND_1") == null)){
			 yds = (QDataSet) zds.property("DEPEND_1");
		}
		
		int nx = rebinDescX.numberOfBins()-1;
		int ny = rebinDescY.numberOfBins()-1;

		int xB = 0,yB = 0;
		
		switch(zds.rank()){
			case 0:
				
				break;
			case 1:
				for(int i = 0; i < zds.length(); i++){
					if (xds == null){
							xB = rebinDescX.whichBin(i, Units.dimensionless);
						} else{
							switch (xds.rank()) {
								case 0:
									xB = rebinDescX.whichBin(xds.value(), (Units) xds.property("UNITS"));
									break;
								case 1:
									xB = rebinDescX.whichBin(xds.value(i), (Units) xds.property("UNITS"));
									break;
								case 2:
									System.err.println(" Don't know what to do with rank 2 Depend 0 datasets yet.");
									break;
								default:
									break;
							}
						}
					if(yds == null){
							yB = rebinDescY.whichBin(i, Units.dimensionless);
					}	else{
							switch (yds.rank()) {
								case 0:
									yB = rebinDescY.whichBin(yds.value(), (Units) yds.property("UNITS"));
									break;
								case 1:
									yB = rebinDescY.whichBin(yds.value(i), (Units) yds.property("UNITS"));
									break;
								case 2:
									System.err.println(" Don't know what to do with rank 2 Depend 0 datasets yet.");
									break;
								default:
									break;
							}
						}
					result.putValue(xB,yB,(zds.value(i)));
				}
				break;
			case 2:
				for(int i = 0; i < zds.length(); i++){
					for(int j = 0; j < zds.length(i); j++){
						if (xds == null){
							xB = rebinDescX.whichBin(i, Units.dimensionless);
						} else{
							switch (xds.rank()) {
								case 0:
									xB = rebinDescX.whichBin(xds.value(), (Units) xds.property("UNITS"));
									break;
								case 1:
									xB = rebinDescX.whichBin(xds.value(i), (Units) xds.property("UNITS"));
									break;
								case 2:
									System.err.println(" Don't know what to do with rank 2 dataset here.");
									break;
								default:
									break;
							}
						}
						if(yds == null){
							yB = rebinDescY.whichBin(i, Units.dimensionless);
						}	else{
								switch (yds.rank()) {
									case 0:
										yB = rebinDescY.whichBin(yds.value(), (Units) yds.property("UNITS"));
										break;
									case 1:
										yB = rebinDescY.whichBin(yds.value(j), (Units) yds.property("UNITS"));
										break;
									case 2:
										System.err.println(" Don't know what to do with rank 2 dataset here.");
										break;
									default:
										break;
								}
							}
						result.putValue(xB,yB,(zds.value(i,j)));
					}
				}
				break;
			case 3:
				System.err.println("Does not support rank 3 datasets.");
				break;
			default:
				System.err.println("Rank is not 0, 1, 2 or 3 or not recognized.");
				break;
		}
		
		int xHardBinPlus = 0, xHardBinMinus = 0, yHardBinPlus = 0, yHardBinMinus = 0, xSoftRad = 0, ySoftRad = 0;
		Datum xDat = rebinDescX.binWidthDatum();
		Datum yDat = rebinDescY.binWidthDatum();
		double xWidth = rebinDescX.binWidth();
		double yWidth = rebinDescY.binWidth();
		
		int  xCurCadSep = 0, xMaxCadSep = 0;
		if(xds != null){
			QDataSet xPlus = (QDataSet) xds.property("BIN_PLUS");	
			if(xPlus != null){
				xHardBinPlus = (int) (xPlus.value() / xDat.doubleValue((Units) xPlus.property("UNITS")));
			} 
			QDataSet xMinus = (QDataSet) xds.property("BIN_MINUS");
			if(xMinus != null){
				xHardBinMinus = (int) (xMinus.value() / xDat.doubleValue((Units) xMinus.property("UNITS")));
			} 
			QDataSet xCad = (QDataSet) xds.property("CADENCE");
			double xCadenceVal = 0;
			if(xCad != null){
				if(xds.property("SCALE_TYPE") != null){
					if("log".equals(xds.property("SCALE_TYPE").toString())){
						xCadenceVal = Math.log(xCad.value());
					}
				} else{
						xCadenceVal = xCad.value();
				}
				xSoftRad = (int) Math.round(xCad.value() / xDat.doubleValue((Units) xCad.property("UNITS")));
			} else{
				for(int i =1; i< xds.length(); i++){
					xCurCadSep = rebinDescX.whichBin(xds.value(i), (Units) xds.property("UNITS")) - rebinDescX.whichBin(xds.value(i-1), (Units) xds.property("UNITS"));
					if(xCurCadSep > xMaxCadSep){
						xMaxCadSep = xCurCadSep;
					}
				}
				xSoftRad = (int) (0.6 * xMaxCadSep);
			}
		}
		

		int  yCurCadSep = 0, yMaxCadSep = 0;
		if(yds != null){
			QDataSet yPlus = (QDataSet) yds.property("BIN_PLUS");	
			if(yPlus != null){
				yHardBinPlus = (int) (yPlus.value() / yDat.doubleValue((Units) yPlus.property("UNITS")));
			} 
			QDataSet yMinus = (QDataSet) yds.property("BIN_MINUS");
			if(yMinus != null){
				yHardBinMinus = (int) (yMinus.value() / yDat.doubleValue((Units) yMinus.property("UNITS")));
			}
			
			
			QDataSet yCad = (QDataSet) yds.property("CADENCE");
			double yCadenceVal;
			if(yCad != null){
				if("log".equals(yds.property("SCALE_TYPE").toString())){
					yCadenceVal = Math.log(yCad.value());
				} else{ 
					yCadenceVal = yCad.value();
				}
				ySoftRad = (int) (yCadenceVal / yDat.doubleValue((Units) yCad.property("UNITS")));
			} else{
				for(int i =1; i< yds.length(); i++){
					yCurCadSep = rebinDescY.whichBin(yds.value(i), (Units) yds.property("UNITS")) - rebinDescY.whichBin(yds.value(i-1), (Units) yds.property("UNITS"));
					if(yCurCadSep > yMaxCadSep){
						yMaxCadSep = yCurCadSep;
					}
				}
				ySoftRad =(int) (0.6 * yMaxCadSep);
			}	
		}
		
		
		System.err.println("x number of bins = " + rebinDescX.numberOfBins());
		System.err.println("x Start = " + rebinDescX.start);
		System.err.println("x End = " + rebinDescX.end);
		System.err.println("x BinPlus = " + xHardBinPlus);
		System.err.println("x BinMinus = " + xHardBinMinus);
		System.err.println("x Cadence = " + xSoftRad);
		System.err.println("x Bin width = " + rebinDescX.binWidth());
		System.err.println("x Bin width Datum = " + rebinDescX.binWidthDatum());
		System.err.println("y number of bins = " + rebinDescY.numberOfBins());
		System.err.println("y Start = " + rebinDescX.start);
		System.err.println("y End = " + rebinDescX.end);
		System.err.println("y BinPlus = " + yHardBinPlus);
		System.err.println("y BinMinus = " + yHardBinMinus);
		System.err.println("y Cadence = " + ySoftRad);
		System.err.println("y Bin width = " + rebinDescY.binWidth());
		System.err.println("y Bin width Datum = " + rebinDescY.binWidthDatum());
		System.err.println("y Max Separation in bins = " + yMaxCadSep);
		
		result = InterpolateHardAndSoftEdge(xHardBinPlus,xHardBinMinus, yHardBinPlus, yHardBinMinus, xSoftRad, ySoftRad, nx, ny, result);

		return result;
	
	}
	
	/**  Interpolates values within an ellipse of each data point.
	 * 
	 * A box defined by the hard edge parameters is placed around each data point.
	 * Box width = xHardBinPlus + xHardBinMinus + 1
	 * Box Length = yHardBinPlus + yHardBinMinus + 1
	 * Bins lying within the box all receive the same value as the data point. 
	 * Next, an ellipse is defined around the box with semi-major and minor axis defined by
	 * the soft edge parameters. Bins lying outside the box and within the ellipse are 
	 * given a weight between 0 and 1 inclusive as a function of distance from the center of the box,
	 * zero being on the edge of the ellipse. 
	 * Weight = 1.0 - (currentXBin**2/xSoftRad**2) + (currentYBin**2 / ySoftRad**2)
	 * where currentXBin and currentYBin are the number of bins away from the data point
	 * in the x and y direction, respectively. 
	 * Each bin keeps track of the all the weights and values 
	 * associated with it the values are averaged in the end to give a final value to each bin.
	 * The final value of a bin = Sum (weight * value) / Sum ( weight) 
	 * 
	 * @param xHardBinPlus  - Defines the number of bins greater than the bin of the data
	 *							  point that should receive the same value as the data. 
	 * @param xHardBinMinus - Defines the number of bins less than the bin of the data point
	 *								that should receive the same value as the data.
	 * @param yHardBinPlus  - Same as xHardBinPlus but in the y direction
	 * @param yHardBinMinus - Same as xHardBinMinus but in the y direction
	 * @param xSoftRad  - defines a semi-major/minor axis of the interpolation ellipse
	 * @param ySoftRad -  defines the other semi-major/minor axis of the interpolation ellipse
	 * @param xbins -  Total number of bins in the x direction. Used just to make sure the interpolation ellipse
	 *						does not go off the bounds of the plot.
	 * @param ybins  - same as x bins but in the y direction.
	 * @param data  - the input dataset.
	 * @return 
	 */
	public WritableDataSet InterpolateHardAndSoftEdge(int xHardBinPlus, int xHardBinMinus, int yHardBinPlus,
			  int yHardBinMinus, int xSoftRad, int ySoftRad, int xbins, int ybins, WritableDataSet data){
		// Create template array of weights to be used for each data point
		double[][] ValTimesWeightSum = new double[xbins+1][ybins+1];
		double[][] WeightSum = new double[xbins+1][ybins+1];
		int[][] count = new int[xbins+1][ybins+1];
		double[][] templateBox;
		templateBox = CreateTemplateBox(xHardBinPlus, xHardBinMinus, yHardBinPlus, yHardBinMinus ,xSoftRad,ySoftRad);
		
		double value = 0.0;
		for(int ix = 0; ix <= xbins; ix++){
			for(int iy = 0; iy <= ybins; iy++){
				value = data.value(ix,iy);
				if(value > 0.0){
					for(int i = -(xHardBinMinus+xSoftRad); i <= (xHardBinPlus+xSoftRad); i++){
						for(int j = -(yHardBinMinus+ySoftRad); j <= (yHardBinPlus+ySoftRad); j++){
							if( (ix + i < 0) ||(ix + i > xbins) || (iy + j < 0) || (iy + j > ybins)){
							} else {
								if(templateBox[i+xHardBinMinus+xSoftRad][j+yHardBinMinus+ySoftRad]==0) {
									continue;
								}
								ValTimesWeightSum[ix+i][iy+j] += value*templateBox[i+xHardBinMinus+xSoftRad][j+yHardBinMinus+ySoftRad];
								WeightSum[ix+i][iy+j] += templateBox[i+xHardBinMinus+xSoftRad][j+yHardBinMinus+ySoftRad];
								count[ix+i][iy+j]++;
							}
						}
					}
				}
			}
		}
		for(int ix2 =0; ix2 <= xbins; ix2++){
			for(int iy2=0;iy2<=ybins;iy2++){
				
				if(count[ix2][iy2] >= 1){
						data.putValue(ix2,iy2,(ValTimesWeightSum[ix2][iy2] / WeightSum[ix2][iy2]));
						
				}else {
					data.putValue(ix2,iy2,-1e31);
					//data.putValue(ix2,iy2, 0.0);
				}
			}
		}
		return data;
	}
	
	public double[][] CreateTemplateBox(int xHardBinPlus, int xHardBinMinus, int yHardBinPlus, int yHardBinMinus, int xSoftRad, int ySoftRad){
		double[][] templateWeights = new double[(xHardBinPlus+xHardBinMinus + 2*xSoftRad)+1][(yHardBinPlus+yHardBinMinus + 2*ySoftRad)+1];
		for(int i = -(xHardBinMinus+xSoftRad); i <= (xHardBinPlus+xSoftRad); i++){
			for(int j = -(yHardBinMinus+ySoftRad); j <= (yHardBinPlus+ySoftRad); j++){
				if(InBinPlusMinuxHardEdgeBox(i, j, xHardBinPlus, xHardBinMinus, yHardBinPlus, yHardBinMinus)){
					templateWeights[i+(xHardBinMinus+xSoftRad)][j+(yHardBinMinus+ySoftRad)] = 1.0;
				} else if(InEllipseCutoff(i,j,(double)(xHardBinMinus + xHardBinPlus+ 2*xSoftRad) / 2.0 ,(double)(yHardBinMinus + yHardBinPlus+ 2*ySoftRad) / 2.0 )){
					templateWeights[i+(xHardBinMinus+xSoftRad)][j+yHardBinMinus+ySoftRad] =  1.0 - EllipseValue(i,j,(xHardBinMinus + xHardBinPlus+ 2*xSoftRad) / (double) 2.0 ,(yHardBinMinus + yHardBinPlus+ 2*ySoftRad) /(double) 2.0 );
				} else {
				
					templateWeights[i+(xHardBinMinus+xSoftRad)][j+yHardBinMinus+ySoftRad] = 0.0;
				}
			}
		}
		return templateWeights;
	}
	
	public boolean InBinPlusMinuxHardEdgeBox(int xPlusMinus, int yPlusMinus, int xHardBinPlus, int xHardBinMinus, int yHardBinPlus, int yHardBinMinus){
		if(xPlusMinus >= -(xHardBinMinus) && xPlusMinus <= xHardBinPlus && yPlusMinus >= -(yHardBinMinus) && yPlusMinus <= yHardBinPlus){
			return true;
		} else return false;
	}
	
	public boolean InEllipseCutoff(int xDist, int yDist, double xR, double yR) {
			return (  (  (double)xDist*xDist / (xR*xR)  ) + ( (double)yDist*yDist / (yR*yR) ) ) <= 1.0;
	}
	
	public double EllipseValue(int xDist, int yDist, double xR, double yR){
		return   (double)xDist*xDist / (xR*xR) + ((double)yDist*yDist / (yR*yR));
	}
	
}



package com.example.detectelevator;

import java.util.ArrayList;
import java.util.List;

public class LiRe {

    private List<Double> Xdata;
    private List<Integer> YData;
    private Double result1;
    private Integer result2;

    public LiRe (List xdata, List YData) {
        Xdata = xdata;
        this.YData = YData;
    }

    public Double predictValue () {
        Double X1 = Xdata.get( 0 ) ;
        Integer Y1 = YData.get( 0 ) ;
        Double Xmean = getXMean( Xdata ) ;
        Integer Ymean = getYMean( YData ) ;
        Double lineSlope = getLineSlope( Xmean , Ymean , X1 , Y1 ) ;
        Double YIntercept = getYIntercept( Xmean , Ymean , lineSlope ) ;
        //Double prediction = ( lineSlope * inputValue ) + YIntercept ;
        return lineSlope ;
    }

    public double getLineSlope (Double Xmean, Integer Ymean, Double X1, Integer Y1) {
        double num1 = X1 - Xmean;
        Integer num2 = Y1 - Ymean;
        double denom = (X1 - Xmean) * (X1 - Xmean);
        return (num1 * num2) / denom;
    }

    public Double getYIntercept (Double Xmean, Integer Ymean, Double lineSlope) {
        return Ymean - (lineSlope * Xmean);
    }

    public Double getXMean (List<Double> Xdata) {
        result1 = 0.0;
        for (Integer i = 0; i < Xdata.size(); i++) {
            result1 = result1 + Xdata.get(i);
        }
        return result1;
    }

    public Integer getYMean (List<Integer> Ydata) {
        result2 = 0 ;
        for (Integer i = 0; i < Ydata.size(); i++) {
            result2 = result2 + Ydata.get(i);
        }
        return result2;
    }

}
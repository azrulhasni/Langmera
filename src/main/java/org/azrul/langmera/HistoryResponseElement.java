/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.azrul.langmera;

import java.util.Date;

/**
 *
 * @author Azrul
 */
public class HistoryResponseElement {
    private Date time;
    private String context;
    private String decision;
    private Double interest;
    private Boolean realInterestTogglePoint;

    public Date getTime() {
        return time;
    }

    public void setTime(Date time) {
        this.time = time;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public String getDecision() {
        return decision;
    }

    public void setDecision(String decision) {
        this.decision = decision;
    }

    public Double getInterest() {
        return interest;
    }

    public void setInterest(Double interest) {
        this.interest = interest;
    }

    public Boolean getRealInterestTogglePoint() {
        return realInterestTogglePoint;
    }

    public void setRealInterestTogglePoint(Boolean realInterestTogglePoint) {
        this.realInterestTogglePoint = realInterestTogglePoint;
    }
}

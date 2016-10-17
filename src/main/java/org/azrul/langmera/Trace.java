/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.azrul.langmera;

import java.io.Serializable;
import java.util.Date;

/**
 *
 * @author Azrul
 */
public class Trace implements Serializable{
    private String decisionId;
    private String context;
    private Double qvalue;
    private Date timeStamp;
    private String option;
    private Double score;
    private Double maxQ;
    
    public Trace(){
        
    }

    public Trace(String decisionId, String context, Double qvalue, Double maxQ, Date timeStamp, String option, Double score) {
        this.decisionId = decisionId;
        this.context = context;
        this.qvalue = qvalue;
        this.maxQ = maxQ;
        this.timeStamp = timeStamp;
        this.option = option;
        this.score = score;
    }

    public String getDecisionId() {
        return decisionId;
    }

    public void setDecisionId(String decisionId) {
        this.decisionId = decisionId;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public Double getQvalue() {
        return qvalue;
    }

    public void setQvalue(Double qvalue) {
        this.qvalue = qvalue;
    }

    public Date getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(Date timeStamp) {
        this.timeStamp = timeStamp;
    }

    public String getOption() {
        return option;
    }

    public void setOption(String option) {
        this.option = option;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public Double getMaxQ() {
        return maxQ;
    }

    public void setMaxQ(Double maxQ) {
        this.maxQ = maxQ;
    }
}

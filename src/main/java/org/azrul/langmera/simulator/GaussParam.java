/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.azrul.langmera.simulator;

/**
 *
 * @author Azrul
 */
public class GaussParam<ACTION> {

    public GaussParam(ACTION action, Double mean, Double sd) {
        this.action = action;
        this.mean = mean;
        this.sd = sd;
    }
    private ACTION action;
    private Double mean;
    private Double sd;

    public ACTION getAction() {
        return action;
    }

    public void setAction(ACTION action) {
        this.action = action;
    }

    public Double getMean() {
        return mean;
    }

    public void setMean(Double mean) {
        this.mean = mean;
    }

    public Double getSd() {
        return sd;
    }

    public void setSd(Double sd) {
        this.sd = sd;
    }

}

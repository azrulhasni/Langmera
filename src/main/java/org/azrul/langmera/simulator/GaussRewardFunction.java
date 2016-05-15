/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.azrul.langmera.simulator;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 *
 * @author Azrul
 */
public class GaussRewardFunction<ACTION> {

    private List<GaussParam> params;
    private Random rand;

    public GaussRewardFunction(GaussParam... params) {
        this.params = Arrays.asList(params);
        this.rand = new Random();
    }

    public Double reward(final ACTION a) {
        GaussParam param = params.stream().filter(p -> a.equals(p.getAction())).findFirst().get();
        Double r = this.rand.nextGaussian() * param.getSd() + param.getMean();
        if (r < 0) {
            return -1.0;
        } else if (r > 0) {
            return 1.0;
        }
        return 0.0;

    }

}

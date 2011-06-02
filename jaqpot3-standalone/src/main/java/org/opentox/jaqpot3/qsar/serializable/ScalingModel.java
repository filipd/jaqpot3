package org.opentox.jaqpot3.qsar.serializable;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import org.opentox.toxotis.client.VRI;

/**
 *
 * @author Pantelis Sopasakis
 * @author Charalampos Chomenides
 */
public class ScalingModel implements Serializable {

    // NOTE: HashMap, VRI and Double are all Serializable!    
    private HashMap<VRI, Double> minVals = new HashMap<VRI, Double>();
    private HashMap<VRI, Double> maxVals = new HashMap<VRI, Double>();
    private double min = 0;
    private double max = 1;
    private VRI datasetReference;

    public ScalingModel() {
    }

    public ScalingModel(final double min, final double max) {
        this.min = min;
        this.max = max;
    }

    public Map<VRI, Double> getMaxVals() {
        return maxVals;
    }

    public Map<String, Double> getMaxVals2() {
        if (maxVals == null) {
            return null;
        }
        Map<String, Double> simpleMap = new HashMap<String, Double>();
        if (!maxVals.isEmpty()) {
            Iterator<Entry<VRI, Double>> iterator = maxVals.entrySet().iterator();
            while (iterator.hasNext()) {
                Entry<VRI, Double> entry = iterator.next();
                simpleMap.put(entry.getKey().toString(), entry.getValue());
            }
        }
        return simpleMap;
    }

    public Map<VRI, Double> getMinVals() {
        return minVals;
    }

    public Map<String, Double> getMinVals2() {
        if (minVals == null) {
            return null;
        }
        Map<String, Double> simpleMap = new HashMap<String, Double>();
        if (!minVals.isEmpty()) {
            Iterator<Entry<VRI, Double>> iterator = minVals.entrySet().iterator();
            while (iterator.hasNext()) {
                Entry<VRI, Double> entry = iterator.next();
                simpleMap.put(entry.getKey().toString(), entry.getValue());
            }
        }
        return simpleMap;
    }

    public double getMax() {
        return max;
    }

    public void setMax(double max) {
        this.max = max;
    }

    public double getMin() {
        return min;
    }

    public void setMin(double min) {
        this.min = min;
    }

    public VRI getDatasetReference() {
        return datasetReference;
    }

    public void setDatasetReference(VRI datasetReference) {
        this.datasetReference = datasetReference;
    }
}
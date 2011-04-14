package org.opentox.jaqpot3.qsar.regression;

import java.io.NotSerializableException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import org.opentox.jaqpot3.exception.JaqpotException;
import org.opentox.jaqpot3.qsar.AbstractTrainer;
import org.opentox.jaqpot3.qsar.IClientInput;
import org.opentox.jaqpot3.qsar.ITrainer;
import org.opentox.jaqpot3.qsar.InstancesUtil;
import org.opentox.jaqpot3.qsar.exceptions.BadParameterException;
import org.opentox.jaqpot3.qsar.exceptions.QSARException;
import org.opentox.jaqpot3.qsar.filter.AttributeCleanup;
import org.opentox.jaqpot3.qsar.filter.SimpleMVHFilter;
import org.opentox.jaqpot3.resources.collections.Algorithms;
import org.opentox.jaqpot3.util.Configuration;
import org.opentox.toxotis.client.VRI;
import org.opentox.toxotis.client.collection.Services;
import org.opentox.toxotis.core.component.Algorithm;
import org.opentox.toxotis.core.component.Dataset;
import org.opentox.toxotis.core.component.Feature;
import org.opentox.toxotis.core.component.Model;
import org.opentox.toxotis.exceptions.impl.ServiceInvocationException;
import org.opentox.toxotis.factory.FeatureFactory;
import org.opentox.toxotis.ontology.ResourceValue;
import org.opentox.toxotis.ontology.collection.OTClasses;
import weka.core.Attribute;
import weka.core.Instances;
import weka.classifiers.functions.LinearRegression;

/**
 *
 * @author Pantelis Sopasakis
 * @author Charalampos Chomenides
 */
public class MlrRegression extends AbstractTrainer {

    private VRI targetUri;
    private VRI datasetUri;
    private VRI featureService;
    private org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MlrRegression.class);

    public MlrRegression() {
    }

    @Override
    public Dataset preprocessDataset(Dataset dataset) {
        return dataset;
    }

    private Instances preprocessInstances(Instances in) throws QSARException {
        AttributeCleanup cleanup = new AttributeCleanup(false, AttributeCleanup.ATTRIBUTE_TYPE.string);
        try {
            Instances filt1 = cleanup.filter(in);
            SimpleMVHFilter mvh = new SimpleMVHFilter();
            Instances fin = mvh.filter(filt1);
            return fin;
        } catch (JaqpotException ex) {
            throw new QSARException(ex);
        } catch (QSARException ex) {
            throw new QSARException(ex);
        }
    }

    @Override
    public Model train(Dataset data) throws JaqpotException {

        try {
            /* CONVERT TO INSTANCES & SET CLASS ATTRIBUTE */
            Instances trainingSet = preprocessInstances(data.getInstances());
            Attribute target = trainingSet.attribute(targetUri.toString());
            if (target == null) {
                throw new BadParameterException("The prediction feature you provided was not found in the dataset");
            } else {
                if (!target.isNumeric()) {
                    throw new QSARException("The prediction feature you provided is not numeric.");
                }
            }
            trainingSet.setClass(target);
            /* Very important: place the target feature at the end! (target = last)*/
            int numAttributes = trainingSet.numAttributes();
            int classIndex = trainingSet.classIndex();
            Instances orderedTrainingSet = null;
            List<String> properOrder = new ArrayList<String>(numAttributes);
            for (int j = 0; j < numAttributes; j++) {
                if (j != classIndex) {
                    properOrder.add(trainingSet.attribute(j).name());
                }
            }
            properOrder.add(trainingSet.attribute(classIndex).name());
            try {
                orderedTrainingSet = InstancesUtil.sortByFeatureAttrList(properOrder, trainingSet, -1);
            } catch (JaqpotException ex) {
                logger.error("Improper dataset - training will stop", ex);
                throw ex;
            }
            orderedTrainingSet.setClass(orderedTrainingSet.attribute(targetUri.toString()));

            /* START CONSTRUCTION OF MODEL */
            Model m = new Model(Configuration.getBaseUri().augment("model", getUuid().toString()));
            m.setAlgorithm(getAlgorithm());
            m.setCreatedBy(getTask().getCreatedBy());
            m.setDataset(datasetUri);
            Feature dependentFeature = new Feature(targetUri);
            m.addDependentFeatures(dependentFeature);

            System.out.println(dependentFeature.getMeta().getTitles());

            /*
             * COMPILE THE LIST OF INDEPENDENT FEATURES with the exact order in which
             * these appear in the Instances object (training set).
             */
            List<Feature> independentFeatures = new ArrayList<Feature>();
            for (int i = 0; i < orderedTrainingSet.numAttributes(); i++) {
                Feature f;
                try {
                    f = new Feature(new VRI(orderedTrainingSet.attribute(i).name()));
                    if (orderedTrainingSet.classIndex() != i) {
                        independentFeatures.add(f);
                    }
                } catch (URISyntaxException ex) {
                    throw new QSARException("The URI: " + orderedTrainingSet.attribute(i).name() + " is not valid", ex);
                }
            }
            m.setIndependentFeatures(independentFeatures);


            /* CREATE PREDICTED FEATURE AND POST IT TO REMOTE SERVER */
            try {
                Feature predictedFeature = FeatureFactory.createAndPublishFeature(
                        "Feature created as prediction feature for the MLR model " + m.getUri(), "",
                        new ResourceValue(m.getUri(), OTClasses.Model()), featureService, token);
                m.addPredictedFeatures(predictedFeature);
            } catch (ServiceInvocationException ex) {
                logger.warn(null, ex);
                throw new JaqpotException(ex);
            }


            /* ACTUAL TRAINING OF THE MODEL USING WEKA */
            LinearRegression linreg = new LinearRegression();
            String[] linRegOptions = {"-S", "1", "-C"};

            try {
                linreg.setOptions(linRegOptions);
                linreg.buildClassifier(orderedTrainingSet);
            } catch (final Exception ex) {// illegal options or could not build the classifier!
                String message = "MLR Model could not be trained";
                logger.error(message, ex);
                throw new JaqpotException(message, ex);
            }
            try {
                m.setActualModel(linreg);
            } catch (NotSerializableException ex) {
                String message = "Model is not serializable";
                logger.error(message, ex);
                throw new JaqpotException(message, ex);
            }

            return m;
        } catch (QSARException ex) {
            String message = "QSAR Exception: cannot train MLR model";
            logger.error(message, ex);
            throw new JaqpotException(message, ex);
        }

    }

    @Override
    public ITrainer parametrize(IClientInput clientParameters) throws BadParameterException {
        String targetString = clientParameters.getFirstValue("prediction_feature");
        if (targetString == null) {
            throw new BadParameterException("The parameter 'prediction_feaure' is mandatory for this algorithm.");
        }
        try {
            targetUri = new VRI(targetString);
        } catch (URISyntaxException ex) {
            throw new BadParameterException("The parameter 'prediction_feaure' you provided is not a valid URI.", ex);
        }
        String datasetUriString = clientParameters.getFirstValue("dataset_uri");
        if (datasetUriString == null) {
            throw new BadParameterException("The parameter 'dataset_uri' is mandatory for this algorithm.");
        }
        try {
            datasetUri = new VRI(datasetUriString);
        } catch (URISyntaxException ex) {
            throw new BadParameterException("The parameter 'dataset_uri' you provided is not a valid URI.", ex);
        }
        String featureServiceString = clientParameters.getFirstValue("feature_service");
        if (featureServiceString != null) {
            try {
                featureService = new VRI(featureServiceString);
            } catch (URISyntaxException ex) {
                throw new BadParameterException("The parameter 'feature_service' you provided is not a valid URI.", ex);
            }
        } else {
            featureService = Services.ideaconsult().augment("feature");
        }
        return this;
    }

    @Override
    public Algorithm getAlgorithm() {
        return Algorithms.mlr();
    }
}

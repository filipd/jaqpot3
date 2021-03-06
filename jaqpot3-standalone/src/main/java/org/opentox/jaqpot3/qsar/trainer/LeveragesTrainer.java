package org.opentox.jaqpot3.qsar.trainer;

import org.opentox.jaqpot3.qsar.serializable.LeveragesModel;
import Jama.Matrix;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.opentox.jaqpot3.exception.JaqpotException;
import org.opentox.jaqpot3.qsar.AbstractTrainer;
import org.opentox.jaqpot3.qsar.IClientInput;
import org.opentox.jaqpot3.qsar.ITrainer;
import org.opentox.jaqpot3.qsar.exceptions.BadParameterException;
import org.opentox.jaqpot3.qsar.exceptions.QSARException;
import org.opentox.jaqpot3.qsar.util.AttributeCleanup;
import org.opentox.jaqpot3.qsar.util.SimpleMVHFilter;
import org.opentox.jaqpot3.resources.collections.Algorithms;
import org.opentox.jaqpot3.util.Configuration;
import org.opentox.toxotis.client.VRI;
import org.opentox.toxotis.client.collection.Services;
import org.opentox.toxotis.core.component.Algorithm;
import org.opentox.toxotis.core.component.Feature;
import org.opentox.toxotis.core.component.Model;
import org.opentox.toxotis.database.engine.task.UpdateTask;
import org.opentox.toxotis.database.exception.DbException;
import org.opentox.toxotis.ontology.ResourceValue;
import org.opentox.toxotis.ontology.collection.OTClasses;
import weka.core.Attribute;
import weka.core.Instances;

/**
 *
 * @author Pantelis Sopasakis
 * @author Charalampos Chomenides
 */
public class LeveragesTrainer extends AbstractTrainer {

    private VRI targetUri;
    private VRI datasetUri;
    private VRI featureService;
    private UUID uuid = UUID.randomUUID();
    private org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(LeveragesTrainer.class);

    private Instances preprocessInstances(Instances in) throws QSARException {
        AttributeCleanup cleanup = new AttributeCleanup(false, AttributeCleanup.AttributeType.string);
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
    public Model train(Instances trainingSet) throws JaqpotException {
        try {
            trainingSet = preprocessInstances(trainingSet);
            Attribute target = trainingSet.attribute(targetUri.toString());
            if (target == null) {
                // Do nothing! It doesn't matter :)
            } else {
                if (!target.isNumeric()) {
                    throw new QSARException("The prediction feature you provided is not numeric.");
                }
            }

            int targetIndex = -1;
            if (target != null) {
                targetIndex = trainingSet.attribute(targetUri.toString()).index();
            }
            /* PROVIDE META DATA FOR THE MODEL */
            Model model = new Model(Configuration.getBaseUri().augment("model", uuid.toString()));
            model.setAlgorithm(getAlgorithm());
            model.setCreatedBy(getTask().getCreatedBy());
            Feature dependentFeature = new Feature(targetUri);
            model.addDependentFeatures(dependentFeature);
            model.setDataset(datasetUri);
            List<Feature> independentFeatures = new ArrayList<Feature>();
            for (int i = 0; i < trainingSet.numAttributes(); i++) {
                if (i != targetIndex) {
                    try {
                        independentFeatures.add(new Feature(new VRI(trainingSet.attribute(i).name())));
                    } catch (URISyntaxException ex) {
                        throw new QSARException("The URI: " + trainingSet.attribute(i).name() + " is not valid", ex);
                    }
                }
            }
            model.setIndependentFeatures(independentFeatures);
            Feature predictedFeature = new Feature();
            predictedFeature.getMeta().addHasSource(new ResourceValue(model.getUri(), OTClasses.model())).
                    addTitle("Feature created as prediction feature for DoA model " + model.getUri());

            Future<VRI> predictedFeatureUri = predictedFeature.publish(featureService, token);
            /* Wait for remote server to respond */
            while (!predictedFeatureUri.isDone()) {
                Thread.sleep(1000);
            }
            try {
                VRI resultUri = predictedFeatureUri.get();
                predictedFeature.setUri(resultUri);
                getTask().getMeta().addComment("Prediction Feature created: " + resultUri);

                UpdateTask updater = new UpdateTask(getTask());
                updater.setUpdateMeta(true);
                try {
                    updater.update();
                } catch (DbException ex) {
                    String msg = "DB Update operation failed";
                    logger.error(msg);
                    throw new JaqpotException(msg, ex);
                } finally {
                    try {
                        updater.close();
                    } catch (DbException ex) {
                        String msg = "DB Updater uncloseable";
                        logger.error(msg);
                        throw new JaqpotException(msg, ex);
                    }
                }

            } catch (ExecutionException ex) {
                logger.error("Exceptional event occured while registering/updating task in DB", ex);
            }

            model.addPredictedFeatures(predictedFeature);
            if (target != null) {
                trainingSet.deleteAttributeAt(targetIndex);
            }
            int k = trainingSet.numInstances();
            int n = trainingSet.numAttributes();
            double[][] dataArray = new double[k][n];
            for (int i = 0; i < k; i++) {
                dataArray[i] = trainingSet.instance(i).toDoubleArray();
            }
            Matrix dataMatrix = new Matrix(dataArray);                        
            Matrix omega = (dataMatrix.transpose().times(dataMatrix)).inverse();
            LeveragesModel actualModel = new LeveragesModel();
            actualModel.setDataMatrix(omega);
            actualModel.setGamma(k, n);
            model.setActualModel(actualModel);
            model.getMeta().addCreator(getTask().getCreatedBy().getUid()).addDescription("Model designed to tell wether a compound "
                    + "belongs to the domain of applicability of any model trained with the dataset " + datasetUri + " using the "
                    + "leverages algorithm");


            return model;
        } catch (InterruptedException ex) {
            logger.error("Action was suddenly interrupted :(", ex);
            throw new JaqpotException(ex);
        } catch (QSARException ex) {
            logger.debug(null, ex);
            throw new JaqpotException(ex);
        } catch (Exception ex) {
            logger.debug(null, ex);
            throw new JaqpotException(ex);
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
        return Algorithms.leverages();
    }
}

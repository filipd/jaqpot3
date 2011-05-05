package org.opentox.jaqpot3.www.services;

import java.net.URISyntaxException;
import org.opentox.jaqpot3.exception.JaqpotException;
import org.opentox.jaqpot3.qsar.IClientInput;
import org.opentox.jaqpot3.qsar.ITrainer;
import org.opentox.jaqpot3.qsar.exceptions.BadParameterException;
import org.opentox.jaqpot3.util.Configuration;
import org.opentox.toxotis.client.VRI;
import org.opentox.toxotis.core.component.Dataset;
import org.opentox.toxotis.core.component.ErrorReport;
import org.opentox.toxotis.core.component.Model;
import org.opentox.toxotis.core.component.Task.Status;
import org.opentox.toxotis.database.engine.model.AddModel;
import org.opentox.toxotis.database.engine.task.UpdateTask;
import org.opentox.toxotis.database.exception.DbException;
import org.opentox.toxotis.exceptions.impl.ServiceInvocationException;
import org.opentox.toxotis.ontology.ResourceValue;
import org.opentox.toxotis.util.aa.AuthenticationToken;
import org.opentox.toxotis.util.aa.policy.IPolicyWrapper;
import org.opentox.toxotis.util.aa.policy.PolicyManager;

/**
 *
 * @author Pantelis Sopasakis
 * @author Charalampos Chomenides
 */
public class TrainingService extends RunnableTaskService {

    private ITrainer trainer;
    private IClientInput clientInput;
    private AuthenticationToken token;
    private org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TrainingService.class);

    public TrainingService(ITrainer trainer, IClientInput clientInput, AuthenticationToken token) {
        this.trainer = trainer;
        this.clientInput = clientInput;
        this.token = token;
    }

    @Override
    public void run() {
        long startingTime = System.currentTimeMillis();

        /*
         * Change the status of the task from QUEUED to RUNNING
         * The task has ALREADY been registered (see ModelResource)
         */
        trainer.getTask().setStatus(Status.RUNNING);
        trainer.getTask().getMeta().addHasSource(new ResourceValue(trainer.getAlgorithm().getUri(), null));

        UpdateTask updater = new UpdateTask(trainer.getTask());
        updater.setUpdateTaskStatus(true);
        try {
            updater.update();// update the task
        } catch (DbException ex) {
            logger.error("Cannot update task to RUNNING", ex);
        } finally {
            if (updater != null) {
                try {
                    updater.close();
                } catch (DbException ex) {
                }
            }
        }

        String datasetUri = clientInput.getFirstValue("dataset_uri");


        try {
            trainer.parametrize(clientInput); // #NODE_01
            VRI datasetURI = new VRI(datasetUri);// #NODE_02


            Dataset ds = null;
            ds = new Dataset(datasetURI);// #NODE_03_a
            if (trainer.needsDataset()) {
                ds.loadFromRemote(token);// #NODE_03_a
            }

            Model resultModel = trainer.train(ds);// #NODE_03_b

            /* Create a policy for the model (on behalf of the user) */
            IPolicyWrapper pw = PolicyManager.defaultSignleUserPolicy("model_" + resultModel.getUri().getId(), resultModel.getUri(), token);
            pw.publish(null, token);

            /* STORE THE MODEL IN THE DATABASE :-)*/
            resultModel.getMeta().addCreator(token.getUser().getUid());

            AddModel modelAdder = new AddModel(resultModel);

            //TODO: Handle exceptions properly
            modelAdder.write();
            modelAdder.close();


            /* UPDATE THE TASK - COMPLETED :)*/
            trainer.getTask().setDuration(System.currentTimeMillis() - startingTime);
            trainer.getTask().getMeta().
                    addComment("Training completed successfully! The model is now stored in the database.");
            trainer.getTask().setStatus(Status.COMPLETED).setHttpStatus(200).setResultUri(resultModel.getUri()).setPercentageCompleted(100);

            UpdateTask taskFinalUpdater = new UpdateTask(trainer.getTask());
            taskFinalUpdater.setUpdateTaskStatus(true);
            taskFinalUpdater.setUpdateDuration(true);
            taskFinalUpdater.setUpdateResultUri(true);
            taskFinalUpdater.setUpdateMeta(true);
            taskFinalUpdater.update();
            taskFinalUpdater.close();

        } catch (BadParameterException ex) {// FROM #NODE_01
            updateFailedTask(trainer.getTask(), ex, "Task failed due to illegal parametrization. ", 400,
                    trainer.getTask().getCreatedBy().getUid());
            logger.trace(null, ex);
        } catch (URISyntaxException ex) {// FROM #NODE_02
            ex.printStackTrace();
            updateFailedTask(trainer.getTask(), ex, "The dataset URI you provided cannot be cast as a valid URI object.", 400,
                    trainer.getTask().getCreatedBy().getUid());
            logger.trace(null, ex);
        } catch (JaqpotException ex) {//FROM NODE_03
            logger.info(null, ex);
            ex.printStackTrace();
            updateFailedTask(trainer.getTask(), ex, "", 500, Configuration.BASE_URI);
        } catch (ServiceInvocationException ex){
            ErrorReport er = ex.asErrorReport();
            er.setErrorCode(ex.getClass().getSimpleName());
            updateFailedTask(trainer.getTask(), er);
        }catch (Throwable throwable) {
            throwable.printStackTrace();
            logger.error(null, throwable);
            updateFailedTask(trainer.getTask(), throwable, "", 500, Configuration.BASE_URI);
        }

    }
}

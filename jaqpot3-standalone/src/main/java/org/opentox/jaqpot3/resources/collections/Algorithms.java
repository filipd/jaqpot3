package org.opentox.jaqpot3.resources.collections;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import org.opentox.jaqpot3.util.Configuration;
//import org.opentox.toxotis.ToxOtisException;
import org.opentox.toxotis.core.component.Algorithm;
import org.opentox.toxotis.core.component.Parameter;
import org.opentox.toxotis.core.html.HTMLUtils;
import org.opentox.toxotis.exceptions.impl.ToxOtisException;
import org.opentox.toxotis.ontology.LiteralValue;
import org.opentox.toxotis.ontology.MetaInfo;
import org.opentox.toxotis.ontology.OntologicalClass;
import org.opentox.toxotis.ontology.ResourceValue;
import org.opentox.toxotis.ontology.collection.OTAlgorithmTypes;
import org.opentox.toxotis.ontology.collection.OTClasses;
import org.opentox.toxotis.ontology.impl.MetaInfoImpl;

/**
 *
 * @author Pantelis Sopasakis
 * @author Charalampos Chomenides
 */
public class Algorithms {

    private static Algorithm mlr;
    private static Algorithm svm;
    private static Algorithm svc;
    private static Algorithm leverages;
    private static Algorithm mvh;
    private static Algorithm cleanup;
    private static Algorithm plsFilter;
    private static Algorithm svmFilter;
    private static Algorithm fastRbfNn;
    private static Algorithm scaling;
//    private static Algorithm fcbf;
//    private static Algorithm multifilter;
    private static Set<Algorithm> repository;
    private static org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Algorithms.class);

    private static final String _LICENSE = "JAQPOT - Just Another QSAR Project under OpenTox\n" +
            "Machine Learning algorithms designed for the prediction of toxicological " +
            "features of chemical compounds become available on the Web.\n\nJaqpot is developed " +
            "under OpenTox (see http://opentox.org ) which is an FP7-funded EU research project. " +
            "This project was developed at the Automatic Control Lab in the Chemical Engineering " +
            "School of National Technical University of Athens. Please read README for more " +
            "information.\n" +
            "\n" +
            "Copyright (C) 2009-2010 Pantelis Sopasakis & Charalampos Chomenides\n" +
            "\n" +
            "This program is free software: you can redistribute it and/or modify " +
            "it under the terms of the GNU General Public License as published by " +
            "the Free Software Foundation, either version 3 of the License, or " +
            "(at your option) any later version.\n" +
            "\n" +
            "This program is distributed in the hope that it will be useful, " +
            "but WITHOUT ANY WARRANTY; without even the implied warranty of " +
            "MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the " +
            "GNU General Public License for more details." +
            "\n\n" +
            "You should have received a copy of the GNU General Public License " +
            "along with this program.  If not, see http://www.gnu.org/licenses/ .\n\n" +
            "The Jaqpot source code is found online at https://github.com/alphaville/jaqpot";

    public static void main(String... args){
        System.out.println(HTMLUtils.normalizeTextForHtml(HTMLUtils.linkUrlsInText(_LICENSE)));
    }

    static {
        repository = new HashSet<Algorithm>();
        for (Method m : Algorithms.class.getDeclaredMethods()) {
            try {
                if (m.getReturnType().equals(Algorithm.class) && m.getParameterTypes().length == 0) {
                    repository.add((Algorithm) m.invoke(null));
                }
            } catch (IllegalAccessException ex) {
                logger.error("Cannot access method in Algorithms", ex);
                throw new RuntimeException(ex);
            } catch (IllegalArgumentException ex) {
                logger.error("Unexpected Illegal Argument Exception while accessing no-argument methods in Algorithms", ex);
                throw new RuntimeException(ex);
            } catch (InvocationTargetException ex) {
                logger.error("Some algorithm definition throws an exception", ex);
                throw new RuntimeException(ex);
            }
        }
    }

    public static Set<Algorithm> getAll() {
        return repository;
    }

    public static Algorithm forName(String name) {
        for (Algorithm a : getAll()) {
            if (a.getMeta().getTitles().contains(new LiteralValue(name))) {
                return a;
            }
        }
        return null;
    }
    

    public static Algorithm fastRbfNn() {
        if (fastRbfNn == null) {
            try {
                fastRbfNn = new Algorithm(Configuration.getBaseUri().augment("algorithm", "fastRbfNn"));
                MetaInfo algorithmMeta = new MetaInfoImpl().addTitle("fastRbfNn", "Fast Training Algorithm for RBF networks based on subtractive clustering").
                        addSubject("Regression", "RBF", "Training", "ANN", "Artificial Neural Network", "Machine Learning", "Single Target", "Eager Learning").
                        addContributor("Pantelis Sopasakis", "Charalampos Chomenides").
                        addDescription("Fast-RBF-NN is a training algorithm for Radial Basis Function Neural Networks. The algorithm is based on the "
                        + "subtractive clustering technique and has a number of advantages compared to the traditional learning algorithms including faster "
                        + "training times and more accurate predictions. Due to these advantages the method proves suitable for developing models for complex "
                        + "nonlinear systems. The algorithm is presented in detail in the publication: H. Sarimveis, A. Alexandridis and G. Bafas, 'A fast algorithm "
                        + "for RBF networks based on subtractive clustering', Neurocomuting 51 (2003), 501-505").
                        addPublisher(Configuration.BASE_URI).
                        setDate(new LiteralValue<Date>(new Date(System.currentTimeMillis()))).
                        addIdentifier(fastRbfNn.getUri().toString());
                fastRbfNn.setMeta(algorithmMeta);
                fastRbfNn.setOntologies(new HashSet<OntologicalClass>());
                fastRbfNn.getOntologies().add(OTAlgorithmTypes.Regression());
                fastRbfNn.getOntologies().add(OTAlgorithmTypes.SingleTarget());
                fastRbfNn.getOntologies().add(OTAlgorithmTypes.EagerLearning());

                fastRbfNn.setParameters(new HashSet<Parameter>());

                Parameter a =
                        new Parameter(
                        Configuration.getBaseUri().augment("prm", "fast_rbf_nn_a"), "a", new LiteralValue(1.0d, XSDDatatype.XSDdouble)).setScope(
                        Parameter.ParameterScope.OPTIONAL);
                a.getMeta().addDescription("Design parameter involved in the caclulation of the initial potential of all vectors of the training set according to the "
                        + "formula P(i)=sum_{j=1}^{K}exp(-a*||x(i)-x(j)||^2) for i=1,2,...,K.");
                fastRbfNn.getParameters().add(a);

                Parameter b =
                        new Parameter(
                        Configuration.getBaseUri().augment("prm", "fast_rbf_nn_b"), "b", new LiteralValue(0.9d, XSDDatatype.XSDdouble)).setScope(
                        Parameter.ParameterScope.OPTIONAL);
                b.getMeta().addDescription("A design parameter that is suggested to be chosen smaller than a to avoid the selection of closely located hidden nodes. This parameter is "
                        + "involved in the formula that defines the potential update in every step of the algorithm, that is P(i) = P(i) - P(L)exp(||x(i)-x*(L)||^2).");
                fastRbfNn.getParameters().add(b);

                Parameter e =
                        new Parameter(
                        Configuration.getBaseUri().augment("prm", "fast_rbf_nn_e"), "e", new LiteralValue(0.6d, XSDDatatype.XSDdouble)).setScope(
                        Parameter.ParameterScope.OPTIONAL);
                e.getMeta().addDescription("Parameter used to implicitly determine the number of iterations and therefore the number hidden nodes the "
                        + "algorithm will find. The algorithm terminates when max_{i}P(i) is less than or equal to e*P*(L)");
                fastRbfNn.getParameters().add(e);
                fastRbfNn.getMeta().addRights(_LICENSE);
                fastRbfNn.setEnabled(true);

            } catch (ToxOtisException ex) {
                throw new RuntimeException(ex);
            }
        }
        return fastRbfNn;
    }

    public static Algorithm scaling() {
        if (scaling == null) {
            try {
                scaling = new Algorithm(Configuration.getBaseUri().augment("algorithm", "scaling"));
                MetaInfo algorithmMeta = new MetaInfoImpl().addTitle("scaling").
                        addSubject("Filter", "Data Preprocessing", "Scaling", "Data Preparation").
                        addContributor("Pantelis Sopasakis", "Charalampos Chomenides").
                        addDescription("There are two ways in which this algorithm can be used. First, clients providing the parameters "
                        + "min and max (which are optional) can the values of a dataset so that every feature accepts values in the interval "
                        + "[min, max]. Alternatively users may provide a 'scaling reference' that is some dataset whose minimum and maximum "
                        + "values per feature are used as guidelines for the scaling. The latter is useful for scaling a dataset for applying it "
                        + "to a model for prediction.").
                        addPublisher(Configuration.BASE_URI).
                        setDate(new LiteralValue<Date>(new Date(System.currentTimeMillis()))).
                        addIdentifier(scaling.getUri().toString());
                scaling.setMeta(algorithmMeta);
                scaling.setOntologies(new HashSet<OntologicalClass>());
                scaling.getOntologies().add(OTAlgorithmTypes.Preprocessing());

                scaling.setParameters(new HashSet<Parameter>());

                Parameter min =
                        new Parameter(
                        Configuration.getBaseUri().augment("prm", "scaling_min"), "min", new LiteralValue(0d, XSDDatatype.XSDdouble)).setScope(
                        Parameter.ParameterScope.OPTIONAL);
                min.getMeta().addDescription("Minimum value for the scaled data");
                scaling.getParameters().add(min);

                Parameter max =
                        new Parameter(
                        Configuration.getBaseUri().augment("prm", "scaling_max"), "max", new LiteralValue(1d, XSDDatatype.XSDdouble)).setScope(
                        Parameter.ParameterScope.OPTIONAL);
                max.getMeta().addDescription("Maximum value for the scaled data");
                scaling.getParameters().add(max);

                Parameter scaling_reference =
                        new Parameter(
                        Configuration.getBaseUri().augment("prm", "scaling_referece"), "scaling_reference", new LiteralValue(null, XSDDatatype.XSDanyURI)).setScope(
                        Parameter.ParameterScope.OPTIONAL);
                scaling_reference.getMeta().addDescription("If a dataset URI is provided, then the scaling is carried out with respect to the minimum and maximum "
                        + "values of the features in that dataset. Used for applying a dataset on a model that requires scaled data.");
                scaling.getParameters().add(scaling_reference);
                scaling.getMeta().addRights(_LICENSE);
                scaling.setEnabled(true);
            } catch (ToxOtisException ex) {
                throw new RuntimeException(ex);
            }
        }
        return scaling;
    }

    public static Algorithm mlr() {
        if (mlr == null) {
            try {
                mlr = new Algorithm(Configuration.getBaseUri().augment("algorithm", "mlr"));
                MetaInfo algorithmMeta = new MetaInfoImpl().addTitle("mlr", "Multiple Linear Regression Training Algorithm").
                        addComment("Multiple Linear Regression Algorithm").
                        addComment("For example cURL commands for this algorithm check out http://cut.gd/P6fa").
                        addSubject("Regression", "Linear", "Training", "Multiple Linear Regression", "Machine Learning", "Single Target", "Eager Learning", "Weka").
                        addContributor("Pantelis Sopasakis", "Charalampos Chomenides").
                        addDescription("Training algorithm for multiple linear regression models. "
                        + "Applies on datasets which contain exclusively numeric data entries. The algorithm is an implementation "
                        + "of LinearRegression of Weka. More information about Linear Regression you will find at "
                        + "http://en.wikipedia.org/wiki/Linear_regression. The weka API for Linear Regression Training is located at "
                        + "http://weka.sourceforge.net/doc/weka/classifiers/functions/LinearRegression.html").
                        addPublisher(Configuration.BASE_URI).
                        setDate(new LiteralValue<Date>(new Date(System.currentTimeMillis()))).
                        addIdentifier(mlr.getUri().toString());
                mlr.setMeta(algorithmMeta);
                mlr.setOntologies(new HashSet<OntologicalClass>());
                mlr.getOntologies().add(OTAlgorithmTypes.Regression());
                mlr.getOntologies().add(OTAlgorithmTypes.SingleTarget());
                mlr.getOntologies().add(OTAlgorithmTypes.EagerLearning());
                mlr.getMeta().addRights(_LICENSE);
                mlr.setEnabled(true);
            } catch (ToxOtisException ex) {
                throw new RuntimeException(ex);
            }
        }
        return mlr;
    }

    public static Algorithm mvh() {
        if (mvh == null) {
            try {
                mvh = new Algorithm(Configuration.getBaseUri().augment("algorithm", "mvh"));
                MetaInfo algorithmMeta = new MetaInfoImpl().addTitle("mvh", "Missing Value Handling Algorithm", "Simple MVH Filter").
                        addComment("You can also use this algorithm from withing the proxy service at " + Configuration.BASE_URI + "/algorithm/multifilter").
                        addComment("For example cURL commands for this algorithm check out http://cut.gd/P6fa").
                        addSubject("Filter", "Data Preprocessing", "Missing Values", "MVH", "Data Preparation", "Weka").
                        addContributor("Pantelis Sopasakis", "Charalampos Chomenides").
                        addDescription("Replaces missing values in the dataset with new ones, leading into a dense dataset using the means-and-modes approach. This "
                        + "action will definitely have effect on the reliability of any model created with the dataset as these values are actually 'guessed' and might "
                        + "strongly divert from the actual ones.").
                        addPublisher(Configuration.BASE_URI).
                        setDate(new LiteralValue<Date>(new Date(System.currentTimeMillis()))).
                        addIdentifier(mvh.getUri().toString());
                Parameter ignoreClass =
                        new Parameter(
                        Configuration.getBaseUri().augment("prm", "ignoreClass"), "ignoreClass", new LiteralValue<Boolean>(false, XSDDatatype.XSDboolean)).setScope(
                        Parameter.ParameterScope.OPTIONAL);
                mvh.setParameters(new HashSet<Parameter>());
                mvh.getParameters().add(ignoreClass);

                mvh.setMeta(algorithmMeta);
                mvh.setOntologies(new HashSet<OntologicalClass>());
                mvh.getOntologies().add(OTAlgorithmTypes.Preprocessing());
                mvh.getMeta().addRights(_LICENSE);
                mvh.setEnabled(true);
            } catch (ToxOtisException ex) {
                throw new RuntimeException(ex);
            }
        }
        return mvh;
    }

    public static Algorithm plsFilter() {
        if (plsFilter == null) {
            try {
                plsFilter = new Algorithm(Configuration.getBaseUri().augment("algorithm", "plsFilter"));
                MetaInfo algorithmMeta = new MetaInfoImpl().addTitle("plsFilter", "Partial Least Squares Filter", "PLS Dataset Preprocessing").
                        addComment("You can also use this algorithm from withing the proxy service at " + Configuration.BASE_URI + "/algorithm/multifilter").
                        addComment("For example cURL commands for this algorithm check out http://cut.gd/P6fa").
                        addSubject("Filter", "Data Preprocessing", "PLS", "Data Preparation", "Weka").
                        addContributor("Pantelis Sopasakis", "Charalampos Chomenides").
                        addDescription("Applies the PLS algorithm on the data and removes some features from the dataset. PLS is a standard, widely used "
                        + "supervised algorithm for dimension reduction on datasets.").
                        addPublisher(Configuration.BASE_URI).
                        setDate(new LiteralValue<Date>(new Date(System.currentTimeMillis()))).
                        addIdentifier(mvh.getUri().toString());
                plsFilter.setParameters(new HashSet<Parameter>());

                Parameter numComponents =
                        new Parameter(
                        Configuration.getBaseUri().augment("prm", "numComponents"), "numComponents", null).setScope(
                        Parameter.ParameterScope.MANDATORY);
                numComponents.getMeta().addDescription("The maximum number of attributes(features) to use");
                plsFilter.getParameters().add(numComponents);

                Parameter plsAlgorithm =
                        new Parameter(
                        Configuration.getBaseUri().augment("prm", "plsAlgorithm"), "algorithm", new LiteralValue<String>("PLS1")).setScope(
                        Parameter.ParameterScope.OPTIONAL);
                plsAlgorithm.getMeta().addDescription("The type of algorithm to use").addComment("Admissible values are PLS1 and SIMPLS");
                plsFilter.getParameters().add(plsAlgorithm);

                Parameter plsPreprocessing =
                        new Parameter(
                        Configuration.getBaseUri().augment("prm", "plsPreprocessing"), "preprocessing", new LiteralValue<String>("center")).setScope(
                        Parameter.ParameterScope.OPTIONAL);
                plsPreprocessing.getMeta().addDescription("Preprocessing on the provided data prior to the application of the PLS algorithm").
                        addComment("Admissible values are 'none', 'center' and 'standardize'");
                plsFilter.getParameters().add(plsPreprocessing);

                Parameter target =
                        new Parameter(
                        Configuration.getBaseUri().augment("prm", "plsTarget"), "target", null).setScope(
                        Parameter.ParameterScope.MANDATORY);
                target.getMeta().addDescription("URI of the target/class feature of the dataset with the respect to which PLS runs");
                plsFilter.getParameters().add(target);

                plsFilter.setMeta(algorithmMeta);
                plsFilter.setOntologies(new HashSet<OntologicalClass>());
                plsFilter.getOntologies().add(OTAlgorithmTypes.Preprocessing());
                plsFilter.getOntologies().add(OTAlgorithmTypes.Supervised());
                plsFilter.getMeta().addRights(_LICENSE);
                plsFilter.setEnabled(true);
            } catch (ToxOtisException ex) {
                throw new RuntimeException(ex);
            }
        }
        return plsFilter;
    }

    public static Algorithm svmFilter() {
        if (svmFilter == null) {
            try {
                svmFilter = new Algorithm(Configuration.getBaseUri().augment("algorithm", "svmFilter"));
                MetaInfo algorithmMeta = new MetaInfoImpl().addTitle("svmFilter", "SVM Filter").
                        addComment("You can also use this algorithm from withing the proxy service at " + Configuration.BASE_URI + "/algorithm/multifilter").
                        addComment("For example cURL commands for this algorithm check out http://cut.gd/P6fa").
                        addSubject("Filter", "Data Preprocessing", "PLS", "Data Preparation", "Weka").
                        addContributor("Pantelis Sopasakis", "Charalampos Chomenides").
                        addDescription("Applies the SVM algorithm on the data and removes some features from the dataset. PLS is a standard, widely used "
                        + "supervised algorithm for dimension reduction on datasets.").
                        addPublisher(Configuration.BASE_URI).
                        setDate(new LiteralValue<Date>(new Date(System.currentTimeMillis()))).
                        addIdentifier(svmFilter.getUri().toString());
                svmFilter.setParameters(new HashSet<Parameter>());



                svmFilter.setMeta(algorithmMeta);
                svmFilter.setOntologies(new HashSet<OntologicalClass>());
                svmFilter.getOntologies().add(OTAlgorithmTypes.Preprocessing());
                svmFilter.getOntologies().add(OTAlgorithmTypes.Supervised());
                svmFilter.setEnabled(true);
                svmFilter.getMeta().addRights(_LICENSE);
            } catch (ToxOtisException ex) {
                throw new RuntimeException(ex);
            }
        }
        return svmFilter;
    }

    public static Algorithm cleanup() {
        if (cleanup == null) {
            try {
                cleanup = new Algorithm(Configuration.getBaseUri().augment("algorithm", "cleanup"));
                MetaInfo algorithmMeta = new MetaInfoImpl().addTitle("cleanup", "Attribute Cleanup Procedure").
                        addComment("You can also use this algorithm from withing the proxy service at " + Configuration.BASE_URI + "/algorithm/multifilter").
                        addComment("For example cURL commands for this algorithm check out http://cut.gd/P6fa").
                        addSubject("Filter", "Data Preprocessing", "Missing Values", "MVH", "Data Preparation").
                        addContributor("Pantelis Sopasakis", "Charalampos Chomenides").
                        addDescription("Removes from the dataset all attributes of certain types. This is useful in many cases, when for example one "
                        + "needs to train a model but have a clean dataset that doesn't include string values").
                        addSeeAlso(new ResourceValue(mvh().getUri(), OTClasses.Algorithm())).
                        addPublisher(Configuration.BASE_URI).
                        setDate(new LiteralValue<Date>(new Date(System.currentTimeMillis()))).
                        addIdentifier(cleanup.getUri().toString());
                Parameter attribute_type =
                        new Parameter(
                        Configuration.getBaseUri().augment("prm", "attribute_type"), "attribute_type", null).setScope(
                        Parameter.ParameterScope.MANDATORY);
                attribute_type.getMeta().addDescription("An array list of attributes that need to be removed from the dataset. "
                        + "Admissible values are 'string', 'nominal' and 'numeric'").
                        addComment("Though HTTP you provide this parameter using brackets. For example, to remove string"
                        + "and numeric attributes the POSTed query would be 'attribute_type&#91;&#93;=string&amp;attribute_type&#91;&#93;=numeric'.");
                cleanup.setParameters(new HashSet<Parameter>());
                cleanup.getParameters().add(attribute_type);
                cleanup.setMeta(algorithmMeta);
                cleanup.setOntologies(new HashSet<OntologicalClass>());
                cleanup.getOntologies().add(OTAlgorithmTypes.Preprocessing());
                cleanup.getMeta().addRights(_LICENSE);
                cleanup.setEnabled(true);
            } catch (ToxOtisException ex) {
                throw new RuntimeException(ex);
            }
        }
        return cleanup;
    }

    public static Algorithm svm() {
        if (svm == null) {
            try {
                svm = new Algorithm(Configuration.getBaseUri().augment("algorithm", "svm"));
                MetaInfo algorithmMeta = new MetaInfoImpl().addTitle("svm", "Support Vector Machine Training Algorithm").
                        addComment("Support Vector Machine Algorithm").
                        addComment("For example cURL commands for this algorithm check out http://cut.gd/P6fa").
                        addSubject("Regression", "Linear", "Training", "Multiple Linear Regression", "Machine Learning", "Single Target", "Eager Learning", "Weka").
                        addContributor("Pantelis Sopasakis", "Charalampos Chomenides").
                        addDescription("Algorithm for training regression models using the Support Vector Machine Learning Algorithm. "
                        + "The training is based on the Weka implementation of SVM and specifically the class weka.classifiers.functions.SVMreg. "
                        + "A comprehensive introductory text is provided by John Shawe-Taylor and Nello Cristianin in the book 'Support Vector Machines' "
                        + "Cambridge University Press, 2000").
                        addPublisher(Configuration.BASE_URI).setDate(new LiteralValue<Date>(new Date(System.currentTimeMillis()))).
                        addIdentifier(svm.getUri().toString());
                svm.setParameters(new HashSet<Parameter>());

                Parameter kernel =
                        new Parameter(
                        Configuration.getBaseUri().augment("prm", "svm_kernel"), "kernel", new LiteralValue<String>("RBF")).setScope(
                        Parameter.ParameterScope.OPTIONAL);
                kernel.getMeta().addDescription("Kernel of the Support Vector Machine. Available kernels include 'rbf', 'linear' and 'polynomial'.").
                        addIdentifier(kernel.getUri().toString());
                svm.getParameters().add(kernel);

                Parameter gamma = new Parameter(Configuration.getBaseUri().augment("prm", "svm_gamma"));
                gamma.setName("gamma").setScope(Parameter.ParameterScope.OPTIONAL);
                gamma.setTypedValue(new LiteralValue<Double>(1.5d));
                gamma.getMeta().
                        addDescription("Gamma Parameter for the SVM kernel").
                        addComment("Only strictly positive values are acceptable").
                        addIdentifier(gamma.getUri().toString());
                svm.getParameters().add(gamma);

                Parameter cost = new Parameter(Configuration.getBaseUri().augment("prm", "svm_cost"));
                cost.setName("cost").setScope(Parameter.ParameterScope.OPTIONAL);
                cost.setTypedValue(new LiteralValue<Double>(100.0d));
                cost.getMeta().addComment("Only strictly positive values are acceptable").
                        addIdentifier(cost.getUri().toString());
                svm.getParameters().add(cost);

                Parameter epsilon = new Parameter(Configuration.getBaseUri().augment("prm", "svm_epsilon"));
                epsilon.setName("epsilon").setScope(Parameter.ParameterScope.OPTIONAL);
                epsilon.setTypedValue(new LiteralValue<Double>(0.1d));
                epsilon.getMeta().addComment("Only strictly positive values are acceptable").
                        addIdentifier(epsilon.getUri().toString());
                svm.getParameters().add(epsilon);

                Parameter tolerance = new Parameter(Configuration.getBaseUri().augment("prm", "svm_tolerance"));
                tolerance.setName("tolerance").setScope(Parameter.ParameterScope.OPTIONAL);
                tolerance.setTypedValue(new LiteralValue<Double>(0.0001d));
                tolerance.getMeta().addComment("Only strictly positive values are acceptable and we advise users to use values that "
                        + "do not exceed 0.10").
                        addIdentifier(tolerance.getUri().toString());
                svm.getParameters().add(tolerance);

                Parameter degree = new Parameter(Configuration.getBaseUri().augment("prm", "svm_degree"));
                degree.setName("degree").setScope(Parameter.ParameterScope.OPTIONAL);
                degree.setTypedValue(new LiteralValue<Integer>(3));
                degree.getMeta().addDescription("Degree of polynomial kernel").
                        addComment("To be used in combination with the polynomial kernel").
                        addIdentifier(degree.getUri().toString());
                svm.getParameters().add(degree);


                svm.setMeta(algorithmMeta);
                svm.setOntologies(new HashSet<OntologicalClass>());
                svm.getOntologies().add(OTAlgorithmTypes.Regression());
                svm.getOntologies().add(OTAlgorithmTypes.SingleTarget());
                svm.getOntologies().add(OTAlgorithmTypes.EagerLearning());
                svm.getMeta().addRights(_LICENSE);
                svm.setEnabled(true);
            } catch (ToxOtisException ex) {
                throw new RuntimeException(ex);
            }
        }
        return svm;
    }

    public static Algorithm svc() {
        if (svc == null) {
            try {
                svc = new Algorithm(Configuration.getBaseUri().augment("algorithm", "svc"));
                MetaInfo algorithmMeta = new MetaInfoImpl().addTitle("svc").
                        addTitle("Support Vector Machine Classification Training Algorithm").
                        addComment("For example cURL commands for this algorithm check out http://cut.gd/P6fa").
                        addSubject("Regression", "Linear", "Training", "Multiple Linear Regression", "Machine Learning", "Single Target", "Eager Learning", "Weka").
                        addContributor("Pantelis Sopasakis", "Charalampos Chomenides").
                        addDescription("Algorithm for training classification models using the Support Vector Machine Learning Algorithm. "
                        + "A comprehensive introductory text is provided by John Shawe-Taylor and Nello Cristianin in the book 'Support Vector Machines' "
                        + "Cambridge University Press, 2000").
                        addPublisher(Configuration.BASE_URI).setDate(new LiteralValue<Date>(new Date(System.currentTimeMillis())));
                svc.setParameters(svm().getParameters());
                svc.setMeta(algorithmMeta);
                svc.setOntologies(new HashSet<OntologicalClass>());
                svc.getOntologies().add(OTAlgorithmTypes.Regression());
                svc.getOntologies().add(OTAlgorithmTypes.SingleTarget());
                svc.getOntologies().add(OTAlgorithmTypes.EagerLearning());
                svc.getMeta().addRights(_LICENSE);
                svc.setEnabled(true);
            } catch (ToxOtisException ex) {
                throw new RuntimeException(ex);
            }
        }
        return svc;
    }

    public static Algorithm leverages() {
        if (leverages == null) {
            try {
                leverages = new Algorithm(Configuration.getBaseUri().augment("algorithm", "leverages"));
                MetaInfo algorithmMeta = new MetaInfoImpl();
                algorithmMeta.addTitle("leverages");
                algorithmMeta.addTitle("Leverages DoA Algorithm");
                algorithmMeta.addSubject("Domain of Applicability", "Model Applicability Domain", "DoA", "MAD").
                        addContributor("Pantelis Sopasakis", "Charalampos Chomenides").
                        addDescription("The well known leverages algorithm for the estimation of a model's applicability domain").
                        addComment("For example cURL commands for this algorithm check out http://cut.gd/P6fa").
                        addPublisher(Configuration.BASE_URI).setDate(new LiteralValue<Date>(new Date(System.currentTimeMillis())));
                leverages.setMeta(algorithmMeta);
                leverages.setOntologies(new HashSet<OntologicalClass>());
                leverages.getOntologies().add(OTAlgorithmTypes.Regression());
                leverages.getOntologies().add(OTAlgorithmTypes.SingleTarget());
                leverages.getOntologies().add(OTAlgorithmTypes.EagerLearning());
                leverages.getMeta().addRights(_LICENSE);
            } catch (ToxOtisException ex) {
                throw new RuntimeException(ex);
            }
        }
        return leverages;
    }
//    public static void main(String... args){
//        System.out.println(Algorithms.forName("null").getUri());
//    }
}

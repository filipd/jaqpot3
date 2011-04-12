package org.opentox.jaqpot3.resources;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.opentox.jaqpot3.exception.JaqpotException;
import org.opentox.jaqpot3.resources.publish.Publisher;
import org.opentox.jaqpot3.util.Configuration;
import org.opentox.jaqpot3.www.URITemplate;
import org.opentox.toxotis.core.component.BibTeX;
import org.opentox.toxotis.database.IDbIterator;
import org.opentox.toxotis.database.engine.DisableComponent;
import org.opentox.toxotis.database.engine.bibtex.FindBibTeX;
import org.opentox.toxotis.database.exception.DbException;
import org.restlet.data.MediaType;
import org.restlet.data.Reference;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.representation.Variant;
import org.restlet.resource.ResourceException;

/**
 *
 * @author Pantelis Sopasakis
 * @author Charalampos Chomenides
 */
public class BibTexResource extends JaqpotResource {

    public static final URITemplate template = new URITemplate("bibtex", "bibtex_id", null);

    public BibTexResource() {
    }

    @Override
    protected void doInit() throws ResourceException {
        super.doInit();
        setAutoCommitting(false);
        initialize(
                MediaType.TEXT_HTML,
                MediaType.APPLICATION_RDF_XML,
                MediaType.register("application/rdf+xml-abbrev", NEWLINE),
                MediaType.APPLICATION_RDF_TURTLE,
                MediaType.APPLICATION_PDF,
                MediaType.TEXT_URI_LIST,
                MediaType.TEXT_RDF_N3,
                MediaType.TEXT_RDF_NTRIPLES,
                MediaType.TEXT_PLAIN);
        acceptString = getRequest().getResourceRef().getQueryAsForm().getFirstValue("accept");
        updatePrimaryId(template);
    }

    @Override
    protected Representation get(Variant variant) throws ResourceException {
        if (acceptString != null) {
            variant.setMediaType(MediaType.valueOf(acceptString));
        }

        FindBibTeX fb = new FindBibTeX(Configuration.getBaseUri().augment("bibtex"));
        System.out.println("Searching for... " + primaryId);
        fb.setSearchById(primaryId);
        IDbIterator<BibTeX> bibtexFound = null;
        BibTeX bibtex = null;
        try {
            bibtexFound = fb.list();
            if (bibtexFound.hasNext()) {
                bibtex = bibtexFound.next();
            }
        } catch (DbException ex) {
            Logger.getLogger(BibTexResource.class.getName()).log(Level.SEVERE, null, ex);
        }finally{
            try {
                bibtexFound.close();
            } catch (DbException ex) {
                Logger.getLogger(BibTexResource.class.getName()).log(Level.SEVERE, null, ex);
            }
            try {
                fb.close();
            } catch (DbException ex) {
                Logger.getLogger(BibTexResource.class.getName()).log(Level.SEVERE, null, ex);
            }
        }


        if (bibtex == null || (bibtex != null && !bibtex.isEnabled())) {
            toggleNotFound();
            return errorReport("BibTeXNotFound", "The bibtex you requested was not found in our database",
                    "The bibtex with id " + primaryId + " was not found in the database",
                    variant.getMediaType(), false);
        }

        Publisher pub = new Publisher(variant.getMediaType());
        try {
            return pub.createRepresentation(bibtex, true);
        } catch (JaqpotException ex) {
            toggleServerError();
            return errorReport("PublicationError", ex.getMessage(), null, variant.getMediaType(), false);
        }

    }

    @Override
    protected Representation delete(Variant variant) throws ResourceException {
        DisableComponent disabler = new DisableComponent(primaryId);
        try {
            int count = disabler.disable();
            return new StringRepresentation(count+" components where disabled.\n", MediaType.TEXT_PLAIN);
        } catch (DbException ex) {
            Logger.getLogger(BibTexResource.class.getName()).log(Level.SEVERE, null, ex);
        }finally{
            try {
                disabler.close();
            } catch (DbException ex) {
                Logger.getLogger(BibTexResource.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return null;
    }
}

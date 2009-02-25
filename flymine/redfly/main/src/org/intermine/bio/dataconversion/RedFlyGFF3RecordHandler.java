package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2009 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.intermine.bio.io.gff3.GFF3Record;
import org.intermine.metadata.Model;
import org.intermine.xml.full.Attribute;
import org.intermine.xml.full.Item;

/**
 * A converter/retriever for the REDfly (http://redfly.ccr.buffalo.edu/) GFF3 files.
 *
 * @author Kim Rutherford
 */

public class RedFlyGFF3RecordHandler extends GFF3RecordHandler
{
    private static final String REDFLY_PREFIX = "REDfly:";
    private Map<String, Item> anatomyMap = new LinkedHashMap<String, Item>();
    private Map<String, Item> geneMap = new HashMap<String, Item>();
    private Map<String, Item> publications = new HashMap<String, Item>();
    protected IdResolverFactory resolverFactory;
    private static final String TAXON_ID = "7227";

    protected static final Logger LOG = Logger.getLogger(RedFlyGFF3RecordHandler.class);

    /**
     * Create a new RedFlyGFF3RecordHandler for the given target model.
     * @param tgtModel the model for which items will be created
     */
    public RedFlyGFF3RecordHandler (Model tgtModel) {
        super(tgtModel);
        // only construct factory here so can be replaced by mock factory in tests
        resolverFactory = new FlyBaseIdResolverFactory("gene");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void process(GFF3Record record) {
        Item feature = getFeature();

        feature.setClassName("CRM");

        String name = record.getId();

        feature.addAttribute(new Attribute("curated", "true"));
        if (record.getAttributes().containsKey("Evidence")) {
            List<String> evidenceList = record.getAttributes().get("Evidence");
            String elementEvidence = evidenceList.get(0);
            feature.addAttribute(new Attribute("evidenceMethod", elementEvidence));
        }

        List<String> ontologyTermIds = record.getAttributes().get("Ontology_term");

        if (ontologyTermIds != null) {
            Iterator<String> ontologyTermIdsIter = ontologyTermIds.iterator();
            List<String> anatomyItems = new ArrayList<String>();

            while (ontologyTermIdsIter.hasNext()) {
                String ontologyTermId = ontologyTermIdsIter.next();
                anatomyItems.add(getAnatomy(ontologyTermId).getIdentifier());
            }

            feature.setCollection("anatomyOntology", anatomyItems);
        }

        String geneName = null;
        String pubmedId = null;
        String redflyID = null;

        List<String> dbxrefs = record.getDbxrefs();

        if (dbxrefs != null) {
            Iterator<String> dbxrefsIter = dbxrefs.iterator();
            while (dbxrefsIter.hasNext()) {
                String dbxref = dbxrefsIter.next();

                int colonIndex = dbxref.indexOf(":");
                if (colonIndex == -1) {
                    throw new RuntimeException("external reference not understood: " + dbxref);
                }

                if (dbxref.startsWith("Flybase:")) {
                    geneName = dbxref.substring(colonIndex + 1);
                } else {
                    if (dbxref.startsWith("PMID:")) {
                        pubmedId = dbxref.substring(colonIndex + 1);
                    } else {
                        if (dbxref.startsWith(REDFLY_PREFIX)) {
                            redflyID = dbxref.substring(colonIndex + 1);
                        } else {
                            throw new RuntimeException("unknown external reference type: "
                                                       + dbxref);
                        }
                    }
                }
            }
        }

        if (geneName == null) {
            throw new RuntimeException("gene name not found when processing " + name
                                       + " found these dbxrefs: " + dbxrefs);
        }
        if (pubmedId == null) {
            throw new RuntimeException("pubmed ID not found when processing " + name
                                       + " found these dbxrefs: " + dbxrefs);
        }
        if (redflyID == null) {
            throw new RuntimeException("REDfly ID not found when processing " + name
                                       + " found these dbxrefs: " + dbxrefs);
        }

        if (geneName.equals("")) {
            geneName = name;
        }

        Item gene = getGene(geneName);
        if (gene != null) {
            feature.setReference("gene", gene);
        }

        if (!pubmedId.equals("")) {
            addPublication(getPublication(pubmedId));
        }

        feature.setAttribute("primaryIdentifier", name);
        feature.setAttribute("secondaryIdentifier", redflyID);
    }

    private Item getGene(String geneId) {
        // try to resolve this id to a current FlyBase identifier
        IdResolver resolver = resolverFactory.getIdResolver();
        int resCount = resolver.countResolutions(TAXON_ID, geneId);
        if (resCount != 1) {
            LOG.info("RESOLVER: failed to resolve gene to one identifier, ignoring gene: "
                     + geneId + " count: " + resCount + " FBgn: "
                     + resolver.resolveId(TAXON_ID, geneId));
            return null;
        }
        String primaryIdentifier = resolver.resolveId(TAXON_ID, geneId).iterator().next();
        Item geneItem = geneMap.get(primaryIdentifier);
        if (geneItem == null) {
            geneItem = getItemFactory().makeItem(null, "Gene", "");
            geneItem.addAttribute(new Attribute("primaryIdentifier", primaryIdentifier));
            geneItem.setReference("organism", getOrganism());
            addItem(geneItem);
            geneMap.put(primaryIdentifier, geneItem);
        }
        return geneItem;
    }

    private Item getAnatomy(String ontologyTermId) {
        if (anatomyMap.containsKey(ontologyTermId)) {
            return anatomyMap.get(ontologyTermId);
        }

        Item anatomyItem = getItemFactory().makeItem(null, "AnatomyTerm", "");
        anatomyItem.addAttribute(new Attribute("identifier", ontologyTermId));
        addItem(anatomyItem);
        anatomyMap.put(ontologyTermId, anatomyItem);
        return anatomyItem;
    }

    /**
     * Return the publication object for the given PubMed id
     *
     * @param pubmedId the PubMed ID
     * @return the publication
     */
    protected Item getPublication(String pubmedId) {
        if (publications.containsKey(pubmedId)) {
            return publications.get(pubmedId);
        }

        Item publicationItem = getItemFactory().makeItem(null, "Publication", "");
        publicationItem.addAttribute(new Attribute("pubMedId", pubmedId));
        addItem(publicationItem);
        publications.put(pubmedId, publicationItem);
        return publicationItem;
    }
}
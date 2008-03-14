package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.intermine.dataconversion.FileConverter;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.FormattedTextParser;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.ReferenceList;

import java.io.Reader;

import org.apache.log4j.Logger;

/**
 * 
 * @author Julie Sullivan
 */
public class Drosophila2ProbeConverter extends FileConverter
{
    protected static final Logger LOG = Logger.getLogger(Drosophila2ProbeConverter.class);

    protected Item dataSource, dataSet, org;
    protected Map<String, Item> bioMap = new HashMap<String, Item>();

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the data model
     * @throws ObjectStoreException if an error occurs in storing
     */
    public Drosophila2ProbeConverter(ItemWriter writer, Model model)
        throws ObjectStoreException {
        super(writer, model);

        dataSource = createItem("DataSource");
        dataSource.setAttribute("name", "Affymetrix");
        store(dataSource);

        dataSet = createItem("DataSet");
        dataSet.setReference("dataSource", dataSource.getIdentifier());
        
        org = createItem("Organism");
        org.setAttribute("taxonId", "7227");
        store(org);
    }


    /**
     * Read each line from flat file.
     *
     * {@inheritDoc}
     */
    public void process(Reader reader) throws Exception {
        
        Iterator<String[]> lineIter = FormattedTextParser.parseCsvDelimitedReader(reader);
        lineIter.next();    // skip header
        boolean hasDataset = false;

        while (lineIter.hasNext()) {
            String[] line = lineIter.next();

            List<Item> delayedItems = new ArrayList<Item>();
            Item probeSet = createProbeSet(line[0], delayedItems);

            String seqType = line[4];

            if (seqType.equalsIgnoreCase("control sequence")) {
                // TODO add a description and flag
                // probeSet.setAttribute("description", line[4]);
                probeSet.setAttribute("isControl", "true");
            }
            
            String cgs = line[17];
            if (!cgs.equals("---")) {
                // set reference to transcript
                Item transcript = createBioEntity("Transcript", line[6]);
                probeSet.setReference("transcript", transcript.getIdentifier());

                // FBgn0029676 /// FBgn0052789 /// FBgn0066138 /// FBgn0066170
                String[] genes = cgs.split(" /// ");
                ReferenceList geneColl = new ReferenceList("genes", new ArrayList<String>());
                for (String identifier : genes) {
                    if (identifier.trim().startsWith("CG")) {
                        Item gene = createBioEntity("Gene", identifier);
                        geneColl.addRefId(gene.getIdentifier());
                    }
                }
                probeSet.addCollection(geneColl);

            }
            store(probeSet);
            for (Item item : delayedItems) {
                store(item);
            }
            if (!hasDataset)  {
                dataSet.setAttribute("title", "Affymetrix array: " + line[1]);
                store(dataSet);
                hasDataset = true;
            }
        }
    }

    private Item createBioEntity(String clsName, String identifier)
        throws ObjectStoreException {
        Item bio = bioMap.get(identifier);
        if (bio == null) {
            bio = createItem(clsName);
            bio.setReference("organism", org.getIdentifier());
            bio.setAttribute("secondaryIdentifier", identifier);            
            bioMap.put(identifier, bio);
            store(bio);
        }
        return bio;
    }

    /**
     * @param clsName target class name
     * @param id identifier
     * @param ordId ref id for organism
     * @param datasourceId ref id for datasource item
     * @param datasetId ref id for dataset item
     * @param writer itemWriter write item to objectstore
     * @return item
     * @throws exception if anything goes wrong when writing items to objectstore
     */
    private Item createProbeSet(String probeSetId, List<Item> delayedItems) {
        Item probeSet = createItem("ProbeSet");
        probeSet.setAttribute("primaryIdentifier", probeSetId);
        probeSet.setAttribute("name", probeSetId);
        probeSet.setReference("organism", org.getIdentifier());
        probeSet.setCollection("evidence",
            new ArrayList(Collections.singleton(dataSet.getIdentifier())));

        Item synonym = createItem("Synonym");
        synonym.setAttribute("type", "identifier");
        synonym.setAttribute("value", probeSetId);
        synonym.setReference("source", dataSource.getIdentifier());
        synonym.setReference("subject", probeSet.getIdentifier());
        delayedItems.add(synonym);

        return probeSet;
    }

    // not used
//    private Item createChromosome(String chrId) throws ObjectStoreException {
//        Item chr = (Item) chrMap.get(chrId);
//        if (chr == null) {
//            chr = createItem("Chromosome");
//            String primaryIdentifier = null;
//            // convert 'arm_2L' -> '2L'
//            if (chrId.contains("_")) {
//                String[] s = chrId.split("_");
//                primaryIdentifier = s[1];
//            } else {
//                primaryIdentifier = chrId;
//            }
//            chr.setAttribute("primaryIdentifier", primaryIdentifier);
//            chr.setReference("organism", org.getIdentifier());
//            chrMap.put(chrId, chr);
//            store(chr);
//        }
//        return chr;
//    }
}

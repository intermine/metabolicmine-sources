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

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.intermine.dataconversion.ItemsTestCase;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.metadata.Model;
import org.intermine.xml.full.ItemFactory;

/**
 * Test for data from miRanda
 *
 * @author "Xavier Watkins"
 *
 */
public class MirandaConverterTest extends ItemsTestCase
{
    MirandaGFF3RecordHandler handler;

    private Model tgtModel;
    private GFF3Converter converter;
    private MockItemWriter writer = new MockItemWriter(new LinkedHashMap());
    private String seqClsName = "Chromosome";
    private String taxonId = "DM";
    private String dataSourceName = "Sanger Institute";
    private String dataSetTitle = "miRanda";

    public MirandaConverterTest(String arg) {
        super(arg);
    }

    protected void setUp() throws Exception {
        super.setUp();
        tgtModel = Model.getInstanceByName("genomic");
        handler = new MirandaGFF3RecordHandler(tgtModel);
        MockIdResolverFactory resolverFactory = new MockIdResolverFactory("Gene");
        resolverFactory.addResolverEntry("7227", "FBgn001", Collections.singleton("mir-92b"));
        resolverFactory.addResolverEntry("7227", "FBgn002", Collections.singleton("mir-312"));
        handler.resolverFactory = resolverFactory;
        converter = new GFF3Converter(writer, seqClsName, taxonId, dataSourceName,
                                      dataSetTitle, "FlyBase", tgtModel, handler, null);
    }

    protected void tearDown() throws Exception {
        converter.close();
    }

    public void testMirandaHandler() throws Exception {
        String gff =
            "3R\tmiRanda\tmiRNA_target\t9403\t9424\t16.9418\t+\t.\ttarget=CG11023-RA;pvalue=3.057390e-02;Name=dme-miR-312;ID=AAA;"
                     + ENDL
                     + "3R\tmiRanda\tmiRNA_target\t9403\t9424\t17.7377\t+\t.\ttarget=CG11023-RA;pvalue=1.179130e-02;Name=dme-miR-92b;ID=BBB"
                     + ENDL;

        BufferedReader srcReader = new BufferedReader(new StringReader(gff));
        converter.parse(srcReader);
        converter.store();

        // uncomment to write a new tgt items file
        //writeItemsFile(writer.getItems(), "miranda-tgt-items.xml");

        Set expected = readItemSet("miranda-tgt-items.xml");
        assertEquals(expected, writer.getItems());
    }

}

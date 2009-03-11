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

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;

import org.intermine.dataconversion.ItemsTestCase;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.metadata.Model;

public class BioGridHumanConverterTest extends ItemsTestCase
{

    Model model = Model.getInstanceByName("genomic");
    BioGridHumanConverter converter;
    MockItemWriter itemWriter;

    public BioGridHumanConverterTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();
        itemWriter = new MockItemWriter(new HashMap());
        converter = new BioGridHumanConverter(itemWriter, model);
        MockIdResolverFactory resolverFactory = new MockIdResolverFactory("Gene");
        resolverFactory.addResolverEntry("7227", "FBgn001", Collections.singleton("FBgn001"));
        resolverFactory.addResolverEntry("7227", "FBgn003", Collections.singleton("FBgn002"));
        converter.resolverFactory = resolverFactory;
    }

    public void testProcess() throws Exception {

        Reader reader =
            new InputStreamReader(getClass().getClassLoader().getResourceAsStream("test.tab"));
        converter.process(reader);
        converter.close();

        // uncomment to write out a new target items file
        //writeItemsFile(itemWriter.getItems(), "BioGridHuman_test.xml");

        //Set expected = readItemSet("UniprotConverterTest_tgt.xml");

        //assertEquals(expected, itemWriter.getItems());
    }
}
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.inria.wimmics.coresetimer;

import fr.inria.acacia.corese.exceptions.EngineException;
import fr.inria.corese.rdftograph.RdfToGraph;
import fr.inria.edelweiss.kgram.core.Mappings;
import fr.inria.edelweiss.kgraph.core.Graph;
import fr.inria.edelweiss.kgraph.query.QueryProcess;
import fr.inria.edelweiss.kgtool.load.Load;
import fr.inria.edelweiss.kgtool.load.LoadException;
import fr.inria.edelweiss.kgtool.print.ResultFormat;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author edemairy
 */
public class CoreseAdapter {
    private final static Logger logger = Logger.getLogger(CoreseAdapter.class.getName());

    private Mappings map;
    private QueryProcess exec;

    public void preProcessing(String fileName, boolean load) throws IOException, LoadException {
        logger.log(Level.INFO, "using {0}", fileName);
        Graph graph = Graph.create(false); // false = without rdfs.entailment
        if (load) {
            InputStream input = RdfToGraph.makeStream(fileName);
            Load ld = Load.create(graph);
            ld.parse(input, RdfToGraph.getCoreseRdfFormat(fileName));
        }
        exec = QueryProcess.create(graph);
    }

    public void execQuery(String query) {
        try {
            map = exec.query(query);
        } catch (EngineException ex) {
            logger.log(Level.SEVERE, "Exception when attempting to execute a query: ", ex);
        }
    }

    public void saveResults(String resultsFileName) {
        ResultFormat formattedResult = ResultFormat.create(map);
        FileWriter output;
        try {
            output = new FileWriter(resultsFileName);
            output.append(formattedResult.toString());
            output.close();
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Exception when trying to save results in " + resultsFileName, ex);
        }
    }

    public void postProcessing() {
        exec.close();
    }

    public Mappings getMappings() {
        return map;
    }
}

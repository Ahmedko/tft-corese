package fr.inria.edelweiss.kgraph.approximate.algorithm;

import fr.inria.edelweiss.kgraph.approximate.strategy.AlgType;
import fr.inria.edelweiss.kgraph.approximate.strategy.ApproximateStrategy;
import fr.inria.edelweiss.kgraph.approximate.algorithm.impl.CombinedAlgorithm;
import fr.inria.edelweiss.kgraph.approximate.algorithm.impl.Equality;
import fr.inria.edelweiss.kgraph.approximate.algorithm.impl.JaroWinkler;
import fr.inria.edelweiss.kgraph.approximate.algorithm.impl.NGram;
import fr.inria.edelweiss.kgraph.approximate.algorithm.impl.wn.NLPHelper;
import fr.inria.edelweiss.kgraph.approximate.algorithm.impl.wn.TextSimilarity;
import fr.inria.edelweiss.kgraph.approximate.strategy.Priority;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Generate instance of similarity measurement algorithm
 *
 * @author Fuqi Song, Wimmics Inria I3S
 * @date 23 sept. 2015
 */
public class SimAlgorithmFactory {

    public static ISimAlgorithm create(String name) {
        AlgType alg = ApproximateStrategy.valueOf(name);
        return alg == null ? null : create(alg);
    }

    /**
     * Create an instance of algorithm using the given type of algorithm
     *
     * @param type
     * @return
     */
    public static ISimAlgorithm create(AlgType type) {
        switch (type) {

            //**N-Gram
            case ng:
                return new NGram();
            case eq:
                return new Equality();
            case jw:
                return new JaroWinkler();
            case wn:
                try {
                    return new TextSimilarity(NLPHelper.createInstance());
                } catch (Exception ex) {
                    Logger.getLogger(SimAlgorithmFactory.class.getName()).log(Level.WARNING, "** Cannot initialize NLP helper, WordNet similarity algorithms are disabled!**");
                }
                return null;
            case ch:
            //integrate the old algorithm
            //return new ClassHieararchy(alg);
            //case dr:
            //return new DomainRange(alg);
            //case mult:
            default:
                //return new BaseAlgorithm(alg);
                return null;
        }
    }

    /**
     * Generate a combined similarity measurement algorithm
     *
     * @param algs
     * @param defWeights
     * @return
     */
    public static ISimAlgorithm createCombined(String algs, boolean defWeights) {
        return createCombined(ApproximateStrategy.getAlgrithmList(algs), defWeights);
    }

    /**
     * Create a combined similarity measurement algorithm
     *
     * @param algs
     * @param defWeights
     * @return
     */
    public static ISimAlgorithm createCombined(List<AlgType> algs, boolean defWeights) {
        List<ISimAlgorithm> algList = new LinkedList<ISimAlgorithm>();

        for (AlgType at : algs) {
            ISimAlgorithm alg = create(at);
            if (alg != null) {
                algList.add(alg);
            }
        }

        double[] weights = defWeights ? Priority.getDefaultWeights(algList.size()) : Priority.getWeightByAlgorithm(algList);
        return new CombinedAlgorithm(algList, weights);
    }
}

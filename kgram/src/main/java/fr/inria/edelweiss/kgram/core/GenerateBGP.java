/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.inria.edelweiss.kgram.core;

import fr.inria.edelweiss.kgram.api.core.Edge;
import static fr.inria.edelweiss.kgram.api.core.ExpType.AND;
import static fr.inria.edelweiss.kgram.api.core.ExpType.BGP;
import static fr.inria.edelweiss.kgram.api.core.ExpType.JOIN;
import static fr.inria.edelweiss.kgram.api.core.ExpType.UNION;
import fr.inria.edelweiss.kgram.api.core.Node;
import fr.inria.edelweiss.kgram.api.query.Producer;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.apache.log4j.Logger;

/**
 *
 *
 * @author Abdoul Macina <macina@i3s.unice.fr>
 */
public class GenerateBGP extends Sorter {

    private Logger logger = Logger.getLogger(GenerateBGP.class);

    private Exp exp;
    private Exp newExp;

    private HashMap<Edge, ArrayList<Producer>> indexEdgeProducers;
    private HashMap<Edge, ArrayList<Node>> indexEdgeVariables;
    private List<Producer> producers;
    private List<Edge> joinedEdges;
    private List<Edge> isolatedEdges;

    public GenerateBGP() {
        this.producers = new ArrayList<Producer>();
        this.indexEdgeProducers = new HashMap<Edge, ArrayList<Producer>>();
        this.indexEdgeVariables = new HashMap<Edge, ArrayList<Node>>();
        joinedEdges = new ArrayList<Edge>();
        isolatedEdges = new ArrayList<Edge>();
    }

    /**
     * Check if data are horizontally partitioned
     *
     * @return
     */
    public boolean isHorizontal() {

        boolean result = true;
        for (int i = 0; i < exp.getExpList().size() && result; i++) {
            Edge e = exp.getExpList().get(i).getEdge();
            if (indexEdgeProducers.get(e).size() < 2) {
                result = false;
            }
        }
        return result;
    }

    /**
     * Check if data are vertically partitioned
     *
     * @return
     */
    public boolean isVertical() {

        boolean result = true;
        for (int i = 0; i < exp.getExpList().size() && result; i++) {
            Edge e = exp.getExpList().get(i).getEdge();
            if ((indexEdgeProducers.get(e) != null) && (indexEdgeProducers.get(e).size() > 1)) {
                result = false;
            }
        }
        return result;
    }

    /**
     * Build BGP expressions depending on the data partition
     *
     * @return
     */
    public Exp buildBGP() {
        if (isVertical()) {
            exp.setType(BGP);
            return exp;
        }
        if (isHorizontal()) {
            return bgpGlobalStrategy();
        }
        return exp;
    }

    /**
     * Handle the different cases we might have depending the distribution of
     * data
     *
     * @return
     */
    public Exp bgpGlobalStrategy() {
        HashMap<Producer, ArrayList<Edge>> indexProducerEdges = buildIndexProducerEdges();
        Exp join = Exp.create(JOIN);
        ArrayList<Edge> edges;

        //All edges might be found in all producers
        if (compareAllProducers()) {
//            logger.info(" CASE 1 ");
            //transforming the first one is enough
            edges = indexProducerEdges.get(producers.get(0));
            createUnionBGPANDLock(edges);
            return newExp;
        } else {
//                logger.info(" CASE 2 " + exp);   
            for (int i = 0; i < producers.size(); i++) {
                Producer p = producers.get(i);
                edges = indexProducerEdges.get(p);
                //generate partial BGP for each source in which join is possible
                if (possiblePerformJoins(edges)) {
                    String key = createUnionBGPANDLock(edges);
                    if (i > 0) {
                        if (!key.isEmpty()) {
                            join = new Exp(JOIN, join, newExp);
                        }
                    } else {
                        join = newExp;
                    }
                } else {
                    for (Edge e : edges) {
                        if (!isolatedEdges.contains(e)) {
                            isolatedEdges.add(e);
                        }
                    }
                }
            }
//          Add isolated edges
            if (isolatedEdges.size() > 0) {
                isolatedEdges.removeAll(joinedEdges);
                for (Edge e : isolatedEdges) {
                    join = new Exp(JOIN, join, Exp.create(BGP, e));
                }
            }
            return join;
        }
    }

    /**
     * Transforms a AND expression (list of edges) to an equivalent BGP and
     * ANDlock BGP will only process the edges in the same source AND lock will
     * only process distrubted edges
     *
     * @param edges
     * @return
     */
    public String createUnionBGPANDLock(ArrayList<Edge> edges) {
        newExp = Exp.create(UNION);
        String key = "";
        Exp bgp = new Exp(BGP);
        Exp andLock = new Exp(AND);
        for (Edge e : edges) {
            if (!joinedEdges.contains(e)) {
                bgp.add(e);
                andLock.add(e);
                key += e.getLabel();
                joinedEdges.add(e);

                //Update disjoined edges
                if (isolatedEdges.contains(e)) {
                    isolatedEdges.remove(e);
                }
            }
        }
        andLock.setLock(true);
        //to avoid BGP U AND lock with one edge
        if (bgp.size() == 1) {
            newExp = bgp;
        } //AND = UNION (BGP, AND lock)
        else {
            newExp.add(bgp);
            newExp.add(andLock);
        }

        return key;
    }

    /**
     * Check if all producer lists have the same predicate by comparing them by
     * pairs
     *
     * If it is the case: a unique BGP expression will be generated
     *
     * @return
     */
    public boolean compareAllProducers() {
        boolean res = true;
        for (int i = 0; i + 1 < exp.getExpList().size() && res; i++) {
            Edge e1 = exp.getExpList().get(i).getEdge();
            Edge e2 = exp.getExpList().get(i + 1).getEdge();
            res = compare2Producers(indexEdgeProducers.get(e1), indexEdgeProducers.get(e2));
        }
        return res;
    }

    /**
     * Check if two producer lists are the same => they will generate the same
     * BGP expression
     *
     * @param list1
     * @param list2
     * @return
     */
    public boolean compare2Producers(ArrayList<Producer> list1, ArrayList<Producer> list2) {
        ArrayList<Producer> tmp1 = (ArrayList<Producer>) list1.clone();
        ArrayList<Producer> tmp2 = (ArrayList<Producer>) list2.clone();

        tmp1.removeAll(list2);
        tmp2.removeAll(list1);

        return (tmp1.size() == tmp2.size()) && (tmp1.isEmpty());

    }

    /**
     * Check if all lists of Nodes share a common variable by pairs
     *
     * @param edges
     * @return
     */
    public boolean possiblePerformJoins(ArrayList<Edge> edges) {
        boolean res = true;
        for (int i = 0; i + 1 < edges.size() && res; i++) {
            Edge e1 = edges.get(i);
            Edge e2 = edges.get(i + 1);
            res = compare2VariableLists(indexEdgeVariables.get(e1), indexEdgeVariables.get(e2));
        }

        return res;
    }

    /**
     * Check if two lists of Nodes share a common variable
     *
     * If it is the case, it means a join might be performed
     *
     * @param list1
     * @param list2
     * @return
     */
    public boolean compare2VariableLists(ArrayList<Node> list1, ArrayList<Node> list2) {
        ArrayList<Node> tmp1 = (ArrayList<Node>) list1.clone();
        tmp1.removeAll(list2);
        boolean result = !tmp1.containsAll(list1);
        return result;
    }

    /**
     * Build the producerEdges index needed to generate BGP
     *
     * @return
     */
    public HashMap<Producer, ArrayList<Edge>> buildIndexProducerEdges() {
        HashMap<Producer, ArrayList<Edge>> indexProducerEdges = new HashMap<Producer, ArrayList<Edge>>();
        ArrayList<Producer> producersList;
        ArrayList<Edge> edges;

        for (int i = 0; i < exp.getExpList().size(); i++) {
            Edge e = exp.getExpList().get(i).getEdge();
            producersList = indexEdgeProducers.get(e);

            for (Producer p : producersList) {
                if (!producers.contains(p)) {
                    producers.add(p);
                }
                if (indexProducerEdges.containsKey(p)) {
                    edges = indexProducerEdges.get(p);
                    edges.add(e);
                    indexProducerEdges.put(p, edges);
                } else {
                    edges = new ArrayList<Edge>();
                    edges.add(e);
                    indexProducerEdges.put(p, edges);
                }
            }
        }
        return indexProducerEdges;
    }

    void setExp(Exp exp) {
        this.exp = exp;
    }

    public HashMap<Edge, ArrayList<Producer>> getIndexEdgeProducers() {
        return indexEdgeProducers;
    }

    public void setIndexEdgeProducers(HashMap<Edge, ArrayList<Producer>> indexEdgeProducers) {
        this.indexEdgeProducers = indexEdgeProducers;
    }

    public HashMap<Edge, ArrayList<Node>> getIndexEdgeVariables() {
        return indexEdgeVariables;
    }

    public void setIndexEdgeVariables(HashMap<Edge, ArrayList<Node>> indexEdgeVariables) {
        this.indexEdgeVariables = indexEdgeVariables;
    }
}

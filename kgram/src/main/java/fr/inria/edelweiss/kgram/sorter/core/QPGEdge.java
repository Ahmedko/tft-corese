package fr.inria.edelweiss.kgram.sorter.core;

import fr.inria.edelweiss.kgram.sorter.impl.qpv1.QPGEdgeWeightModel;
import java.util.ArrayList;
import java.util.List;

/**
 * Basic Query triple Pattern Graph edge used to connect two QPG nodes
 *
 * @author Fuqi Song, Wimmics Inria I3S
 * @date 27 juin 2014
 */
public class QPGEdge {

    public final static int BI_DIRECT = 10;
    public final static int SIMPLE = 20;
    
    private final QPGNode n1;
    private final QPGNode n2;
    private AbstractCostModel costModel = null;
    private double cost = -1;
    //private boolean directed = false;
    private List<String> variables = null;
    private int type;

    public QPGEdge(QPGNode n1, QPGNode n2) {
        this.n1 = n1;
        this.n2 = n2;

        //set list of shared vairables
        this.variables = n1.shared(n2);
        this.costModel = new QPGEdgeWeightModel(this);
    }

    public double getCost() {
        return cost;
    }

    public void setCost(double cost) {
        this.cost = cost;
    }

    public AbstractCostModel getCostModel() {
        return costModel;
    }

    public List<String> getVariables() {
        return variables;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    
    /**
     * Get one of the nodes in the edge
     *
     * @param in
     * @return
     */
    public QPGNode get(int in) {
        return in == 0 ? n1 : (in == 1 ? n2 : null);
    }

    /**
     * Get the other node in the edge
     *
     * @param n
     * @return
     */
    public QPGNode get(QPGNode n) {
        return n1.equals(n) ? n2 : n1;
    }

    /**
     * Get the two nodes in a list
     *
     * @return
     */
    public List<QPGNode> getNodes() {
        List l = new ArrayList();
        l.add(n1);
        l.add(n2);
        return l;
    }

    @Override
    public String toString() {
        return "{ " + n1 + ", " + n2 + ", " + cost + ", " + type + '}';
    }
}
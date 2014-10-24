package fr.inria.edelweiss.kgram.sorter.core;

/**
 * Interface for calling Producer from kgraph, these methods are implemented
 * without the needs of stats data, only depending on the structure/index of
 * graph
 *
 * @author Fuqi Song, WImmics Inria I3S
 * @date 4 juin 2014
 */
public interface IProducerQP {

    public static final int ALL = 0;
    public static final int SUBJECT = 1;
    public static final int PREDICATE = 2;
    public static final int OBJECT = 3;
    public static final int TRIPLE = 4;
    public static final int NA = -1;


    /**
     * Return the size of triples according to different types
     * 
     * @param type: ALL, return the number of all triples in the graph
     *              SUBJECT( PREDICATE| OBJECT): return the number of disinct SUBJECT( PREDICATE| OBJECT)
     * @return 
     */
    public int getSize(int type);

    /**
     * Return the count of triples according the given type
     * @param n
     * @param type
     * @return 
     */
    public int getCount(QPGNode n, int type);
}
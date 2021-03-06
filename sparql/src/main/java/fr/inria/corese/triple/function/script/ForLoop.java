package fr.inria.corese.triple.function.script;

import fr.inria.acacia.corese.api.Computer;
import fr.inria.acacia.corese.api.IDatatype;
import fr.inria.acacia.corese.cg.datatype.DatatypeMap;
import fr.inria.acacia.corese.triple.parser.Expression;
import fr.inria.acacia.corese.triple.parser.Processor;
import fr.inria.acacia.corese.triple.parser.Term;
import fr.inria.acacia.corese.triple.parser.Variable;
import fr.inria.corese.triple.function.term.Binding;
import fr.inria.edelweiss.kgram.api.query.Environment;
import fr.inria.edelweiss.kgram.api.query.Producer;

/**
 *
 * @author Olivier Corby, Wimmics INRIA I3S, 2015
 *
 */
public class ForLoop extends Statement {
    
    static final IDatatype TRUE = DatatypeMap.TRUE;

    public ForLoop(Expression var, Expression exp, Expression body) {
        super(Processor.FOR, var, exp);
        add(body);
    }
    
    @Override
    public StringBuffer toString(StringBuffer sb) {         
        sb.append(Processor.FOR);
        sb.append(" (");
        getArg(0).toString(sb);
        sb.append(" ");       
        sb.append(Processor.IN);
        sb.append(" ");       
        getArg(1).toString(sb);
        sb.append(")");
        sb.append(" {");
        sb.append(Term.NL);
        sb.append("  ");
        getArg(2).toString(sb);
        sb.append(Term.NL);
        sb.append("}");
        return sb;
    }  
    
     @Override
     public ForLoop getFor(){
         return this;
     }
     

    @Override
    public Variable getVariable() {
        return getBasicArg(0).getVariable();
    }

    @Override
    public Expression getDefinition() {
        return getBasicArg(1);
    }

    @Override
    public Expression getBody() {
        return getBasicArg(2);
    }
    
    
    @Override
    public IDatatype eval(Computer eval, Binding b, Environment env, Producer p) {
        IDatatype list = getDefinition().eval(eval, b, env, p);
        if (list == null) {
            return null;
        }
        Variable var = getVariable();
        Expression body = getBody();
        IDatatype res = null;
        b.set(this, var, TRUE);

        if (list.isList()) {
            for (IDatatype dt : list.getValues()) {
                b.bind(this, var, dt);
                res = body.eval(eval, b, env, p);
                if (b.isResult()) { //if (isReturn(res)) {
                    b.unset(this, var, dt);
                    return res;
                }
            }
        } else {
            for (IDatatype dt : list) {
                b.bind(this, var, dt);
                res = body.eval(eval, b, env, p);
                if (b.isResult()) { //if (isReturn(res)) {
                    b.unset(this, var, dt);
                    return res;
                }
            }
        }

        b.unset(this, var, TRUE);
        return TRUE;
    }
   
    
}

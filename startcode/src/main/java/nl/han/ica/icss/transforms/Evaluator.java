package nl.han.ica.icss.transforms;

import nl.han.ica.datastructures.HANLinkedList;
import nl.han.ica.datastructures.IHANLinkedList;
import nl.han.ica.icss.ast.*;

import java.util.ArrayList;
import java.util.HashMap;

public class Evaluator implements Transform {

    private IHANLinkedList<HashMap<String, Literal>> scopes = new HANLinkedList<>();

    @Override
    public void apply(AST ast) {

        //eerst ff een guard
        if (ast == null || ast.root == null) return;
        scopes = new HANLinkedList<>();
        push(); // pusht een nieuwe scope op de stack
        simplify(ast.root); // begin evaluatie
        pop(); // wanneer evaluatie klaar is
    }

    private void simplify(ASTNode node) {

        boolean opened = isScopeNode(node);
        if (opened) push();

        simplifyChildren(node);
        if (opened) pop();
    }


    private boolean isScopeNode(ASTNode node) {
        return (node instanceof Stylesheet) || (node instanceof Stylerule);
    }


    private void push() {
        scopes.addFirst(new HashMap<String, Literal>());
    }

    private void pop() {
        scopes.removeFirst();
    }

    private HashMap<String, Literal> current() {
        return scopes.getFirst();
    }

    private Literal resolve(String name) {
        for (int i = 0; i < scopes.getSize(); i++) {
            HashMap<String, Literal> scope = scopes.get(i);
            if (scope != null && scope.containsKey(name)) {
                return scope.get(name);
            }
        }
        return null;
    }

    private Literal eval(Expression e) {
        if (e == null) return null;
        if (e instanceof Literal) {
            return (Literal) e;
        }
        if (e instanceof VariableReference) {
            VariableReference vr = (VariableReference) e;
            return resolve(vr.name);
        }
        return null;
    }

    // experssie omzetten naar een literal
    private void reduceDecl(Declaration d) {
        if (d.expression == null) return;
        Literal lit = eval(d.expression);
        if (lit == null) return;
        //children van declaration vervangen door literal child
        for (ASTNode child : d.getChildren()) d.removeChild(child);
        d.addChild(lit);
    }

    private void simplifyChildren(ASTNode node) {
        ArrayList<ASTNode> children = new ArrayList<>(node.getChildren());
        for (ASTNode child : children) {

            // variableAssignment: evalueren, in scope zetten, en verwijderen
            if (child instanceof VariableAssignment) {
                VariableAssignment va = (VariableAssignment) child;
                Literal val = eval(va.expression);
                if (val != null) current().put(va.name.name, val);
                node.removeChild(va);
                continue;
            }
            //declaration: expression  naar literal
            if (child instanceof Declaration) {
                reduceDecl((Declaration) child);
                continue;
            }

            // 3) Anders: recurseer
            simplify(child);
        }
    }
}





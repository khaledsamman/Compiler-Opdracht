package nl.han.ica.icss.transforms;

import nl.han.ica.datastructures.HANLinkedList;
import nl.han.ica.datastructures.IHANLinkedList;
import nl.han.ica.icss.ast.*;
import nl.han.ica.icss.ast.literals.BoolLiteral;
import nl.han.ica.icss.ast.literals.PercentageLiteral;
import nl.han.ica.icss.ast.literals.PixelLiteral;
import nl.han.ica.icss.ast.literals.ScalarLiteral;
import nl.han.ica.icss.ast.operations.AddOperation;
import nl.han.ica.icss.ast.operations.MultiplyOperation;
import nl.han.ica.icss.ast.operations.SubtractOperation;

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
        pop(); // sluit wanneer evaluatie klaar is
    }


    // check of node een nieuwe scope moet krijgen. bijv. stylesheet of styleRule. if ja -> maak nieuwe 'doosje'
    // vervolgens door de children lopen
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

    // evalueer expressie tot een literal
    // als de expressie een literal is dan geef die dat gewoon terug
    // als het een variabele is, wordt we waarden opgezocht via resolve()
    // als het een operation is, bereken de linker en rechterkand met eval() en combineer
    // [TR01] eval() rekent expressies uit; reduceDecl() vervangt Declaration.expression door berekende Literal.
    private Literal eval(Expression e) {
        if (e == null) return null;
        if (e instanceof Literal) {
            return (Literal) e;
        }
        if (e instanceof VariableReference) {
            VariableReference vr = (VariableReference) e;
            return resolve(vr.name);
        }
        if (e instanceof AddOperation) {
            AddOperation op = (AddOperation) e;
            Literal left = eval(op.lhs);
            Literal right = eval(op.rhs);
            return addLiterals(left, right);
        }
        if (e instanceof SubtractOperation) {
            SubtractOperation op = (SubtractOperation) e;
            Literal left = eval(op.lhs);
            Literal right = eval(op.rhs);
            return subtractLiterals(left, right);
        }
        if (e instanceof MultiplyOperation) {
            MultiplyOperation op = (MultiplyOperation) e;
            Literal left = eval(op.lhs);
            Literal right = eval(op.rhs);
            return multiplyLiterals(left, right);
        }
        return new ScalarLiteral(0);
    }
    // tel twee literals op
    private Literal addLiterals(Literal left, Literal right) {
        if (left instanceof PixelLiteral && right instanceof PixelLiteral)
            return new PixelLiteral(((PixelLiteral) left).value + ((PixelLiteral) right).value);
        if (left instanceof PercentageLiteral && right instanceof PercentageLiteral)
            return new PercentageLiteral(((PercentageLiteral) left).value + ((PercentageLiteral) right).value);
        if (left instanceof ScalarLiteral && right instanceof ScalarLiteral)
            return new ScalarLiteral(((ScalarLiteral) left).value + ((ScalarLiteral) right).value);
        return new ScalarLiteral(0); // ongeldig typecombinatie
    }

    // trek twee literals af
    private Literal subtractLiterals(Literal left, Literal right) {
        if (left instanceof PixelLiteral && right instanceof PixelLiteral)
            return new PixelLiteral(((PixelLiteral) left).value - ((PixelLiteral) right).value);
        if (left instanceof PercentageLiteral && right instanceof PercentageLiteral)
            return new PercentageLiteral(((PercentageLiteral) left).value - ((PercentageLiteral) right).value);
        if (left instanceof ScalarLiteral && right instanceof ScalarLiteral)
            return new ScalarLiteral(((ScalarLiteral) left).value - ((ScalarLiteral) right).value);
        return new ScalarLiteral(0);
    }

    // vermenigvuldig twee literals
    //Scalar * Pixel -> Pixel
    //Pixel * Scalar -> Pixel
    //Scalar * Percentage -> Percentage
    //Percentage * Scalar -> Percentage
    //Scalar * Scalar -> Scalar
    private Literal multiplyLiterals(Literal left, Literal right) {
        if (left instanceof ScalarLiteral && right instanceof PixelLiteral)
            return new PixelLiteral(((ScalarLiteral) left).value * ((PixelLiteral) right).value);
        if (right instanceof ScalarLiteral && left instanceof PixelLiteral)
            return new PixelLiteral(((ScalarLiteral) right).value * ((PixelLiteral) left).value);
        if (left instanceof ScalarLiteral && right instanceof PercentageLiteral)
            return new PercentageLiteral(((ScalarLiteral) left).value * ((PercentageLiteral) right).value);
        if (right instanceof ScalarLiteral && left instanceof PercentageLiteral)
            return new PercentageLiteral(((ScalarLiteral) right).value * ((PercentageLiteral) left).value);
        if (left instanceof ScalarLiteral && right instanceof ScalarLiteral)
            return new ScalarLiteral(((ScalarLiteral) left).value * ((ScalarLiteral) right).value);
        return new ScalarLiteral(0);
    }

    // experssie omzetten naar een literal
    //neemt iets zoasl width: ParWidth + 20px;
    // roept eval() aan, die berekent het resultaat. dus als bijv. parwidt 500px. wordt width 520px;
    // vervolgens haalt die oude expressie weg en zet die het nieuwe literal erin
    private void reduceDecl(Declaration d) {
        if (d.expression == null) return;
        Literal lit = eval(d.expression);
        if (lit == null) return;
        //children van declaration vervangen door literal child
        for (ASTNode child : d.getChildren()) d.removeChild(child);
        d.addChild(lit);
    }

    //loopt door alle kinderen van huidige node
    // als het child een variabele is: bereken de waarde -> sla waarde in huidige scope op -> verwijder variabele uit de AST
    // als het child een declaratie is: rope reduceDecl() -> berekent echte waarde. anders is het waarschijnlijk iets als een nieuwe stylerule of ifclause dus ga daar recursief in verder.
    // [TR01][TR02] Beide transformaties in een boomtraversal (scopes + evaluatie + if eliminatie).
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
            //if else
            if (child instanceof IfClause) {
                IfClause ifc = (IfClause) child;

                Literal condLit = eval(ifc.getConditionalExpression());
                boolean takeIfBody = isTrue(condLit);

                java.util.List<ASTNode> chosen = new java.util.ArrayList<>(
                        takeIfBody ? ifc.body : elseBody(ifc)
                );

                node.removeChild(ifc);

                for (ASTNode stmt : chosen) node.addChild(stmt);

                for (ASTNode stmt : chosen) {
                    if (stmt instanceof VariableAssignment) {
                        VariableAssignment va2 = (VariableAssignment) stmt;
                        Literal val2 = eval(va2.expression);
                        if (val2 != null) current().put(va2.name.name, val2);
                        node.removeChild(va2);
                    } else if (stmt instanceof Declaration) {
                        reduceDecl((Declaration) stmt);
                    } else {
                        simplify(stmt);
                    }
                }
                continue;
            }

            //declaration: expression  naar literal
            if (child instanceof Declaration) {
                reduceDecl((Declaration) child);
                continue;
            }

            // recursie!!
            simplify(child);
        }
    }
    private boolean isTrue(Literal lit) {
        return (lit instanceof BoolLiteral) && ((BoolLiteral) lit).value;
    }
    private java.util.List<ASTNode> elseBody(IfClause ifc)  {
        for (ASTNode child : ifc.getChildren()) {
            if (child instanceof ElseClause) {
                return ((ElseClause) child).body;
            }
        }
        return java.util.Collections.emptyList();
    }
}





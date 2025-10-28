package nl.han.ica.icss.checker;

import nl.han.ica.datastructures.HANLinkedList;
import nl.han.ica.datastructures.IHANLinkedList;
import nl.han.ica.icss.ast.*;
import nl.han.ica.icss.ast.literals.*;
import nl.han.ica.icss.ast.operations.AddOperation;
import nl.han.ica.icss.ast.operations.MultiplyOperation;
import nl.han.ica.icss.ast.operations.SubtractOperation;
import nl.han.ica.icss.ast.types.ExpressionType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class Checker {

    private IHANLinkedList<HashMap<String, ExpressionType>> variableTypes = new HANLinkedList<>();
    private final List<SemanticError> semanticErrors = new ArrayList<>();

    private AST currentAst;


    public boolean check(AST ast) {
        this.currentAst = ast;
        pushScope();

        if (ast != null && ast.root != null) {
            checkNode(ast.root);
        }


        popScope();
        return semanticErrors.isEmpty();
    }
    private void error(ASTNode where, String msg) {
        SemanticError e = new SemanticError(msg);
        semanticErrors.add(e);
        if (where != null) {
            where.setError(msg);

        }
    }

    public List<SemanticError> getErrors() {
        return semanticErrors;
    }

    private void checkNode(ASTNode node) {
        if (node == null) return;

        // scoping bij stylerules
        if (node instanceof Stylerule) {
            pushScope();
        } else if (isBlockLike(node)) {
            pushScope();
        }

        //checks
        if (node instanceof VariableAssignment) {
            checkVariableAssignment((VariableAssignment) node);
        } else if (node instanceof Declaration) {
            checkDeclaration((Declaration) node);
        } else if (node instanceof IfClause) {
            checkIfClause((IfClause) node);
        }

        // recursie!!
        for (ASTNode child : node.getChildren()) {
            checkNode(child);
        }

        // scope dicht
        if (node instanceof Stylerule) {
            popScope();
        } else if (isBlockLike(node)) {
            popScope();
        }
    }

    private boolean isBlockLike(ASTNode node) {
        return (node instanceof IfClause) || (node instanceof ElseClause);
    }


//node check
    private void checkVariableAssignment(VariableAssignment va) {
        if (va.name == null) {
            error(va, "Variable assignment missing name.");
            return;
        }
        String varName = va.name.name;

        ExpressionType rhsType = typeOf(va.expression);
        if (rhsType == ExpressionType.UNDEFINED) {
            error(va, "Variable " + varName + " assigned from invalid expression.");
            return;
        }
        // overschrijven
        currentScope().put(varName, rhsType);
    }

    private void checkDeclaration(Declaration decl) {
        if (decl.expression == null) {
            error(decl, "Declaration for '" + decl.property.name + "' has no expression.");
            return;
        }
        ExpressionType et = typeOf(decl.expression);

        // Strikte properties
        switch (decl.property.name) {
            case "color":
            case "background-color":
                if (et != ExpressionType.COLOR) {
                    error(decl, "Property '" + decl.property.name + "' expects COLOR but found " + et + ".");
                }
                return;

            case "width":
            case "height":
            case "margin":
            case "padding":
                if (!(et == ExpressionType.PIXEL || et == ExpressionType.PERCENTAGE)) {
                    error(decl, "Property '" + decl.property.name + "' expects PIXEL or PERCENTAGE but found " + et + ".");
                }
                return;
        }
    }


    private void checkIfClause(IfClause ifc) {
        if (ifc.getConditionalExpression() == null) {
            error(ifc, "If-clause without condition.");
            return;
        }
        ExpressionType cond = typeOf(ifc.getConditionalExpression());
        if (cond != ExpressionType.BOOL) {
            error(ifc, "If condition should be BOOL, found " + cond + ".");
        }
    }

//types
    private ExpressionType typeOf(Expression e) {
        if (e == null) return ExpressionType.UNDEFINED;

        // literals
        if (e instanceof ColorLiteral)      return ExpressionType.COLOR;
        if (e instanceof PixelLiteral)      return ExpressionType.PIXEL;
        if (e instanceof PercentageLiteral) return ExpressionType.PERCENTAGE;
        if (e instanceof ScalarLiteral)     return ExpressionType.SCALAR;
        if (e instanceof BoolLiteral)       return ExpressionType.BOOL;

        // var ref
        if (e instanceof VariableReference) {
            String name = ((VariableReference) e).name;
            ExpressionType t = resolveVar(name);
            if (t == null) {
                error(e, "Use of undefined variable " + name + ".");
                return ExpressionType.UNDEFINED;
            }
            return t;
        }

        if (e instanceof AddOperation) {
            AddOperation op = (AddOperation) e;
            ExpressionType l = typeOf(op.lhs);
            ExpressionType r = typeOf(op.rhs);
            if (l == r && (l == ExpressionType.PIXEL || l == ExpressionType.PERCENTAGE || l == ExpressionType.SCALAR)) {
                return l;
            }
            error(e, "Invalid '+' between " + l + " and " + r + ".");
            return ExpressionType.UNDEFINED;
        }

        if (e instanceof SubtractOperation) {
            SubtractOperation op = (SubtractOperation) e;
            ExpressionType l = typeOf(op.lhs);
            ExpressionType r = typeOf(op.rhs);
            if (l == r && (l == ExpressionType.PIXEL || l == ExpressionType.PERCENTAGE || l == ExpressionType.SCALAR)) {
                return l;
            }
            error(e, "Invalid '-' between " + l + " and " + r + ".");
            return ExpressionType.UNDEFINED;
        }

        if (e instanceof MultiplyOperation) {
            MultiplyOperation op = (MultiplyOperation) e;
            ExpressionType l = typeOf(op.lhs);
            ExpressionType r = typeOf(op.rhs);
            if (l == ExpressionType.SCALAR && isNumeric(r)) return r;
            if (r == ExpressionType.SCALAR && isNumeric(l)) return l;
            if (l == ExpressionType.SCALAR && r == ExpressionType.SCALAR) return ExpressionType.SCALAR;

            error(e,"Invalid '*' between " + l + " and " + r + ". One side must be SCALAR.");
            return ExpressionType.UNDEFINED;
        }

        return ExpressionType.UNDEFINED;
    }

    private boolean isNumeric(ExpressionType t) {
        return t == ExpressionType.PIXEL || t == ExpressionType.PERCENTAGE || t == ExpressionType.SCALAR;
    }

    private ExpressionType expectedTypeForProperty(String property) {
        if (property == null) return ExpressionType.UNDEFINED;
        switch (property) {
            case "color":
            case "background-color":
                return ExpressionType.COLOR;
            case "width":
            case "height":
            case "margin":
            case "padding":
                return ExpressionType.UNDEFINED;
            default:
                return ExpressionType.UNDEFINED; // onbekend -> geen check
        }
    }

    private boolean isAssignableTo(ExpressionType actual, ExpressionType expected) {
        if (expected == ExpressionType.UNDEFINED) {

            // deze helper blijft generiek. de property specifieke checks zitten al in checkDeclaration().
            return true;
        }
        return actual == expected;
    }


// scope helpers
    private void pushScope() {
        // variabele-scopes worden LIFO gepusht
        HashMap<String, ExpressionType> frame = new HashMap<>();
        variableTypes.addFirst(frame);
    }

    private void popScope() {
        // verwijder het bovenste frame
        variableTypes.removeFirst();
    }

    private HashMap<String, ExpressionType> currentScope() {
        return variableTypes.getFirst();
    }

    private ExpressionType resolveVar(String name) {
        for (int i = 0; i < variableTypes.getSize(); i++) { // inner → outer
            HashMap<String, ExpressionType> scope = variableTypes.get(i);
            if (scope != null && scope.containsKey(name)) {
                return scope.get(name);
            }
        }
        return null;
    }

}

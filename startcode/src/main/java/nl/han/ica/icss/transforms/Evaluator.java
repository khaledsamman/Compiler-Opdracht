package nl.han.ica.icss.transforms;

import nl.han.ica.datastructures.HANLinkedList;
import nl.han.ica.datastructures.IHANLinkedList;
import nl.han.ica.icss.ast.*;

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
    }
private void push (){

}

private void pop(){

}

}



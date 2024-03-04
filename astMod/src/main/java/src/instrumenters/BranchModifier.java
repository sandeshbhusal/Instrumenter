package src.instrumenters;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.IfStatement;

public class BranchModifier extends ASTVisitor {
    @Override
    public boolean visit(ExpressionStatement exp) {
        if (exp.getParent() instanceof IfStatement) {
            System.out.println("Found an if statement expression: " + exp.toString());
        }

        return true;
    } 
}

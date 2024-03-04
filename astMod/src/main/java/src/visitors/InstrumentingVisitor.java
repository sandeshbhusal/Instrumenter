package src.visitors;

import java.util.HashSet;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

public class InstrumentingVisitor extends ASTVisitor {
    ASTRewrite rewriter;

    String currentMethodName = null;
    String currentClassName = null;

    ClassAttributes currentClassAttributes = null;
    MethodAttributes currentMethodAttributes = null;
    IfAttributes currentIfAttributes = null;

    public static class IfAttributes {
        HashSet<String> instanceVariables;
        Expression predicate;
        MethodAttributes parent;
        int id = 0;

        public IfAttributes(MethodAttributes parent) {
            this.instanceVariables = new HashSet<>();
            this.parent = parent;
        }
    }

    public static class MethodAttributes {
        String methodName;
        HashSet<String> localVariables;
        HashSet<IfAttributes> ifStatements;
        ClassAttributes parent;

        public MethodAttributes(ClassAttributes parent) {
            this.localVariables = new HashSet<>();
            this.ifStatements = new HashSet<>();
            this.parent = parent;
        }
    }

    public static class ClassAttributes {
        String className;
        HashSet<String> instanceVariables;
        HashSet<MethodAttributes> declaredMethods;

        public ClassAttributes() {
            this.instanceVariables = new HashSet<>();
            this.declaredMethods = new HashSet<>();
        }
    }

    public InstrumentingVisitor(ASTRewrite rewrite) {
        this.rewriter = rewrite;
    }

    /* Check the type of node, and initialize current maps, strings, etc. */
    @Override
    public void preVisit(ASTNode node) {
        if (node instanceof TypeDeclaration) {
            // Set current method name
            TypeDeclaration decl = (TypeDeclaration) node;
            this.currentClassName = decl.getName().toString();
            // Initialize map
            this.currentClassAttributes = new ClassAttributes();
        }

        if (node instanceof MethodDeclaration) {
            MethodDeclaration decl = (MethodDeclaration) node;
            this.currentMethodName = decl.getName().toString();
            this.currentMethodAttributes = new MethodAttributes(this.currentClassAttributes);
            this.currentClassAttributes.declaredMethods.add(currentMethodAttributes);
        }

        if (node instanceof IfStatement) {
            this.currentIfAttributes = new IfAttributes(this.currentMethodAttributes);
            this.currentMethodAttributes.ifStatements.add(currentIfAttributes);
        }
    }

    @Override
    public void endVisit(IfStatement node) {
        // Add a "Print" invocation before the variable, print its value.
        MethodInvocation invocation = this.rewriter.getAST().newMethodInvocation();

        invocation.setExpression(this.rewriter.getAST().newName("Recorder"));
        invocation.setName(this.rewriter.getAST().newSimpleName("instrument"));

        for (String variable : currentIfAttributes.instanceVariables) {
            SimpleName arg = this.rewriter.getAST().newSimpleName(variable);
            invocation.arguments().add(arg);
        }

        ExpressionStatement statement = this.rewriter.getAST().newExpressionStatement(invocation);

        // Insert the print statement before the if statement
        ListRewrite rewrite = rewriter.getListRewrite(node.getParent(), Block.STATEMENTS_PROPERTY);
        rewrite.insertBefore(statement, node, null);
    }

    @Override
    public void endVisit(MethodDeclaration node) {
        this.currentMethodAttributes = null;
    }

    @Override
    public void endVisit(TypeDeclaration node) {
        this.currentClassAttributes = null;
    }

    // ----------------- On Visit of each statement type ----------------------
    @Override
    public boolean visit(IfStatement node) {
        // Get the variables involved in the expression.
        Expression exp = node.getExpression();

        // Collect all variables we can find in the expression.
        NameCollector collector = new NameCollector();
        exp.accept(collector);
        HashSet<String> varNames = collector.identifiers;

        // Check what values we can prune (local vars).
        varNames.retainAll(this.currentClassAttributes.instanceVariables);
        this.currentIfAttributes.instanceVariables = varNames;

        return true;
    }

    @Override
    public boolean visit(VariableDeclarationFragment node) {
        if (this.currentMethodAttributes == null) {
            // BUG: All instance variables need to be declared before methods begin.
            this.currentClassAttributes.instanceVariables.add(node.getName().toString());
        } else {
            this.currentMethodAttributes.localVariables.add(node.getName().toString());
        }
        return true;
    }
}

// TODO: Use this to generate all the variables
// Maybe this is an overkill for now.
// class ExpressionVisitor1 extends ASTVisitor {
// public static int depth = 0;

// public static class ExpressionInfo {
// Expression expression;
// public HashSet<String> identifiers = new HashSet<>();
// }

// // @Override
// // public boolean visit(ExpressionStatement exp) {

// // }
// }

class NameCollector extends ASTVisitor {
    public HashSet<String> identifiers = new HashSet<>();

    @Override
    public boolean visit(SimpleName node) {
        this.identifiers.add(node.getFullyQualifiedName());

        return true;
    }
}

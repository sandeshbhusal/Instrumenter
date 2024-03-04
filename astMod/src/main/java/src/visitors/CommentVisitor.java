package src.visitors;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class CommentVisitor extends ASTVisitor {
    private static final Logger logger = LoggerFactory.getLogger(CommentVisitor.class.getName());
    ASTRewrite rewrite;

    CommentVisitor(AST node) {
        this.rewrite = ASTRewrite.create(node);
    }

    public ASTRewrite getRewrite() {
        return this.rewrite;
    }

    public static class IfMap {
        public HashMap<Integer, Set<String>> ifVariableMap = new HashMap<>();

        @Override
        public String toString() {
            return this.ifVariableMap.toString();
        }
    }

    public static class MethodMap {
        public String name;
        public ArrayList<IfMap> ifMap = new ArrayList<>();

        @Override
        public String toString() {
            return this.name + this.ifMap.toString();
        }
    }

    public static class ClassMap {
        public String name;
        public ArrayList<String> variables = new ArrayList<>();
        public ArrayList<MethodMap> methodMap = new ArrayList<>();

        @Override
        public String toString() {
            return this.name + this.methodMap.toString();
        }
    }

    private IfMap currentIfMap = null;
    private MethodMap currentMethodMap = null;
    private ClassMap currentClassMap = null;
    private String currentClass = null;
    private String currentMethod = null;

    private ArrayList<ClassMap> classes = new ArrayList<>();

    private final Set<SimpleName> names = new HashSet<>();

    @Override
    public void preVisit(ASTNode node) {
        if (node instanceof TypeDeclaration) {
            this.currentClass = ((TypeDeclaration) node).getName().toString();
            logger.debug("Entering class {}", currentClass);
            this.currentClassMap = new ClassMap();

        } else if (node instanceof MethodDeclaration) {
            this.currentMethod = ((MethodDeclaration) node).getName().toString();
            logger.debug("Entering method {}", currentMethod);
            this.currentMethodMap = new MethodMap();

        } else if (node instanceof IfStatement) {
            logger.debug("Visiting IF statement");

            AST root = node.getAST();
            ASTNode parent = node.getParent();

            // Create a new string literal
            StringLiteral stringLiteral = root.newStringLiteral();
            stringLiteral.setLiteralValue("Hello, world!");

            // Create a new MethodInvocation
            MethodInvocation methodInvocation = root.newMethodInvocation();
            QualifiedName qualifiedSystem = root.newQualifiedName(root.newSimpleName("observer"),
                    root.newSimpleName("Reporter"));
            methodInvocation.setExpression(qualifiedSystem);
            methodInvocation.setName(root.newSimpleName("reportVariable"));

            StringLiteral lit = root.newStringLiteral();
            lit.setLiteralValue("classname.methodname");

            methodInvocation.arguments().add(lit);
            methodInvocation.arguments().add(5); // BRanch ID

            // Create a new ExpressionStatement
            ExpressionStatement helloWorldStatement = root.newExpressionStatement(methodInvocation);

            // Insert the helloWorldStatement before the IF statement
            ListRewrite re = rewrite.getListRewrite(parent, Block.STATEMENTS_PROPERTY);
            re.insertBefore(helloWorldStatement, node, null);

            this.currentIfMap = new IfMap();

        } else if (node instanceof VariableDeclaration) {
            if (this.currentMethod == null) {
                // Instance variable declaration.
                VariableDeclaration statement = (VariableDeclaration) node;
                String variableName = statement.getName().toString();
                this.currentClassMap.variables.add(variableName);

                logger.debug("Found instance variable {}", variableName);
            }
        }
    }

    @Override
    public void endVisit(MethodDeclaration node) {
        // At the end of method declaration, collect all 'if' statements,
        // dump it to a method map.
        this.currentMethod = null;

        this.currentMethodMap.name = currentClass + "." + node.getName().toString();
        this.currentMethodMap.ifMap.add(currentIfMap);

        this.classes.add(this.currentClassMap);
        this.currentClassMap = new ClassMap();
    }

    @Override
    public void endVisit(TypeDeclaration node) {
        this.currentClass = null;

        this.currentClassMap.name = ((TypeDeclaration) node).getName().toString();
        this.currentClassMap.methodMap.add(currentMethodMap);
        this.classes.add(this.currentClassMap);

        this.currentClassMap = new ClassMap();
    }

    @Override
    public void endVisit(IfStatement node) {
        Expression predicate = node.getExpression();

        // Reset the name collector
        this.names.clear();

        // Collect names in the predicate
        NameCollector collector = new NameCollector();
        predicate.accept(collector);

        // Add names to the current if statement
        addIfStatement(this.names);
    }

    private void addIfStatement(Set<SimpleName> variables) {
        Set<String> names = new HashSet<>();

        for (SimpleName var : variables) {
            if (this.currentClassMap.variables.contains(var.toString())) {
                // Add this to the if statement.
                names.add(var.toString());
            }
        }

        int index = this.currentIfMap.ifVariableMap.size();
        this.currentIfMap.ifVariableMap.put(index, names);

        this.currentIfMap = new IfMap(); // Reset the If map.
    }

    private static class NameCollector extends ASTVisitor {
        private final Set<SimpleName> names = new HashSet<>();

        @Override
        public void endVisit(SimpleName name) {
            this.names.add(name);
        }
    }
}

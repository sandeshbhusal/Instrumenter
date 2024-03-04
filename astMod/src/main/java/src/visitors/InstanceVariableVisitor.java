package src.visitors;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import java.util.*;

/* InstanceVariableVisitor: This class collects all the variables declared in a 
 * class. This should be visited on finding a class. All nested classes are collected
 * as well.
 */
public class InstanceVariableVisitor extends ASTVisitor {
    private String currentClassName = "";
    private String currentMethodName = "";
    private String thisClassName = "";

    public InstanceVariableVisitor(String name) {
        thisClassName = name;
    }

    public List<VariableDeclaration> declarations = new ArrayList<>();

    public static class VariableDeclaration {
        public String className;
        public String name;
        public Type type;

        public VariableDeclaration(String className, String name, Type type) {
            this.className = className;
            this.name = name;
            this.type = type;
        }
    }

    @Override
    public void preVisit(ASTNode node) {
        if (node instanceof TypeDeclaration) {
            TypeDeclaration tdcl = (TypeDeclaration) node;
            this.currentClassName = tdcl.getName().toString();
        }

        if (node instanceof MethodDeclaration) {
            MethodDeclaration decl = (MethodDeclaration) node;
            this.currentMethodName = decl.getName().toString();
        }
    }

    @Override
    public void endVisit(TypeDeclaration node) {
        this.currentClassName = "";
    }

    @Override
    public void endVisit(MethodDeclaration node) {
        this.currentMethodName = "";
    }

    @Override
    public void endVisit(VariableDeclarationStatement decl) {
        // Only collect variables when current method is nothing.
        if (this.currentMethodName == "") {
            Type type = decl.getType();

            for (Object vdf : decl.fragments()) {
                if (vdf instanceof VariableDeclarationFragment) {
                    VariableDeclarationFragment frag = (VariableDeclarationFragment) vdf;

                    if (currentClassName == thisClassName)
                        declarations.add(new VariableDeclaration(thisClassName, frag.getName().toString(), type));
                }
            }
        }
    }
}

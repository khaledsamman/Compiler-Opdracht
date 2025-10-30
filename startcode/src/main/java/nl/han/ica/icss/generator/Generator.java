package nl.han.ica.icss.generator;


import nl.han.ica.icss.ast.*;
import nl.han.ica.icss.ast.literals.ColorLiteral;
import nl.han.ica.icss.ast.literals.PercentageLiteral;
import nl.han.ica.icss.ast.literals.PixelLiteral;
import nl.han.ica.icss.ast.literals.ScalarLiteral;
import nl.han.ica.icss.ast.selectors.ClassSelector;
import nl.han.ica.icss.ast.selectors.IdSelector;
import nl.han.ica.icss.ast.selectors.TagSelector;


public class Generator {

	public String generate(AST ast) {
		if (ast == null || ast.root == null) return "";
		StringBuilder css = new StringBuilder();
		emitStyleSheet (ast.root, css);
        return css.toString();

	}

	private void emitStyleSheet(ASTNode node, StringBuilder css) {
		for (ASTNode child :  node.getChildren()) {
			if (child instanceof Stylerule) {
				emitStyleRule((Stylerule) child, css, 0);
				css.append("\n");
			}
		}
	}

	private void emitStyleRule(Stylerule rule, StringBuilder css, int level) {
		String selector = getSelectorName(rule);
		addSpaces(css, level).append(selector).append(" {\n");

		// alle declaraties
		for (ASTNode child : rule.getChildren()) {
			if (child instanceof Declaration) {
				emitDeclaration((Declaration) child, css, level + 1);
			}
		}
		addSpaces(css, level).append("}\n");
	}
	private void emitDeclaration(Declaration d, StringBuilder css, int level) {
		String value = literalToCss(d.expression);
		addSpaces(css, level)
				.append(d.property.name)
				.append(": ")
				.append(value)
				.append(";\n");
	}

	// maak inspringing
	// [GE02] Indent: 2 spaties per scopeniveau.
	private StringBuilder addSpaces(StringBuilder css, int level) {
		for (int i = 0; i < level; i++) css.append("  "); // 2 spaties per scope
		return css;
	}

	// bepaal selector string
	private String getSelectorName(Stylerule rule) {
		for (ASTNode child : rule.getChildren()) {
			if (child instanceof TagSelector)
				return ((TagSelector) child).tag;
			if (child instanceof ClassSelector)
				return ((ClassSelector) child).cls;
			if (child instanceof IdSelector)
				return ((IdSelector) child).id;
		}
		return "";
	}

	// converteer literal naar css string
	// [GE01] AST → CSS2 string: selectors, declaraties en literal→CSS string omzetting.
	private String literalToCss(Expression e) {
		if (e instanceof ColorLiteral) return ((ColorLiteral) e).value;
		if (e instanceof PixelLiteral) return ((PixelLiteral) e).value + "px";
		if (e instanceof PercentageLiteral) return ((PercentageLiteral) e).value + "%";
		if (e instanceof ScalarLiteral) return Integer.toString(((ScalarLiteral) e).value);
		return "";
	}
}


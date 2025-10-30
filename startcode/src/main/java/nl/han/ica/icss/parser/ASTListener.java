package nl.han.ica.icss.parser;


import nl.han.ica.datastructures.HANStack;
import nl.han.ica.datastructures.IHANStack;
import nl.han.ica.icss.ast.*;
import nl.han.ica.icss.ast.literals.*;
import nl.han.ica.icss.ast.operations.AddOperation;
import nl.han.ica.icss.ast.operations.MultiplyOperation;
import nl.han.ica.icss.ast.operations.SubtractOperation;
import nl.han.ica.icss.ast.selectors.ClassSelector;
import nl.han.ica.icss.ast.selectors.IdSelector;
import nl.han.ica.icss.ast.selectors.TagSelector;

/**
 * This class extracts the ICSS Abstract Syntax Tree from the Antlr Parse tree.
 */

//was hier aan het overwegen als ik ParseTreeProperty<> moet gebruiken of niet. uiteindelijk niet gekozen.
public class ASTListener extends ICSSBaseListener {

	private final AST ast = new AST();
	// [PA00] Gebruik eigen HANStack<ASTNode> om AST-parents te managen.
	private final IHANStack<ASTNode> parents = new HANStack<>();
	private final IHANStack<Expression> exprStack = new HANStack<>();
	private final IHANStack<Selector> selectorStack = new HANStack<>();
	private final IHANStack<Integer> ifPhase = new HANStack<>(); // 0=then 1=else


	public AST getAST() { return ast; }

//stylesheet
	// [PA01] Basis CSS-structuur: stylesheet, stylerule, selector en declaration worden naar AST gezet.
	@Override
	public void enterStylesheet(ICSSParser.StylesheetContext ctx) {
		Stylesheet sheet = new Stylesheet();
		ast.setRoot(sheet);
		parents.push(sheet);
	}

	@Override
	public void exitStylesheet(ICSSParser.StylesheetContext ctx) {
		parents.pop();
	}

	//stylerule + selector
	@Override
	public void enterStylerule(ICSSParser.StyleruleContext ctx) {
		Stylerule rule = new Stylerule();
		parents.peek().addChild(rule);
		parents.push(rule);
	}

	@Override
	public void exitStylerule(ICSSParser.StyleruleContext ctx) {
		parents.pop();
	}

	@Override
	public void exitSelector(ICSSParser.SelectorContext ctx) {
		Selector sel;
		if (ctx.LOWER_IDENT() != null) {
			sel = new TagSelector(ctx.LOWER_IDENT().getText());
		} else if (ctx.CLASS_IDENT() != null) {
			sel = new ClassSelector(ctx.CLASS_IDENT().getText());
		} else {
			sel = new IdSelector(ctx.ID_IDENT().getText());
		}
		((Stylerule) parents.peek()).addChild(sel);
	}

	//declarations en variables
	@Override
	public void exitDeclaration(ICSSParser.DeclarationContext ctx) {
		String property = ctx.LOWER_IDENT().getText();
		Expression value = exprStack.pop();
		Declaration decl = new Declaration(property);
		decl.addChild(value);
		parents.peek().addChild(decl);
	}


	// [PA02] Variabele toekennen en VariableReference ondersteunen in expressies.
	@Override
	public void exitVariableAssignment(ICSSParser.VariableAssignmentContext ctx) {
		String name = ctx.CAPITAL_IDENT().getText();
		Expression value = exprStack.pop();

		VariableAssignment va = new VariableAssignment();
		va.name = new VariableReference(name);
		va.expression = value;

		parents.peek().addChild(va);
	}


  //expressions
	// [PA03] Rekenkundige expressies met correcte prioriteit (mul voor add/sub).
	// expression is alleen een doorgeefluik
	@Override
	public void exitAddExpr(ICSSParser.AddExprContext ctx) {
		int k = ctx.mulExpr().size();
		// k sub expressies rechts naar links en bouw links
		Expression acc = exprStack.pop(); // meest rechtse
		for (int i = k - 2; i >= 0; i--) {
			Expression left = exprStack.pop();
			String op = ctx.getChild(2 * i + 1).getText(); // + of -
			if ("-".equals(op)) {
				SubtractOperation sub = new SubtractOperation();
				sub.lhs = left;
				sub.rhs = acc;
				acc = sub;
			} else {
				AddOperation add = new AddOperation();
				add.lhs = left;
				add.rhs = acc;
				acc = add;
			}
		}
		exprStack.push(acc);
	}

	// [PA03] Rekenkundige expressies met correcte prioriteit (mul voor add/sub).
	@Override
	public void exitMulExpr(ICSSParser.MulExprContext ctx) {
		int n = ctx.primary().size();
		Expression acc = exprStack.pop(); // meest rechtse primary
		for (int i = n - 2; i >= 0; i--) {
			Expression left = exprStack.pop();
			MultiplyOperation mul = new MultiplyOperation();
			mul.lhs = left;
			mul.rhs = acc;
			acc = mul;
		}
		exprStack.push(acc);
	}

	// [PA02] Variabele toekennen en VariableReference ondersteunen in expressies.
	@Override
	public void exitPrimary(ICSSParser.PrimaryContext ctx) {
		if (ctx.value() != null) {
			exprStack.push(literalFrom(ctx.value()));
		} else if (ctx.CAPITAL_IDENT() != null) {
			exprStack.push(new VariableReference(ctx.CAPITAL_IDENT().getText()));
		} else {
			// {addExpr} -> binnenste addExpr heeft al op de stack gepusht. niets doen.
		}
	}

	//values en literals
	private Expression literalFrom(ICSSParser.ValueContext ctx) {
		if (ctx.COLOR() != null) {
			return new ColorLiteral(ctx.COLOR().getText());
		}
		if (ctx.PIXELSIZE() != null) {
			String txt = ctx.PIXELSIZE().getText();          // bijv.500px
			int n = Integer.parseInt(txt.substring(0, txt.length() - 2));
			return new PixelLiteral(n);
		}
		if (ctx.PERCENTAGE() != null) {
			String txt = ctx.PERCENTAGE().getText();         // bijv.80%
			int n = Integer.parseInt(txt.substring(0, txt.length() - 1));
			return new PercentageLiteral(n);
		}
		if (ctx.SCALAR() != null) {
			return new ScalarLiteral(Integer.parseInt(ctx.SCALAR().getText()));
		}
		if (ctx.TRUE() != null) {
			return new BoolLiteral(true);
		}
		return new BoolLiteral(false);
	}

//if-else
	// [PA04] IfClause + optionele ElseClause.
	@Override
	public void enterIfStatement(ICSSParser.IfStatementContext ctx) {
		IfClause ifc = new IfClause();
		parents.peek().addChild(ifc);
		parents.push(ifc);
		ifPhase.push(0);
	}

	@Override
	public void exitIfStatement(ICSSParser.IfStatementContext ctx) {
		Expression cond = exprStack.pop();
		((IfClause) parents.peek()).conditionalExpression = cond;
		parents.pop();
		ifPhase.pop();
	}



	@Override
	public void enterBlock(ICSSParser.BlockContext ctx) {
		Integer phaseTop = ifPhase.peek();
		if (phaseTop != null && parents.peek() instanceof IfClause) {
			int phase = ifPhase.pop();
			if (phase == 0) {
				// THEN-block: kinderen direct aan IfClause.body toevoegen.
				// Truc: push dezelfde IfClause nogmaals zodat exitBlock daarvoor kan poppen.
				parents.push(parents.peek());
			} else {
				// ELSE-block: maak ElseClause en routeer kinderen daarheen.
				ElseClause ec = new ElseClause();
				((IfClause) parents.peek()).addChild(ec);
				parents.push(ec);
			}
			ifPhase.push(phase + 1); // volgende block (indien aanwezig) wordt ELSE
			return;
		}

		// Anders: generieke block (we gebruiken je huidige container als target)
		parents.push(parents.peek());
	}


	@Override
	public void exitBlock(ICSSParser.BlockContext ctx) {
		parents.pop();
	}
}
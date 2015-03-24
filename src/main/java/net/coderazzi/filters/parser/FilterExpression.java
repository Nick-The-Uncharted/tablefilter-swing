package net.coderazzi.filters.parser;

import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import javax.swing.RowFilter;
import javax.swing.table.TableModel;

import com.sun.org.apache.xerces.internal.impl.xpath.regex.ParseException;

public class FilterExpression {
	/**
	 * The original infix expression.
	 */
	private String expression = null;

	/**
	 * The cached RPN (Reverse Polish Notation) of the expression.
	 */
	private List<String> rpn = null;

	/**
	 * All defined operators with name and implementation.
	 */
	Map<String, Operator> operators = new HashMap<String, FilterExpression.Operator>();
	
	private Parser parser;
	/**
	 * The expression evaluators exception class.
	 */
	public class ExpressionException extends RuntimeException {
		private static final long serialVersionUID = 1118142866870779047L;

		public ExpressionException(String message) {
			super(message);
		}
	}

	/**
	 * Abstract definition of a supported operator. An operator is defined by
	 * its name (pattern), precedence and if it is left- or right associative.
	 */
	public abstract class Operator {
		/**
		 * This operators name (pattern).
		 */
		private String oper;
		/**
		 * Operators precedence.
		 */
		private int precedence;

		/**
		 * Creates a new operator.
		 * 
		 * @param oper
		 *            The operator name (pattern).
		 * @param precedence
		 *            The operators precedence.
		 * @param leftAssoc
		 *            <code>true</code> if the operator is left associative,
		 *            else <code>false</code>.
		 */
		public Operator(String oper, int precedence) {
			this.oper = oper;
			this.precedence = precedence;
		}

		public String getOper() {
			return oper;
		}

		public int getPrecedence() {
			return precedence;
		}

		/**
		 * Implementation for this operator.
		 * 
		 * @param v1
		 *            Operand 1.
		 * @param v2
		 *            Operand 2.
		 * @return The result of the operation.
		 * @throws java.text.ParseException 
		 */
		public abstract RowFilter<TableModel, Integer> eval(Object v1, Object v2) throws ParseException, java.text.ParseException;
	}

	abstract class Parser{
		public abstract RowFilter<TableModel, Integer> eval(String v) throws java.text.ParseException;
	}
	
	/**
	 * Expression tokenizer that allows to iterate over a {@link String}
	 * expression token by token. Blank characters will be skipped.
	 */
	private class Tokenizer implements Iterator<String> {

		/**
		 * Actual position in expression string.
		 */
		private int pos = 0;
		
		/**
		 * The original input expression.
		 */
		private String input;

		/**
		 * Creates a new tokenizer for an expression.
		 * 
		 * @param input
		 *            The expression string.
		 */
		public Tokenizer(String input) {
			this.input = input.trim();
		}

		@Override
		public boolean hasNext() {
			return (pos < input.length());
		}

		@Override
		public String next() {
			StringBuilder token = new StringBuilder();
			if (pos >= input.length()) {
				return null;
			}
			char ch = input.charAt(pos);
			while (Character.isWhitespace(ch) && pos < input.length()) {
				ch = input.charAt(++pos);
			}
			
			if (!Character.isWhitespace(ch) && ch != '('
					&& ch != ')' && ch != '&' && ch != '|') {
				while ((!Character.isWhitespace(ch) && ch != '('
						&& ch != ')'  && ch != '&' && ch != '|' &&  (pos < input.length()))) {
					token.append(input.charAt(pos++));
					ch = pos == input.length() ? 0 : input.charAt(pos);
				}
			} else if (ch == '(' || ch == ')') {
				token.append(ch);
				pos++;
			} else {
				String str = "!~><=~!";
				// extract operator
				while (!Character.isLetter(ch) && !Character.isDigit(ch) && ch != '_'
						&& !Character.isWhitespace(ch) && ch != '('
						&& ch != ')' && str.indexOf(ch) == -1 && (pos < input.length())) {
					token.append(input.charAt(pos));
					pos++;
					ch = pos == input.length() ? 0 : input.charAt(pos);
				}
				if (!operators.containsKey(token.toString())) {
					throw new ExpressionException("Unknown operator '" + token
							+ "' at position " + (pos - token.length() + 1));
				}
			}
			return token.toString();
		}

		@Override
		public void remove() {
			throw new ExpressionException("remove() not supported");
		}

		/**
		 * Get the actual character position in the string.
		 * 
		 * @return The actual character position.
		 */
		public int getPos() {
			return pos;
		}

	}

	/**
	 * Creates a new expression instance from an expression string.
	 * 
	 * @param expression
	 *            The expression. E.g. <code>"2.4*sin(3)/(2-4)"</code> or
	 *            <code>"sin(y)>0 & max(z, 3)>3"</code>
	 */
	public FilterExpression(String expression) {
		this.expression = expression;
		
	}

	/**
	 * Implementation of the <i>Shunting Yard</i> algorithm to transform an
	 * infix expression to a RPN expression.
	 * 
	 * @param expression
	 *            The input expression in infx.
	 * @return A RPN representation of the expression, with each token as a list
	 *         member.
	 */
	private List<String> shuntingYard(String expression) throws ExpressionException{
		List<String> outputQueue = new ArrayList<String>();
		Stack<String> stack = new Stack<String>();

		Tokenizer tokenizer = new Tokenizer(expression);

		String previousToken = null;
		while (tokenizer.hasNext()) {
			String token = tokenizer.next();
			
			if (operators.containsKey(token)) {
				Operator o1 = operators.get(token);
				String token2 = stack.isEmpty() ? null : stack.peek();
				while (operators.containsKey(token2)
						&& (o1.getPrecedence() <= operators
								.get(token2).getPrecedence())) {
					outputQueue.add(stack.pop());
					token2 = stack.isEmpty() ? null : stack.peek();
				}
				stack.push(token);
			} else if ("(".equals(token)) {
				if (previousToken != null) {
					if (Character.isLetter(previousToken.charAt(0))||Character.isDigit(previousToken.charAt(0))||previousToken.charAt(0) == '_') {
						System.out.println(previousToken);
						throw new ExpressionException("Missing operator at character position " + tokenizer.getPos());
					}
				}
				stack.push(token);
			} else if (")".equals(token)) {
				while (!stack.isEmpty() && !"(".equals(stack.peek())) {
					outputQueue.add(stack.pop());
				}
				if (stack.isEmpty()) {
					throw new ExpressionException("Mismatched parentheses at character position" +  tokenizer.getPos());
				}
				//pop "("
				stack.pop();
			}else{
				outputQueue.add(token);
			}
			previousToken = token;
		}
		while (!stack.isEmpty()) {
			String element = stack.pop();
			if ("(".equals(element) || ")".equals(element)) {
				throw new ExpressionException("Mismatched parentheses at character position" +  tokenizer.getPos());
			}
			outputQueue.add(element);
		}
		return outputQueue;
	}

	/**
	 * Evaluates the expression.
	 * 
	 * @return The result of the expression.
	 * @throws java.text.ParseException 
	 */
	@SuppressWarnings("unchecked")
	public RowFilter<TableModel, Integer> eval() throws java.text.ParseException,ExpressionException{

		Stack<Object> stack = new Stack<Object>();

		try{
			for (String token : getRPN()) {
				if (operators.containsKey(token)) {
					Object v1 = stack.pop();
					Object v2 = stack.pop();
					stack.push(operators.get(token).eval(v2, v1));
				} else {
					stack.push(parser.eval(token));
				}
			}
			
			if(stack.isEmpty()){
				throw new ExpressionException("missing operand");
			}else if(stack.size() > 1){
				throw new ExpressionException("missing oeprator");
			}
			return (RowFilter<TableModel, Integer>)stack.pop();
		}catch(EmptyStackException ese){
			throw new ExpressionException("missing operand");
		}

	}

	/**
	 * Adds an operator to the list of supported operators.
	 * 
	 * @param operator
	 *            The operator to add.
	 * @return The previous operator with that name, or <code>null</code> if
	 *         there was none.
	 */
	public Operator addOperator(Operator operator) {
		return operators.put(operator.getOper(), operator);
	}
	
	/**
	 * Adds an unary operator to the list of supported operators.
	 * 
	 * @param operator
	 *            The operator to add.
	 * @return The previous operator with that name, or <code>null</code> if
	 *         there was none.
	 */
	public void setParser(Parser parser) {
		this.parser = parser;
	}
	
	
	/**
	 * Get an iterator for this expression, allows iterating over an expression
	 * token by token.
	 * 
	 * @return A new iterator instance for this expression.
	 */
	public Iterator<String> getExpressionTokenizer() {
		return new Tokenizer(this.expression);
	}

	/**
	 * Cached access to the RPN notation of this expression, ensures only one
	 * calculation of the RPN per expression instance. If no cached instance
	 * exists, a new one will be created and put to the cache.
	 * 
	 * @return The cached RPN instance.
	 */
	private List<String> getRPN() throws ExpressionException{
		if (rpn == null) {
			rpn = shuntingYard(this.expression);
		}
		return rpn;
	}

}

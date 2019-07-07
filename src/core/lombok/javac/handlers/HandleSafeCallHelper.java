package lombok.javac.handlers;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.PrimitiveTypeTree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.*;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Name;
import lombok.Lombok;
import lombok.core.AST;
import lombok.core.handlers.SafeCallAbortProcessing;
import lombok.core.handlers.SafeCallIllegalUsingException;
import lombok.core.handlers.SafeCallInternalException;
import lombok.core.handlers.SafeCallUnexpectedStateException;
import lombok.javac.JavacAST;
import lombok.javac.JavacNode;
import lombok.javac.JavacResolution;
import lombok.javac.JavacTreeMaker;

import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import static com.sun.tools.javac.code.Flags.STATIC;
import static java.util.Arrays.asList;
import static javax.lang.model.type.TypeKind.BOOLEAN;
import static javax.lang.model.type.TypeKind.INT;
import static lombok.core.AST.Kind.TYPE;
import static lombok.core.handlers.SafeCallAbortProcessing.Place.methodErrorType;
import static lombok.core.handlers.SafeCallAbortProcessing.Place.resolveExprType;
import static lombok.core.handlers.SafeCallIllegalUsingException.MsgBuilder.unsupportedExpression;
import static lombok.core.handlers.SafeCallIllegalUsingException.*;
import static lombok.core.handlers.SafeCallUnexpectedStateException.Place.*;
import static lombok.javac.Javac.*;
import static lombok.javac.Javac.isPrimitive;
import static lombok.javac.JavacResolution.createJavaType;

/**
 * Created by Bulgakov Alexander on 20.12.16.
 */
public final class HandleSafeCallHelper {
	public static final int NOT_USED = -1;
	
	private HandleSafeCallHelper() {
	}
	private static JCExpression getDefaultValue(
			JavacTreeMaker maker, TypeKind typeKind, int pos) {
		JCExpression expression = getDefaultValue(maker, typeKind);
		expression.pos = pos;
		return expression;
	}
	private static JCExpression getDefaultValue(
			JavacTreeMaker maker, TypeKind typeKind) {
		switch (typeKind) {
			case BOOLEAN:
				return maker.Literal(CTC_BOOLEAN, 0);
			case CHAR:
				return maker.Literal(CTC_CHAR, 0);
			case BYTE:
			case SHORT:
			case INT:
				return maker.Literal(CTC_INT, 0);
			case LONG:
				return maker.Literal(CTC_LONG, 0L);
			case FLOAT:
				return maker.Literal(CTC_FLOAT, 0F);
			case DOUBLE:
				return maker.Literal(CTC_DOUBLE, 0D);
		}
		throw new IllegalArgumentException("unsupported type " + typeKind);
	}
	
	private static JCExpression newElvis(
			JavacTreeMaker maker,
			JavacAST ast, JCExpression expr,
			Type truePartType
	) {
		return newElvis(maker, ast, expr, truePartType, truePartType);
	}
	
	private static JCExpression newElvis(
			JavacTreeMaker maker,
			JavacAST ast,
			JCExpression expr,
			Type truePartType,
			Type falsePartType
	) {
		JCExpression result;
		JCExpression falsePart = getFalsePart(maker, falsePartType, expr.pos);
		if (expr instanceof JCMethodInvocation) {
			JCMethodInvocation mi = (JCMethodInvocation) expr;
			JCExpression meth = mi.meth;
			if (meth instanceof JCFieldAccess) {
				JCExpression parentExpr = ((JCFieldAccess) meth).selected;
				if (parentExpr instanceof JCNewClass) {
					result = null;
				} else {
					JCExpression notNullCond = newIfNullThenConditional(maker, ast, parentExpr, mi,
							falsePart);
					JCExpression newParentCnd = newElvis(maker, ast, parentExpr, falsePartType);
					if (newParentCnd instanceof JCConditional) {
						JCConditional cnd = (JCConditional) newParentCnd;
						JCExpression truepart = cnd.truepart;
						while (truepart instanceof JCConditional) {
							cnd = (JCConditional) truepart;
							truepart = cnd.truepart;
						}
						cnd.truepart = notNullCond;
						result = newParentCnd;
					} else result = notNullCond;
				}
			} else if (meth instanceof JCIdent) {
				result = null;
			} else {
				throw new SafeCallUnexpectedStateException(elvisConditionalMethodInvocation, expr, meth.getClass());
			}
		} else if (expr instanceof JCFieldAccess) {
			result = newIfNullThenConditional(maker, ast, ((JCFieldAccess) expr).selected, expr, falsePart);
		} else if (expr instanceof JCIdent) {
			if (!truePartType.isPrimitive() && falsePartType.isPrimitive()) {
				return newIfNullThenConditional(maker, ast, expr, expr, falsePart);
			} else return null;
		} else if (expr instanceof JCLiteral) {
			result = null;
		} else if (expr instanceof JCArrayAccess) {
			JCArrayAccess arrayAccess = (JCArrayAccess) expr;
			JCExpression indexed = arrayAccess.indexed;
			result = newIfNullThenConditional(maker, ast, indexed, expr, falsePart);
		} else if (expr instanceof JCTypeCast) {
			result = newIfNullThenConditional(maker, ast, ((JCTypeCast) expr).expr, expr, falsePart);
		} else if (expr == null) result = null;
		else {
			throw new SafeCallUnexpectedStateException(newElvis, expr, expr.getClass());
		}
		return result;
	}
	
	private static JCExpression getFalsePart(JavacTreeMaker maker, Type falsePartType, int pos) {
		JCExpression falsePart;
		if (falsePartType instanceof ClassType || falsePartType instanceof ArrayType ||
				falsePartType instanceof CapturedType) {
			falsePart = newNullLiteral(maker, pos);
		} else {
			falsePart = getDefaultValue(maker, falsePartType.getKind(), pos);
		}
		return falsePart;
	}
	
	private static JCExpression newIfNullThenConditional(
			JavacTreeMaker maker, JavacAST ast,
			JCExpression checkable, JCExpression truePart, JCExpression falsePart
	) {
		
		JCBinary baseCheck = maker.Binary(CTC_NOT_EQUAL, checkable, newNullLiteral(maker, checkable.pos));
		baseCheck.pos = checkable.pos;
		final JCExpression result;
		if (truePart instanceof JCArrayAccess) {
			JCArrayAccess arrayAccess = (JCArrayAccess) truePart;
			JCExpression indexed = arrayAccess.indexed;
			JCFieldAccess length = maker.Select(indexed, ast.toName("length"));
			length.pos = indexed.pos;
			JCExpression index = arrayAccess.index;
			
			Integer value = getIntConstant(index);
			
			if (value != null) {
				if (value < 0) {
					result = maker.Literal(false);
				} else {
					result = newAnd(maker, baseCheck, newLess(maker, index, length));
				}
			} else {
				result = newAnd(maker, baseCheck, newIndexRange(maker, length, index));
			}
		} else {
			result = baseCheck;
		}
		
		return newConditional(maker, result, truePart, falsePart);
	}
	
	private static JCBinary newIndexRange(JavacTreeMaker maker, JCFieldAccess length, JCExpression index) {
		return newAnd(maker, newGE(maker, index, newZeroLiteral(maker, index.pos)), newLess(maker, index, length));
	}
	
	private static JCBinary newAnd(JavacTreeMaker maker, JCExpression left, JCExpression right) {
		JCBinary binary = maker.Binary(CTC_AND, left, right);
		binary.pos = left.pos;
		return binary;
	}
	
	private static JCBinary newLess(JavacTreeMaker maker, JCExpression left, JCFieldAccess right) {
		JCBinary checkMax = maker.Binary(CTC_LESS_THAN, left, right);
		checkMax.pos = left.pos;
		return checkMax;
	}
	
	private static Integer getIntConstant(JCExpression expression) {
		Integer value = null;
		if (expression instanceof JCLiteral) {
			JCLiteral literal = (JCLiteral) expression;
			Object litVal = literal.value;
			if (litVal instanceof Number) {
				Number num = (Number) litVal;
				value = num.intValue();
				//minIsZero = value.equals(0);
			}
		}
		return value;
	}
	
	private static JCBinary newGE(JavacTreeMaker maker, JCExpression left, JCExpression right) {
		JCBinary noLessZero = maker.Binary(CTC_GREATER_OR_EQUAL, left, right);
		noLessZero.pos = left.pos;
		return noLessZero;
	}
	
	private static JCExpression newConditional(
			JavacTreeMaker maker, JCExpression condition, JCExpression truePart, JCExpression falsePart) {
		if (condition instanceof JCLiteral) {
			JCLiteral literal = (JCLiteral) condition;
			Type type = condition.type;
			if (type.getKind() == BOOLEAN) {
				Object value = literal.value;
				if (value instanceof Number) {
					int intValue = ((Number) value).intValue();
					if (intValue == 0) return falsePart;
					if (intValue == 1) return truePart;
				}
			}
		}
		JCConditional conditional = maker.Conditional(condition, truePart, falsePart);
		conditional.pos = truePart.pos;
		return conditional;
	}
	
	private static JCLiteral newZeroLiteral(JavacTreeMaker maker, int pos) {
		JCLiteral literal = maker.Literal(0);
		literal.pos = pos;
		return literal;
	}
	
	private static JCLiteral newNullLiteral(JavacTreeMaker maker, int pos) {
		JCLiteral literal = maker.Literal(CTC_BOT, null);
		literal.pos = pos;
		return literal;
	}
	
	protected static JCExpression resolveExprType(
			JCExpression expression, JavacNode annotationNode, JavacResolution javacResolution
	) throws SafeCallAbortProcessing {
		JavacNode javacNode = annotationNode.directUp();
		boolean clazz = javacNode.getKind() == AST.Kind.FIELD;
		Type type;
		JCExpression result;
		if (clazz) {
			javacResolution.resolveClassMember(javacNode);
			type = expression.type;
			result = expression;
		} else {
			Map<JCTree, JCTree> treeMap = javacResolution.resolveMethodMember(javacNode);
			JCExpression tExp = (JCExpression) treeMap.get(expression);
			type = tExp.type;
			result = tExp;
		}
		if (type.isErroneous()) {
			String msg = "'" + expression + "' cannot be resolved";
			throw new SafeCallAbortProcessing(resolveExprType, msg);
		} else return result;
	}
	
	private static VarRef populateInitStatements(
			final int level,
			JCVariableDecl rootVar,
			JCExpression expr,
			ListBuffer<JCStatement> statements,
			JavacNode annotationNode,
			JavacResolution javacResolution) {
		Name templateName = rootVar.name;
		JavacAST ast = annotationNode.getAst();
		JavacTreeMaker treeMaker = annotationNode.getTreeMaker();
		Type type = expr.type;
		
		int notDuplicatedLevel = verifyNotDuplicateLevel(templateName, level, annotationNode);
		Name varName = newVarName(templateName, notDuplicatedLevel, annotationNode);
		JCVariableDecl resultVar;
		int lastLevel;
		if (expr instanceof JCMethodInvocation) {
			JCMethodInvocation mi = (JCMethodInvocation) expr;
			JCExpression meth = mi.meth;
			Type mType = meth.type;
			if (mType instanceof MethodType) {
				MethodType methodType = (MethodType) mType;
				List<JCExpression> args = mi.args;
				JCExpression[] newArgs = new JCExpression[args.size()];
				
				boolean varArgs = mi.varargsElement != null;
				lastLevel = populateMethodCallArgs(rootVar, notDuplicatedLevel,
						args, methodType, varArgs, newArgs, statements, annotationNode, javacResolution);
				mi.args = List.from(newArgs);
				
				if (meth instanceof JCFieldAccess) {
					JCFieldAccess fieldAccess = (JCFieldAccess) meth;
					Symbol sym = fieldAccess.sym;
					boolean isStatic = sym.isStatic();
					if (isStatic) {
						fieldAccess.selected = newIndentOfSymbolOwner(ast, sym, fieldAccess.pos);
						resultVar = makeVariableDecl(treeMaker, statements, varName, type, mi);
					} else {
						VarRef varRef = populateFieldAccess(rootVar, javacResolution, annotationNode,
								notDuplicatedLevel, lastLevel, type, (JCFieldAccess) meth, true, mi.args,
								statements);
						resultVar = varRef.var;
						lastLevel = varRef.level;
					}
				} else if (meth instanceof JCIdent) {
					resultVar = makeVariableDecl(treeMaker, statements, varName, type, expr);
				} else {
					throw new SafeCallUnexpectedStateException(populateInitStatementsMethodInvocation,
							expr, meth.getClass());
				}
			} else if (mType instanceof ErrorType) {
				throw new SafeCallAbortProcessing(methodErrorType, expr);
			} else {
				throw new SafeCallUnexpectedStateException(unsupportedMethodType,
						expr, mType != null ? mType.getClass() : null);
			}
		} else if (expr instanceof JCFieldAccess) {
			JCFieldAccess fieldAccess = (JCFieldAccess) expr;
			Symbol sym = fieldAccess.sym;
			boolean isStatic = sym.isStatic();
			if (isStatic) {
				fieldAccess.selected = newIndentOfSymbolOwner(ast, sym, fieldAccess.pos);
				resultVar = makeVariableDecl(treeMaker, statements, varName, type, fieldAccess);
				lastLevel = notDuplicatedLevel;
			} else {
				VarRef varRef = populateFieldAccess(rootVar, javacResolution, annotationNode,
						notDuplicatedLevel, notDuplicatedLevel, type, fieldAccess,
						false, null, statements);
				resultVar = varRef.var;
				lastLevel = varRef.level;
			}
		} else if (
				expr instanceof JCNewClass
				) {
			lastLevel = notDuplicatedLevel;
			resultVar = makeVariableDecl(treeMaker, statements, varName, type, expr);
		} else if (expr instanceof JCNewArray) {
			lastLevel = notDuplicatedLevel;
			JCNewArray newArray = (JCNewArray) expr;
			List<JCExpression> elems = newArray.elems;
			if (elems != null && !elems.isEmpty()) {
				JCExpression[] newElems = new JCExpression[elems.size()];
				Type expectedType = ((ArrayType) type).getComponentType();
				lastLevel = populateArrayInitializer(rootVar, lastLevel, elems, expectedType, newElems, statements,
						annotationNode, javacResolution);
				newArray.elems = List.from(newElems);
			}
			
			List<JCExpression> dimensions = newArray.dims;
			if (dimensions != null && !dimensions.isEmpty()) {
				JCExpression[] newDimensions = new JCExpression[dimensions.size()];
				lastLevel = populateArrayDimensions(rootVar, lastLevel,
						dimensions, getIntType(ast), newDimensions, statements, annotationNode,
						javacResolution);
				newArray.dims = List.from(newDimensions);
			}
			
			resultVar = makeVariableDecl(treeMaker, statements, varName, type, newArray);
		} else if (expr instanceof JCLiteral) {
			//JCLiteral literal = (JCLiteral) expr;
			//if (literal.getKind() == NULL_LITERAL) {
			lastLevel = NOT_USED;
			resultVar = null;
			//} else {
			//	lastLevel = notDuplicatedLevel;
			//	resultVar = makeVariableDecl(treeMaker, statements, varName, type, expr);
			//}
		} else if (isLambda(expr)) {
			lastLevel = NOT_USED;
			resultVar = null;
		} else if (expr instanceof JCIdent) {
			JCIdent resolvedIdent = (JCIdent) expr;
			Symbol sym = resolvedIdent.sym;
			boolean isClass = sym instanceof ClassSymbol;
			boolean isThis = sym instanceof VarSymbol && sym.name.contentEquals("this");
			if (isClass || isThis) {
				lastLevel = NOT_USED;
				resultVar = null;
			} else {
				lastLevel = notDuplicatedLevel;
				resultVar = makeVariableDecl(treeMaker, statements, varName, type, expr);
			}
		} else if (expr instanceof JCParens) {
			JCParens parens = (JCParens) expr;
			VarRef varRef = populateInitStatements(notDuplicatedLevel, rootVar,
					parens.expr, statements, annotationNode, javacResolution);
			if (varRef.var != null) {
				parens.expr = newIdent(treeMaker, varRef);
				JCVariableDecl var = varRef.var;
				parens.expr = var.init;
				var.init = parens;
				resultVar = var;
				lastLevel = varRef.level;
			} else {
				lastLevel = NOT_USED;
				resultVar = null;
			}
		} else if (expr instanceof JCTypeCast) {
			JCTypeCast typeCast = (JCTypeCast) expr;
			VarRef varRef = populateInitStatements(notDuplicatedLevel + 1, rootVar,
					typeCast.expr, statements, annotationNode, javacResolution);
			
			if (varRef.var != null) {
				typeCast.expr = newIdent(treeMaker, varRef);
				
				JCVariableDecl var = varRef.var;
				boolean primitive = isPrimitive(var.vartype);
				
				JCExpression newExpr = primitive ? typeCast : newElvis(treeMaker, ast, typeCast, type);
				
				lastLevel = varRef.level;
				resultVar = makeVariableDecl(treeMaker, statements, varName, type, newExpr);
			} else {
				
				lastLevel = notDuplicatedLevel;
				resultVar = makeVariableDecl(treeMaker, statements, varName, type, typeCast);
			}
		} else if (expr instanceof JCArrayAccess) {
			JCArrayAccess arrayAccess = (JCArrayAccess) expr;
			JCExpression index = arrayAccess.index;
			VarRef indexVarRef = populateInitStatements(notDuplicatedLevel + 1, rootVar,
					index, statements, annotationNode, javacResolution);
			
			if (indexVarRef.var != null) {
				lastLevel = indexVarRef.level;
				JCExpression indexType = indexVarRef.var.vartype;
				boolean primitive = isPrimitive(indexType);
				JCIdent indexVarIdent = newIdent(treeMaker, indexVarRef);
				if (primitive) arrayAccess.index = indexVarIdent;
				else {
					JCExpression newExpr = newElvis(treeMaker, ast, indexVarIdent, indexType.type,
							getIntType(ast));
					lastLevel = verifyNotDuplicateLevel(templateName, ++lastLevel, annotationNode);
					Name indexVarName = newVarName(templateName, lastLevel, annotationNode);
					
					JCVariableDecl positionVar = makeVariableDecl(treeMaker, statements, indexVarName, type, newExpr);
					arrayAccess.index = newIdent(treeMaker, positionVar);
				}
			} else lastLevel = notDuplicatedLevel;
			
			JCExpression indexed = arrayAccess.indexed;
			VarRef indexedVarRef = populateInitStatements(lastLevel + 1, rootVar,
					indexed, statements, annotationNode, javacResolution);
			arrayAccess.indexed = newIdent(treeMaker, indexedVarRef);
			JCExpression checkNullExpr = newElvis(treeMaker, ast, arrayAccess, type);
			
			resultVar = makeVariableDecl(treeMaker, statements, varName, type, checkNullExpr);
			lastLevel = indexedVarRef.level;
		} else if (expr instanceof JCUnary) {
			JCUnary unary = (JCUnary) expr;
			JCExpression expression = unary.arg;
			VarRef varRef = populateInitStatements(notDuplicatedLevel + 1, rootVar, expression,
					statements, annotationNode, javacResolution);
			Type varType;
			if (varRef.var != null) {
				Type expectedType = unary.type;

				if (!expectedType.isPrimitive()) {
					Symbol operator = getOperator(unary);
					expectedType = getOperatorType(operator, expr);
				}
				varRef = protectIfPrimitive(annotationNode, varRef, templateName, expression.type,
						expectedType, statements);
				lastLevel = varRef.level;
				unary.arg = newIdent(treeMaker, varRef);
				varType = expectedType;
			} else {
				lastLevel = notDuplicatedLevel;
				varType = type;
			}
			resultVar = makeVariableDecl(treeMaker, statements, varName, varType, unary);
		} else {
			throw new SafeCallIllegalUsingException(unsupportedExpression, expr);
		}
		return new VarRef(resultVar, lastLevel);
	}
	
	private static Symbol getOperator(JCTree.JCUnary unary)  {
		try {
			return (Symbol) unary.getClass().getField("operator").get(unary);
		} catch (IllegalAccessException e) {
			throw Lombok.sneakyThrow(e);
		} catch (NoSuchFieldException e) {
			throw Lombok.sneakyThrow(e);
		}
	}
	
	private static JCExpression newIndentOfSymbolOwner(JavacAST ast, Symbol sym, int pos) {
		ClassSymbol owner = (ClassSymbol) sym.owner;
		Name fullname = owner.fullname;
		JCExpression javaType = createJavaType(fullname.toString(), ast);
		javaType.pos = pos;
		return javaType;
	}
	
	private static Type getOperatorType(Symbol operator, JCExpression expr) {

		if (operator == null || !operator.getClass().getSimpleName().equals("OperatorSymbol")) {
			throw new SafeCallIllegalUsingException(unsupportedUnaryOperatorSymbol(expr, operator), expr);
		}

		Type opSymType = operator.type;
		if (!(opSymType instanceof MethodType)) {
			throw new SafeCallIllegalUsingException(unsupportedUnaryOperatorType(expr, opSymType), expr);
		}
		MethodType opSymMethodType = (MethodType) opSymType;
		return opSymMethodType.getReturnType();
	}
	
	private static VarRef protectIfPrimitive(
			JavacNode annotationNode, VarRef varRef, Name templateName, Type actualType,
			Type expectedType, ListBuffer<JCStatement> statements) {
		if (expectedType.isPrimitive() && !actualType.isPrimitive()) {
			JavacTreeMaker treeMaker = annotationNode.getTreeMaker();
			JavacAST ast = annotationNode.getAst();
			JCIdent ref = newIdent(treeMaker, varRef);
			JCExpression elvis = newElvis(treeMaker, ast, ref, actualType, expectedType);
			int notDuplicateLevel = verifyNotDuplicateLevel(templateName, varRef.level + 1, annotationNode);
			Name newVarName = newVarName(templateName, notDuplicateLevel, annotationNode);
			JCVariableDecl variableDecl = makeVariableDecl(treeMaker, statements, newVarName, expectedType, elvis);
			varRef = new VarRef(variableDecl, notDuplicateLevel);
			
		}
		return varRef;
	}
	
	private static Type getIntType(JavacAST ast) {
		return (Type) ast.getTypesUtil().getPrimitiveType(INT);
	}
	
	
	private static int populateMethodCallArgs(
			JCVariableDecl rootVar, int notDuplicatedLevel, List<JCExpression> args,
			MethodType type,
			boolean varArg,
			JCExpression[] resultArgs, ListBuffer<JCStatement> statements,
			JavacNode annotationNode, JavacResolution javacResolution) {
		
		List<Type> argtypes = type.argtypes;
		
		int lastLevel;
		
		int elemLevel = notDuplicatedLevel;
		
		int elemIndex = 0;
		for (JCExpression arg : args) {
			Type expectedType = getParamType(rootVar, varArg, argtypes, elemIndex);
			elemLevel = populateArgument(rootVar, elemLevel, arg, expectedType, resultArgs,
					elemIndex,
					statements, annotationNode, javacResolution
			);
			elemIndex++;
		}
		lastLevel = elemLevel;
		return lastLevel;
	}
	
	private static Type getParamType(JCVariableDecl rootVar, boolean varArg, List<Type> argtypes, int elemIndex) {
		Type expectedType;
		if (varArg) {
			int lastParameterIndex = argtypes.length() - 1;
			if (elemIndex < lastParameterIndex) {
				expectedType = argtypes.get(elemIndex);
			} else {
				Type varArgType = argtypes.get(lastParameterIndex);
				if (!(varArgType instanceof ArrayType)) {
					throw new SafeCallInternalException(rootVar, "last method parameter is not array");
				}
				ArrayType arrayType = (ArrayType) varArgType;
				expectedType = arrayType.elemtype;
			}
		} else {
			expectedType = argtypes.get(elemIndex);
		}
		return expectedType;
	}
	
	private static int populateArrayInitializer(
			JCVariableDecl rootVar, int notDuplicatedLevel, List<JCExpression> args,
			Type expectedType, JCExpression[] resultArgs, ListBuffer<JCStatement> statements,
			JavacNode annotationNode, JavacResolution javacResolution) {
		int lastLevel;
		int elemLevel = notDuplicatedLevel;
		
		int elemIndex = 0;
		for (JCExpression arg : args) {
			elemLevel = populateArgument(rootVar, elemLevel, arg, expectedType, resultArgs,
					elemIndex++, statements, annotationNode, javacResolution
			);
			
		}
		lastLevel = elemLevel;
		return lastLevel;
	}
	
	private static int populateArrayDimensions(
			JCVariableDecl rootVar, int notDuplicatedLevel, List<JCExpression> args,
			Type expectedType, JCExpression[] resultArgs, ListBuffer<JCStatement> statements,
			JavacNode annotationNode, JavacResolution javacResolution) {
		int lastLevel;
		int elemLevel = notDuplicatedLevel;
		
		int elemIndex = 0;
		for (JCExpression arg : args) {
			
			elemLevel = populateArgument(rootVar, elemLevel, arg, expectedType, resultArgs,
					elemIndex, statements, annotationNode, javacResolution
			);
			
			JCExpression resultArg = resultArgs[elemIndex];
			JavacTreeMaker treeMaker = annotationNode.getTreeMaker();
			int pos = resultArg.pos;
			
			final JCExpression condition;
			JCExpression baseCnd = newConditional(treeMaker, newGE(treeMaker, resultArg,
					newZeroLiteral(treeMaker, pos)), resultArg,
					newZeroLiteral(treeMaker, pos));
			
			Integer intConstant = getIntConstant(resultArg);
			if (intConstant != null) {
				if (intConstant < 0) {
					condition = newZeroLiteral(treeMaker, pos);
				} else {
					condition = resultArg;
				}
			} else {
				condition = baseCnd;
			}
			
			Name templateName = rootVar.name;
			elemLevel = verifyNotDuplicateLevel(templateName, elemLevel + 1, annotationNode);
			Name conditionVarName = newVarName(templateName, elemLevel, annotationNode);
			
			JCVariableDecl conditionVar = makeVariableDecl(treeMaker, statements, conditionVarName,
					getIntType(annotationNode.getAst()), condition);
			resultArgs[elemIndex] = newIdent(treeMaker, conditionVar);
			elemIndex++;
		}
		lastLevel = elemLevel;
		return lastLevel;
	}
	
	private static int populateArgument(
			JCVariableDecl rootVar, int level,
			JCExpression arg, Type expectedType,
			JCExpression[] resultArgs, int resultPosition,
			ListBuffer<JCStatement> statements,
			JavacNode annotationNode, JavacResolution javacResolution) {
		Name templateName = rootVar.name;
		JavacTreeMaker treeMaker = annotationNode.getTreeMaker();
		VarRef varRef = populateInitStatements(level + 1, rootVar, arg,
				statements, annotationNode, javacResolution);
		JCVariableDecl var = varRef.var;
		JCExpression newElem;
		if (var != null) {
			JCExpression vartypeExpr = var.vartype;
			Type varType = vartypeExpr.type;
			boolean mustBeConditional = expectedType.isPrimitive() &&
					!varType.isPrimitive();
			
			if (mustBeConditional) {
				int conditionalLevel = verifyNotDuplicateLevel(templateName,
						varRef.level + 1, annotationNode);
				Name conditionalVarName = newVarName(templateName, conditionalLevel, annotationNode);
				
				JCIdent ident = newIdent(treeMaker, varRef);
				JCExpression checkNullExpr = newElvis(treeMaker, annotationNode.getAst(), ident, varType,
						expectedType);
				
				JCVariableDecl conditionalVar = makeVariableDecl(treeMaker, statements,
						conditionalVarName, expectedType, checkNullExpr);
				
				newElem = newIdent(treeMaker, conditionalVar);
				level = conditionalLevel;
			} else {
				newElem = newIdent(treeMaker, varRef);
				level = varRef.level;
			}
		} else newElem = arg;
		resultArgs[resultPosition] = newElem;
		return level;
	}
	
	private static boolean isLambda(JCExpression expr) {
		return expr != null && expr.getClass().getSimpleName().equals("JCLambda");
	}
	
	private static JCFieldAccess newSelect(JavacTreeMaker treeMaker, JCFieldAccess fa, Name childName) {
		JCFieldAccess select = treeMaker.Select(newIdent(treeMaker, childName, fa.pos), fa.name);
		select.pos = fa.pos;
		return select;
	}
	
	private static JCIdent newIdent(JavacTreeMaker treeMaker, Name childName, int pos) {
		JCIdent ident = treeMaker.Ident(childName);
		ident.pos = pos;
		return ident;
	}
	
	private static JCIdent newIdent(JavacTreeMaker treeMaker, JCVariableDecl var) {
		JCIdent ident = newIdent(treeMaker, var.name, var.pos);
		ident.pos = var.pos;
		return ident;
	}
	
	private static JCIdent newIdent(JavacTreeMaker treeMaker, VarRef varRef) {
		return newIdent(treeMaker, varRef.var);
	}
	
	private static JCVariableDecl makeVariableDecl(
			JavacTreeMaker treeMaker, ListBuffer<JCStatement> statements,
			Name varName, Type type, JCExpression expression) {
		JCVariableDecl varDecl;
		varDecl = newVarDecl(treeMaker, varName, type, expression);
		statements.add(varDecl);
		return varDecl;
	}
	
	private static Name newVarName(Name name, int notDuplicatedLevel, JavacNode annotationNode) {
		return annotationNode.toName(newVarName(name.toString(), notDuplicatedLevel));
	}
	
	private static int verifyNotDuplicateLevel(final Name name, final int level, JavacNode annotationNode) {
		int varLevel = level;
		String base = name.toString();
		String newName = newVarName(base, varLevel);
		JavacNode varNode = annotationNode.up();
		JavacNode upNode = varNode.up();
		JavacNode rootNode = upNode;
		while (!TYPE.equals(upNode.getKind())) {
			rootNode = upNode;
			upNode = upNode.up();
		}
		
		JCTree root = rootNode.get();
		
		Collection<JCVariableDecl> vars = findDuplicateCandidates((JCVariableDecl) varNode.get(), root);
		boolean hasDuplicate;
		do {
			hasDuplicate = false;
			for (VariableTree var : vars) {
				String nameStr = var.getName().toString();
				if (nameStr.equals(newName)) {
					hasDuplicate = true;
					break;
				}
			}
			if (hasDuplicate) {
				varLevel++;
				if (varLevel < 0) {
					varLevel = level;
					base += "_";
				}
				newName = newVarName(base, varLevel);
			}
		} while (hasDuplicate);
		
		return varLevel;
	}
	
	private static Collection<JCVariableDecl> findDuplicateCandidates(JCVariableDecl waterline, JCTree parent) {
		Collection<JCVariableDecl> vars = new ArrayList<JCVariableDecl>();
		findDuplicateCandidates(waterline, parent, vars);
		return vars;
		
	}
	
	private static boolean findDuplicateCandidates(
			JCVariableDecl waterline, JCTree tree, Collection<JCVariableDecl> vars
	) {
		if (tree == null) {
			throw new IllegalArgumentException("tree is null");
		}
		if (tree instanceof JCBlock) {
			return findDuplicateCandidates(waterline, ((JCBlock) tree).getStatements(), vars);
		} else if (tree instanceof JCMethodDecl) {
			return findDuplicateCandidates(waterline, ((JCMethodDecl) tree).getBody(), vars);
		} else if (tree instanceof ExpressionStatementTree) {
			return false;
		} else if (tree instanceof ClassTree) {
			return false;
		} else if (tree instanceof JCIf) {
			JCIf jcIf = (JCIf) tree;
			boolean endReached = findDuplicateCandidates(waterline, jcIf.getThenStatement(), vars);
			JCStatement elsepart = jcIf.getElseStatement();
			if (!endReached && elsepart != null) endReached = findDuplicateCandidates(waterline, elsepart, vars);
			return endReached;
		} else if (tree instanceof JCVariableDecl) {
			return findDuplicateCandidates(waterline, asList((JCVariableDecl) tree), vars);
		} else if (tree instanceof JCForLoop) {
			JCForLoop forLoop = (JCForLoop) tree;
			ArrayList<JCStatement> statements = new ArrayList<JCStatement>();
			statements.addAll(forLoop.getInitializer());
			statements.add(forLoop.getStatement());
			return findDuplicateCandidates(waterline, statements, vars);
		} else if (tree instanceof JCEnhancedForLoop) {
			JCEnhancedForLoop forLoop = (JCEnhancedForLoop) tree;
			ArrayList<JCStatement> statements = new ArrayList<JCStatement>();
			statements.add(forLoop.var);
			statements.add(forLoop.getStatement());
			
			return findDuplicateCandidates(waterline, statements, vars);
		} else if (tree instanceof JCWhileLoop) {
			JCWhileLoop whileLoopTree = (JCWhileLoop) tree;
			return findDuplicateCandidates(waterline, asList((JCStatement) whileLoopTree.getStatement()), vars);
		} else if (tree instanceof JCDoWhileLoop) {
			JCDoWhileLoop doWhileLoop = (JCDoWhileLoop) tree;
			return findDuplicateCandidates(waterline, doWhileLoop.body, vars);
		} else if (tree instanceof JCTry) {
			JCTry jcTry = (JCTry) tree;
			boolean stop = findDuplicateCandidates(waterline, jcTry.body, vars);
			if (!stop) for (JCCatch catcher : jcTry.catchers) {
				stop = findDuplicateCandidates(waterline, catcher.body, vars);
				if (stop) return stop;
			}
			JCBlock finalizer = jcTry.finalizer;
			if (!stop && finalizer != null) {
				stop = findDuplicateCandidates(waterline, finalizer, vars);
			}
			return stop;
		} else if (tree instanceof JCSwitch) {
			JCSwitch jcSwitch = (JCSwitch) tree;
			for (JCCase jcCase : jcSwitch.cases) {
				boolean stop = findDuplicateCandidates(waterline, jcCase.getStatements(), vars);
				if (stop) return true;
			}
			return false;
		} else if (
				tree instanceof JCBreak || tree instanceof JCContinue || tree instanceof JCSkip ||
						tree instanceof JCThrow
				) {
			return false;
		} else if (tree instanceof JCSynchronized) {
			JCSynchronized sync = (JCSynchronized) tree;
			return findDuplicateCandidates(waterline, sync.body, vars);
		} else {
			throw new SafeCallUnexpectedStateException(findDuplicateCandidates, waterline, tree.getClass());
		}
	}
	
	private static boolean findDuplicateCandidates(
			JCVariableDecl waterline,
			Collection<? extends JCStatement> statements,
			Collection<JCVariableDecl> vars) {
		Collection<JCVariableDecl> foundVars = new ArrayList<JCVariableDecl>();
		
		boolean found = false;
		for (JCStatement statement : statements) {
			if (statement instanceof JCVariableDecl) {
				JCVariableDecl var = (JCVariableDecl) statement;
				JCExpression init = var.init;
				if (isLambda(init)) {
					JCTree lambdaBody = getLambdaBody(init);
					Collection<JCVariableDecl> childVars = new ArrayList<JCVariableDecl>();
					boolean foundInChild = findDuplicateCandidates(waterline, lambdaBody, childVars);
					if (foundInChild) {
						found = true;
						foundVars.addAll(childVars);
						break;
					}
				} else /*if (init == null ||
						init instanceof JCLiteral ||
						init instanceof JCIdent ||
						init instanceof JCMethodInvocation ||
						init instanceof JCFieldAccess ||
						init instanceof JCConditional ||
						init instanceof JCArrayAccess ||
						init instanceof JCNewArray ||
						init instanceof JCTypeCast ||
						init instanceof JCParens ||
						init instanceof JCNewClass
						)*/ {
					if (var == waterline) {
						found = true;
						break;
					} else foundVars.add(var);
//				} else {
//					throw new SafeCallUnexpectedStateException(findDuplicateCandidates, var, init.getClass());
				}
			} else {
				Collection<JCVariableDecl> childVars = new ArrayList<JCVariableDecl>();
				boolean foundInChild = findDuplicateCandidates(waterline, statement, childVars);
				if (foundInChild) {
					found = true;
					foundVars.addAll(childVars);
					break;
				}
			}
		}
		
		if (found) {
			vars.addAll(foundVars);
		}
		return found;
	}
	
	private static JCTree getLambdaBody(JCExpression init) {
		Method method = null;
		try {
			method = init.getClass().getMethod("getBody");
		} catch (NoSuchMethodException e) {
			throw new IllegalStateException(e);
		}
		JCTree body;
		try {
			body = (JCTree) method.invoke(init);
		} catch (IllegalAccessException e) {
			throw new IllegalStateException(e);
		} catch (InvocationTargetException e) {
			throw new IllegalStateException(e);
		}
		return body;
	}
	
	private static String newVarName(String name, int level) {
		return name + level;
	}
	
	private static JCVariableDecl newVarDecl(JavacTreeMaker treeMaker, Name name, Type type, JCExpression expr) {
		Type checkType;
		if (type instanceof ExecutableType) checkType = type.getReturnType();
		else if (type instanceof CapturedType) checkType = type.getUpperBound();
		else checkType = type;
		
		boolean nonameOwner = hasNonameOwner(checkType);
		
		JCExpression vartype;
		if (nonameOwner) {
			vartype = newIdent(treeMaker, checkType.asElement().name, expr.pos);
		} else {
			JCExpression type1 = treeMaker.Type(checkType);
			//remove dot if it is at start of type
			vartype = removeStartDot(treeMaker, type1);
		}
		
		vartype.pos = expr.pos;
		JCVariableDecl variableDecl = treeMaker.VarDef(treeMaker.Modifiers(0), name, vartype, expr);
		variableDecl.pos = expr.pos;
		return variableDecl;
	}
	
	private static boolean hasNonameOwner(Type type) {
		boolean nonameOwner = false;
		if (type instanceof ClassType || type instanceof CapturedType) {
			Symbol owner = type.tsym.owner;
			nonameOwner = owner.name.isEmpty();
		} else if (type instanceof ArrayType) {
			ArrayType arrayType = (ArrayType) type;
			Type componentType = arrayType.getComponentType();
			nonameOwner = hasNonameOwner(componentType);
		}
		return nonameOwner;
	}
	
	private static JCExpression removeStartDot(JavacTreeMaker treeMaker, JCExpression expression) {
		if (expression instanceof JCTypeApply) {
			JCTypeApply typeApply = (JCTypeApply) expression;
			typeApply.clazz = removeStartDot(treeMaker, typeApply.clazz);
			List<JCExpression> arguments = typeApply.arguments;
			JCExpression[] newArguments = new JCExpression[arguments.length()];
			int index = 0;
			for (JCExpression argument : arguments) {
				newArguments[index++] = removeStartDot(treeMaker, argument);
			}
			typeApply.arguments = List.from(newArguments);
		} else if (expression instanceof JCWildcard) {
			JCWildcard wildcard = (JCWildcard) expression;
			JCTree inner = wildcard.inner;
			if (inner instanceof JCExpression) {
				wildcard.inner = removeStartDot(treeMaker, (JCExpression) inner);
			}
		} else if (expression instanceof JCFieldAccess) {
			JCFieldAccess fieldAccess = (JCFieldAccess) expression;
			JCExpression selected = fieldAccess.selected;
			JCFieldAccess parent = null;
			while (selected instanceof JCFieldAccess) {
				parent = fieldAccess;
				fieldAccess = (JCFieldAccess) selected;
				selected = fieldAccess.selected;
			}
			if (parent != null && selected instanceof JCIdent) {
				JCIdent lastIdent = (JCIdent) selected;
				if (lastIdent.name.isEmpty()) {
					lastIdent = newIdent(treeMaker, fieldAccess.name, fieldAccess.pos);
					parent.selected = lastIdent;
				}
				
			}
		} else if (expression instanceof JCArrayTypeTree) {
			JCArrayTypeTree arrayTypeTree = (JCArrayTypeTree) expression;
			arrayTypeTree.elemtype = removeStartDot(treeMaker, arrayTypeTree.elemtype);
		}
		return expression;
	}
	
	private static VarRef populateFieldAccess(
			JCVariableDecl rootVar, JavacResolution javacResolution, JavacNode annotationNode,
			int fieldVarLevel, int lastLevel, Type type, JCFieldAccess fa, boolean isMeth,
			List<JCExpression> args, ListBuffer<JCStatement> statements) {
		Name templateName = rootVar.name;
		JavacTreeMaker treeMaker = annotationNode.getTreeMaker();
		JCExpression selected = fa.selected;
		VarRef varRef = populateInitStatements(lastLevel + 1, rootVar, selected, statements, annotationNode,
				javacResolution);
		JCExpression variableExpr;
		if (varRef.var != null) {
			Name childName = varRef.getVarName();
			JCFieldAccess newFa = newSelect(treeMaker, fa, childName);
			newFa.type = annotationNode.getSymbolTable().objectType;
			JCExpression newExpr = isMeth ? args != null ? treeMaker.App(newFa, args) : treeMaker.App(newFa) : newFa;
			newExpr.pos = newFa.pos;
			variableExpr = newElvis(treeMaker, annotationNode.getAst(), newExpr, type);
		} else if (isMeth) {
			fa.type = type;
			variableExpr = args != null ? treeMaker.App(fa, args) : treeMaker.App(fa);
			variableExpr.pos = fa.pos;
		} else variableExpr = fa;
		
		Name newName = newVarName(templateName, fieldVarLevel, annotationNode);
		JCVariableDecl variableDecl = makeVariableDecl(treeMaker, statements, newName, type, variableExpr);
		int maxLevel = varRef.level > fieldVarLevel ? varRef.level : fieldVarLevel;
		return new VarRef(variableDecl, maxLevel);
		
	}
	
	static JCBlock newInitBlock(
			JCVariableDecl varDecl, final JCExpression defaultValue,
			JavacNode annotationNode
	) {
		JavacResolution javacResolution = new JavacResolution(annotationNode.getContext());
		
		JavacTreeMaker maker = annotationNode.getTreeMaker();
		ListBuffer<JCStatement> statements = new ListBuffer<JCStatement>();
		JCExpression expr = varDecl.init;
		if (expr == null) return null;
		
		if (defaultValue != null) {
			checkDefaultValueType(varDecl, defaultValue, annotationNode, javacResolution);
		}
		
		JCExpression resolveExpr = resolveExprType(expr, annotationNode, javacResolution);
		
		VarRef initVarRef = populateInitStatements(1, varDecl, resolveExpr, statements,
				annotationNode, javacResolution);
		Name name = initVarRef.getVarName();
		if (name == null) return null;
		boolean removeOnlyOneStatement = statements.length() == 1;
		
		JCExpression rhs = newIdent(maker, initVarRef);
		JCExpression lhs = newIdent(maker, varDecl);
		JCExpression varType = varDecl.vartype;
		boolean isVarTypePrimitive = isPrimitive(varType);
		if (isVarTypePrimitive && !isPrimitive(initVarRef.var.vartype)) {
			TypeKind kind;
			if (varType instanceof PrimitiveTypeTree) {
				PrimitiveTypeTree type = (PrimitiveTypeTree) varType;
				kind = type.getPrimitiveTypeKind();
			} else if (varType.type == null) {
				throw new SafeCallUnexpectedStateException(cannotRecognizeType, varType, varType.getClass());
			} else kind = varType.type.getKind();
			
			rhs = newIfNullThenConditional(maker, annotationNode.getAst(), rhs, rhs,
					defaultValue != null ? defaultValue : getDefaultValue(maker, kind, rhs.pos));
			removeOnlyOneStatement = false;
		} else if (!removeOnlyOneStatement && defaultValue != null) {
			if (isVarTypePrimitive) {
				JCExpression init = initVarRef.var.init;
				if (init instanceof JCConditional) {
					JCConditional conditional = (JCConditional) init;
					conditional.falsepart = defaultValue;
				}
			} else {
				rhs = newIfNullThenConditional(maker, annotationNode.getAst(), rhs, rhs, defaultValue);
			}
		}
		
		if (removeOnlyOneStatement) return null;
		JCExpressionStatement assign = maker.Exec(maker.Assign(lhs, rhs));
		
		statements.add(assign);
		boolean isStatic = (varDecl.mods.flags & STATIC) != 0;
		int flags = isStatic ? STATIC : 0;
		JCBlock block = maker.Block(flags, statements.toList());
		block.pos = varDecl.pos;
		return block;
	}
	
	private static void checkDefaultValueType(JCVariableDecl varDecl,
	                                          JCExpression defaultValue,
	                                          JavacNode annotationNode, JavacResolution javacResolution) {
		JCExpression init = varDecl.init;
		try {
			varDecl.init = defaultValue;
			final JCTree.JCExpression resolved = resolveExprType(defaultValue, annotationNode, javacResolution);
			
			if (!(isStaticField(resolved) || resolved instanceof JCTree.JCIdent || resolved instanceof JCTree.JCLiteral)) {
				throw new SafeCallIllegalUsingException(
						incorrectFalseExprType(resolved.getClass()),
						defaultValue);
			}
			
			if (isPrimitive(varDecl.vartype) && !resolved.type.isPrimitive()) {
				throw new SafeCallIllegalUsingException(
						incorrectFalseNotPrimitive(resolved.type.toString()),
						defaultValue);
			}
		} finally {
			varDecl.init = init;
		}
	}
	
	private static boolean isStaticField(JCExpression resolved) {
		if (resolved instanceof JCFieldAccess) {
			JCFieldAccess fieldAccess = (JCFieldAccess) resolved;
			Symbol sym = fieldAccess.sym;
			if (sym instanceof VarSymbol) {
				VarSymbol varSymbol = (VarSymbol) sym;
				return varSymbol.isStatic();
			}
		}
		return false;
	}
	
	static <T> List<T> addBlockAfterVarDec(T varDecl, T initBlock, List<T> members) {
		ListBuffer<T> newMembers = new ListBuffer<T>();
		for (T tree : members) {
			newMembers.add(tree);
			if (tree == varDecl) {
				newMembers.add(initBlock);
			}
		}
		return newMembers.toList();
	}
	
	private static class VarRef {
		final JCVariableDecl var;
		final private int level;
		
		VarRef(JCVariableDecl var, int level) {
			this.var = var;
			this.level = level;
		}
		
		Name getVarName() {
			return var != null ? var.name : null;
		}
	}
}

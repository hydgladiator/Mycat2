package cn.lightfish.sqlEngine.ast.expr.booleanExpr.logicalExpr;

import cn.lightfish.sqlEngine.context.RootSessionContext;
import cn.lightfish.sqlEngine.ast.expr.booleanExpr.BooleanExpr;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class BooleanAndExpr implements BooleanExpr {

  private final RootSessionContext context;
  private final BooleanExpr left;
  private final BooleanExpr right;

  @Override
  public Boolean test() {
    Boolean leftValue = left.test();
    if (leftValue == null) {
      return false;
    }
    Boolean rightValue = right.test();
    if (rightValue == null) {
      return false;
    }
    return leftValue && rightValue;
  }
}
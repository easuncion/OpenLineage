package io.openlineage.spark.agent.lifecycle;

import com.google.common.collect.ImmutableList;
import io.openlineage.client.OpenLineage;
import io.openlineage.client.OpenLineage.InputDataset;
import io.openlineage.client.OpenLineage.OutputDataset;
import io.openlineage.spark.agent.lifecycle.plan.LogicalRelationVisitor;
import io.openlineage.spark.api.DatasetFactory;
import io.openlineage.spark.api.OpenLineageContext;
import io.openlineage.spark2.agent.lifecycle.plan.CreateTableLikeCommandVisitor;
import io.openlineage.spark2.agent.lifecycle.plan.DatasetSourceVisitor;
import java.util.List;
import org.apache.spark.sql.catalyst.plans.logical.LogicalPlan;
import scala.PartialFunction;

class Spark2VisitorFactoryImpl extends BaseVisitorFactory {

  @Override
  public List<PartialFunction<LogicalPlan, List<OutputDataset>>> getOutputVisitors(
      OpenLineageContext context) {
    DatasetFactory<OutputDataset> outputFactory = DatasetFactory.output(context.getOpenLineage());
    return ImmutableList.<PartialFunction<LogicalPlan, List<OpenLineage.OutputDataset>>>builder()
        .addAll(super.getOutputVisitors(context))
        .add(new DatasetSourceVisitor(context, DatasetFactory.output(context.getOpenLineage())))
        .add(new CreateTableLikeCommandVisitor(context))
        .add(new LogicalRelationVisitor(context, outputFactory, false))
        .build();
  }

  @Override
  public List<PartialFunction<LogicalPlan, List<InputDataset>>> getInputVisitors(
      OpenLineageContext context) {
    DatasetFactory<InputDataset> inputFactory = DatasetFactory.input(context.getOpenLineage());
    return ImmutableList.<PartialFunction<LogicalPlan, List<InputDataset>>>builder()
        .addAll(super.getInputVisitors(context))
        .add(new LogicalRelationVisitor(context, inputFactory, true))
        .build();
  }

  public <D extends OpenLineage.Dataset>
      List<PartialFunction<LogicalPlan, List<D>>> getCommonVisitors(
          OpenLineageContext context, DatasetFactory<D> factory) {
    return ImmutableList.<PartialFunction<LogicalPlan, List<D>>>builder()
        .addAll(super.getBaseCommonVisitors(context, factory))
        .add(new DatasetSourceVisitor(context, factory))
        .build();
  }
}

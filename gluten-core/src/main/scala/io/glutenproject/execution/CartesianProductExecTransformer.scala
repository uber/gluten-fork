/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.glutenproject.execution

import io.glutenproject.backendsapi.BackendsApiManager
import io.glutenproject.expression.ExpressionConverter
import io.glutenproject.extension.ValidationResult
import io.glutenproject.metrics.MetricsUpdater
import io.glutenproject.substrait.SubstraitContext
import io.glutenproject.substrait.rel.RelBuilder

import org.apache.spark.{Dependency, NarrowDependency, Partition, SparkContext, TaskContext}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.catalyst.expressions.{Attribute, Expression}
import org.apache.spark.sql.catalyst.plans.{Inner, JoinType}
import org.apache.spark.sql.execution.SparkPlan
import org.apache.spark.sql.execution.joins.BaseJoinExec
import org.apache.spark.sql.execution.metric.SQLMetric
import org.apache.spark.sql.vectorized.ColumnarBatch

case class CartesianProductExecTransformer(
    left: SparkPlan,
    right: SparkPlan,
    condition: Option[Expression])
  extends BaseJoinExec
  with TransformSupport {

  override def joinType: JoinType = Inner

  override def leftKeys: Seq[Expression] = Nil

  override def rightKeys: Seq[Expression] = Nil

  // Note: "metrics" is made transient to avoid sending driver-side metrics to tasks.
  @transient override lazy val metrics: Map[String, SQLMetric] =
    BackendsApiManager.getMetricsApiInstance.genCartesianProductTransformerMetrics(sparkContext)

  override def metricsUpdater(): MetricsUpdater = {
    BackendsApiManager.getMetricsApiInstance.genCartesianProductTransformerMetricsUpdater(metrics)
  }

  override def doTransform(context: SubstraitContext): TransformContext = {
    val leftPlanContext = left.asInstanceOf[TransformSupport].doTransform(context)
    val (inputLeftRelNode, inputLeftOutput) =
      (leftPlanContext.root, leftPlanContext.outputAttributes)

    val rightPlanContext = right.asInstanceOf[TransformSupport].doTransform(context)
    val (inputRightRelNode, inputRightOutput) =
      (rightPlanContext.root, rightPlanContext.outputAttributes)

    val expressionNode = condition.map {
      expr =>
        ExpressionConverter
          .replaceWithExpressionTransformer(expr, inputLeftOutput ++ inputRightOutput)
          .doTransform(context.registeredFunction)
    }

    val extensionNode =
      JoinUtils.createExtensionNode(inputLeftOutput ++ inputRightOutput, validation = false)

    val operatorId = context.nextOperatorId(this.nodeName)

    val currRel = RelBuilder.makeCrossRel(
      inputLeftRelNode,
      inputRightRelNode,
      expressionNode.orNull,
      extensionNode,
      context,
      operatorId
    )
    TransformContext(inputLeftOutput ++ inputRightOutput, output, currRel)
  }

  override protected def doValidateInternal(): ValidationResult = {
    if (!BackendsApiManager.getSettings.supportCartesianProductExec()) {
      return ValidationResult.notOk("Cartesian product is not supported in this backend")
    }
    val substraitContext = new SubstraitContext
    val expressionNode = condition.map {
      expr =>
        ExpressionConverter
          .replaceWithExpressionTransformer(expr, left.output ++ right.output)
          .doTransform(substraitContext.registeredFunction)
    }
    val extensionNode =
      JoinUtils.createExtensionNode(left.output ++ right.output, validation = true)

    val currRel = RelBuilder.makeCrossRel(
      null,
      null,
      expressionNode.orNull,
      extensionNode,
      substraitContext,
      substraitContext.nextOperatorId(this.nodeName)
    )
    doNativeValidation(substraitContext, currRel)
  }

  override def nodeName: String = "CartesianProductExecTransformer"

  override def output: Seq[Attribute] = left.output ++ right.output

  override protected def withNewChildrenInternal(
      newLeft: SparkPlan,
      newRight: SparkPlan): CartesianProductExecTransformer =
    copy(left = newLeft, right = newRight)

  override def columnarInputRDDs: Seq[RDD[ColumnarBatch]] = {
    val rddsLeft = getColumnarInputRDDs(left)
    val rddsRight = getColumnarInputRDDs(right)

    if (rddsLeft.isEmpty || rddsRight.isEmpty) {
      // If any of the child RDD is empty then result will also be empty
      return Seq.empty
    }

    /**
     * Both the children of cartesian product should be [[InputIteratorTransformer]] see
     * [[ColumnarCollapseTransformStages]]. InputIteratorTransformer should always return single
     * child RDD. Usually, it should be WholeStageRDD or MapPartitionRDD.
     */
    assert(rddsLeft.size == 1 && rddsRight.size == 1)

    val cartesianRDD = new CartesianColumnarBatchRDD(sparkContext, rddsLeft.head, rddsRight.head)
    Seq(cartesianRDD)
  }
}

/** The [[Partition]] used by [[CartesianColumnarBatchRDD]]. */
class CartesianColumnarBatchRDDPartition(
    idx: Int,
    @transient private val rdd1: RDD[_],
    @transient private val rdd2: RDD[_],
    s1Index: Int,
    s2Index: Int
) extends Partition {
  var s1: Partition = rdd1.partitions(s1Index)
  var s2: Partition = rdd2.partitions(s2Index)
  override val index: Int = idx
}

/**
 * [[CartesianColumnarBatchRDD]] is the columnar version of [[org.apache.spark.rdd.CartesianRDD]].
 */
class CartesianColumnarBatchRDD(
    sc: SparkContext,
    var rdd1: RDD[ColumnarBatch],
    var rdd2: RDD[ColumnarBatch])
  extends RDD[ColumnarBatch](sc, Nil)
  with Serializable {

  private val numPartitionsInRdd2 = rdd2.partitions.length

  override def getPartitions: Array[Partition] = {
    // create the cross product split
    val array = new Array[Partition](rdd1.partitions.length * rdd2.partitions.length)
    for (s1 <- rdd1.partitions; s2 <- rdd2.partitions) {
      val idx = s1.index * numPartitionsInRdd2 + s2.index
      array(idx) = new CartesianColumnarBatchRDDPartition(idx, rdd1, rdd2, s1.index, s2.index)
    }
    array
  }

  override def getPreferredLocations(split: Partition): Seq[String] = {
    val currSplit = split.asInstanceOf[CartesianColumnarBatchRDDPartition]
    (rdd1.preferredLocations(currSplit.s1) ++ rdd2.preferredLocations(currSplit.s2)).distinct
  }

  override def compute(split: Partition, context: TaskContext): Iterator[ColumnarBatch] = {

    /**
     * Cartesian RDD returns both left and right RDD iterators. Due to the override method
     * signature, it is not possible to return Seq from here. see [getIterators] in
     * [[ColumnarInputRDDsWrapper]]
     */
    throw new IllegalStateException("Never reach here")
  }

  override def getDependencies: Seq[Dependency[_]] = List(
    new NarrowDependency(rdd1) {
      def getParents(id: Int): Seq[Int] = List(id / numPartitionsInRdd2)
    },
    new NarrowDependency(rdd2) {
      def getParents(id: Int): Seq[Int] = List(id % numPartitionsInRdd2)
    }
  )

  override def clearDependencies(): Unit = {
    super.clearDependencies()
    rdd1 = null
    rdd2 = null
  }

  def getIterators(split: Partition, context: TaskContext): Seq[Iterator[ColumnarBatch]] = {
    val currSplit = split.asInstanceOf[CartesianColumnarBatchRDDPartition]
    Seq(rdd1.iterator(currSplit.s1, context), rdd2.iterator(currSplit.s2, context))
  }
}

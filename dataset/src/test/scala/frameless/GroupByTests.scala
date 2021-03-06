package frameless

import frameless.functions.aggregate._
import org.scalacheck.Prop
import org.scalacheck.Prop._

class GroupByTests extends TypedDatasetSuite {
  test("groupByMany('a).agg(sum('b))") {
    def prop[
      A: TypedEncoder : Ordering,
      B: TypedEncoder,
      Out: TypedEncoder : Numeric
    ](data: List[X2[A, B]])(
      implicit
      summable: CatalystSummable[B, Out],
      widen: B => Out
    ): Prop = {
      val dataset = TypedDataset.create(data)
      val A = dataset.col[A]('a)
      val B = dataset.col[B]('b)

      val datasetSumByA = dataset.groupByMany(A).agg(sum(B)).collect().run.toVector.sortBy(_._1)
      val sumByA = data.groupBy(_.a).mapValues(_.map(_.b).map(widen).sum).toVector.sortBy(_._1)

      datasetSumByA ?= sumByA
    }

    check(forAll(prop[Int, Long, Long] _))
  }

  test("groupBy('a).agg(sum('b))") {
    def prop[
      A: TypedEncoder : Ordering,
      B: TypedEncoder,
      Out: TypedEncoder : Numeric
    ](data: List[X2[A, B]])(
      implicit
      summable: CatalystSummable[B, Out],
      widen: B => Out
    ): Prop = {
      val dataset = TypedDataset.create(data)
      val A = dataset.col[A]('a)
      val B = dataset.col[B]('b)

      val datasetSumByA = dataset.groupBy(A).agg(sum(B)).collect().run.toVector.sortBy(_._1)
      val sumByA = data.groupBy(_.a).mapValues(_.map(_.b).map(widen).sum).toVector.sortBy(_._1)

      datasetSumByA ?= sumByA
    }

    check(forAll(prop[Int, Long, Long] _))
  }

  test("groupBy('a).mapGroups('a, sum('b))") {
    def prop[
      A: TypedEncoder : Ordering,
      B: TypedEncoder : Numeric
    ](data: List[X2[A, B]]): Prop = {
      val dataset = TypedDataset.create(data)
      val A = dataset.col[A]('a)
      val B = dataset.col[B]('b)

      val datasetSumByA = dataset.groupBy(A)
        .mapGroups { case (a, xs) => (a, xs.map(_.b).sum) }
        .collect().run().toVector.sortBy(_._1)
      val sumByA = data.groupBy(_.a).mapValues(_.map(_.b).sum).toVector.sortBy(_._1)

      datasetSumByA ?= sumByA
    }

    check(forAll(prop[Int, Long] _))
  }

  test("groupBy('a).agg(sum('b), sum('c))") {
    def prop[
      A: TypedEncoder : Ordering,
      B: TypedEncoder,
      C: TypedEncoder,
      OutB: TypedEncoder : Numeric,
      OutC: TypedEncoder: Numeric
    ](data: List[X3[A, B, C]])(
      implicit
      summableB: CatalystSummable[B, OutB],
      summableC: CatalystSummable[C, OutC],
      widenb: B => OutB,
      widenc: C => OutC
    ): Prop = {
      val dataset = TypedDataset.create(data)
      val A = dataset.col[A]('a)
      val B = dataset.col[B]('b)
      val C = dataset.col[C]('c)

      val datasetSumByAB = dataset
        .groupBy(A)
        .agg(sum(B), sum(C))
        .collect().run.toVector.sortBy(_._1)

      val sumByAB = data.groupBy(_.a).mapValues { xs =>
        (xs.map(_.b).map(widenb).sum, xs.map(_.c).map(widenc).sum)
      }.toVector.map {
        case (a, (b, c)) => (a, b, c)
      }.sortBy(_._1)

      datasetSumByAB ?= sumByAB
    }

    check(forAll(prop[String, Long, BigDecimal, Long, BigDecimal] _))
  }

  test("groupBy('a, 'b).agg(sum('c), sum('d))") {
    def prop[
      A: TypedEncoder : Ordering,
      B: TypedEncoder : Ordering,
      C: TypedEncoder,
      D: TypedEncoder,
      OutC: TypedEncoder : Numeric,
      OutD: TypedEncoder : Numeric
    ](data: List[X4[A, B, C, D]])(
      implicit
      summableC: CatalystSummable[C, OutC],
      summableD: CatalystSummable[D, OutD],
      widenc: C => OutC,
      widend: D => OutD
    ): Prop = {
      val dataset = TypedDataset.create(data)
      val A = dataset.col[A]('a)
      val B = dataset.col[B]('b)
      val C = dataset.col[C]('c)
      val D = dataset.col[D]('d)

      val datasetSumByAB = dataset
        .groupBy(A, B)
        .agg(sum(C), sum(D))
        .collect().run.toVector.sortBy(x => (x._1, x._2))

      val sumByAB = data.groupBy(x => (x.a, x.b)).mapValues { xs =>
        (xs.map(_.c).map(widenc).sum, xs.map(_.d).map(widend).sum)
      }.toVector.map {
        case ((a, b), (c, d)) => (a, b, c, d)
      }.sortBy(x => (x._1, x._2))

      datasetSumByAB ?= sumByAB
    }

    check(forAll(prop[Byte, Int, Long, BigDecimal, Long, BigDecimal] _))
  }

  test("groupBy('a, 'b).mapGroups('a, 'b, sum('c))") {
    def prop[
      A: TypedEncoder : Ordering,
      B: TypedEncoder : Ordering,
      C: TypedEncoder : Numeric
    ](data: List[X3[A, B, C]]): Prop = {
      val dataset = TypedDataset.create(data)
      val A = dataset.col[A]('a)
      val B = dataset.col[B]('b)
      val C = dataset.col[C]('c)

      val datasetSumByAB = dataset
        .groupBy(A, B)
        .mapGroups { case ((a, b), xs) => (a, b, xs.map(_.c).sum) }
        .collect().run().toVector.sortBy(x => (x._1, x._2))

      val sumByAB = data.groupBy(x => (x.a, x.b))
        .mapValues { xs => xs.map(_.c).sum }
        .toVector.map { case ((a, b), c) => (a, b, c) }.sortBy(x => (x._1, x._2))

      datasetSumByAB ?= sumByAB
    }

    check(forAll(prop[Byte, Int, Long] _))
  }

  test("groupBy('a).mapGroups(('a, toVector(('a, 'b))") {
    def prop[
      A: TypedEncoder,
      B: TypedEncoder
    ](data: Vector[X2[A, B]]): Prop = {
      val dataset = TypedDataset.create(data)
      val A = dataset.col[A]('a)

      val datasetGrouped = dataset
        .groupBy(A)
        .mapGroups((a, xs) => (a, xs.toVector))
        .collect().run.toMap

      val dataGrouped = data.groupBy(_.a)

      datasetGrouped ?= dataGrouped
    }

    check(forAll(prop[Short, Option[Short]] _))
    check(forAll(prop[Option[Short], Short] _))
    check(forAll(prop[X1[Option[Short]], Short] _))
  }

  test("groupBy('a).flatMapGroups(('a, toVector(('a, 'b))") {
    def prop[
      A: TypedEncoder : Ordering,
      B: TypedEncoder : Ordering
    ](data: Vector[X2[A, B]]): Prop = {
      val dataset = TypedDataset.create(data)
      val A = dataset.col[A]('a)

      val datasetGrouped = dataset
        .groupBy(A)
        .flatMapGroups((a, xs) => xs.map(x => (a, x)))
        .collect().run
        .sorted

      val dataGrouped = data
        .groupBy(_.a).toSeq
        .flatMap { case (a, xs) => xs.map(x => (a, x)) }
        .sorted

      datasetGrouped ?= dataGrouped
    }

    check(forAll(prop[Short, Option[Short]] _))
    check(forAll(prop[Option[Short], Short] _))
    check(forAll(prop[X1[Option[Short]], Short] _))
  }
}

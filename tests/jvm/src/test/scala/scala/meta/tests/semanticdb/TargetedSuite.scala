package scala.meta.tests
package semanticdb

// Contributing tips:
// - To run an individual test: ~testsJVM/testOnly *TargetedSuite -- -z "package YYY"
//   and replace YYY with the package name of your test.
// - On test failure, the obtained output is printed to the console for
//   easy copy-paste to replace the current expected output.
// - Try to follow the alphabetical order of the enclosing package, at the time
//   of this writing the latest package is `j`, so the next package should be `l`.
// - glhf, and if you have any questions don't hesitate to ask in the gitter channel :)
class TargetedSuite extends SemanticdbSuite {

  targeted(
    // curried function application with named args, #648
    """package a
      |object Curry {
      |  def bar(children: Int)(x: Int) = children + x
      |  <<bar>>(children = 4)(3)
      |}
    """.trim.stripMargin, { (_, second) => assert(second == "a/Curry.bar().") }
  )

  targeted(
    """
      |package b
      |case class User(name: String, age: Int)
      |object M {
      |  val u: User = ???
      |  u.<<copy>>(<<age>> = 43)
      |}
    """.trim.stripMargin, { (_, copy, age) =>
      assert(copy == "b/User#copy().")
      assert(age == "b/User#copy().(age)")
    }
  )

  occurrences(
    """|// See https://github.com/scalameta/scalameta/issues/2040
       |package example
       |
       |import scala.language.implicitConversions
       |
       |object Issue2040 {
       |  trait Prettifier
       |  object Prettifier {
       |    implicit val default: Prettifier = ???
       |  }
       |
       |  trait AnyShouldWrapper {
       |    def shouldBe(right: Any): Boolean
       |  }
       |
       |  trait Base {
       |    def i() = 1
       |  }
       |
       |  trait FooSpec extends Base {
       |    implicit def convertToAnyShouldWrapper(o: Any): AnyShouldWrapper
       |
       |    i shouldBe 1
       |  }
       |
       |  trait BarSpec extends Base {
       |    implicit def convertToAnyShouldWrapper(o: Any)(implicit prettifier: Prettifier): AnyShouldWrapper
       |
       |    i shouldBe 1
       |  }
       |}
    """.stripMargin,
    """|[1:8..1:15): example <= example/
       |[3:7..3:12): scala => scala/
       |[3:13..3:21): language => scala/language.
       |[3:22..3:41): implicitConversions => scala/language.implicitConversions.
       |[5:7..5:16): Issue2040 <= example/Issue2040.
       |[6:8..6:18): Prettifier <= example/Issue2040.Prettifier#
       |[7:9..7:19): Prettifier <= example/Issue2040.Prettifier.
       |[8:17..8:24): default <= example/Issue2040.Prettifier.default.
       |[8:26..8:36): Prettifier => example/Issue2040.Prettifier#
       |[8:39..8:42): ??? => scala/Predef.`???`().
       |[11:8..11:24): AnyShouldWrapper <= example/Issue2040.AnyShouldWrapper#
       |[12:8..12:16): shouldBe <= example/Issue2040.AnyShouldWrapper#shouldBe().
       |[12:17..12:22): right <= example/Issue2040.AnyShouldWrapper#shouldBe().(right)
       |[12:24..12:27): Any => scala/Any#
       |[12:30..12:37): Boolean => scala/Boolean#
       |[15:8..15:12): Base <= example/Issue2040.Base#
       |[16:8..16:9): i <= example/Issue2040.Base#i().
       |[19:8..19:15): FooSpec <= example/Issue2040.FooSpec#
       |[19:24..19:28): Base => example/Issue2040.Base#
       |[20:17..20:42): convertToAnyShouldWrapper <= example/Issue2040.FooSpec#convertToAnyShouldWrapper().
       |[20:43..20:44): o <= example/Issue2040.FooSpec#convertToAnyShouldWrapper().(o)
       |[20:46..20:49): Any => scala/Any#
       |[20:52..20:68): AnyShouldWrapper => example/Issue2040.AnyShouldWrapper#
       |[22:4..22:5): i => example/Issue2040.Base#i().
       |[22:6..22:14): shouldBe => example/Issue2040.AnyShouldWrapper#shouldBe().
       |[25:8..25:15): BarSpec <= example/Issue2040.BarSpec#
       |[25:24..25:28): Base => example/Issue2040.Base#
       |[26:17..26:42): convertToAnyShouldWrapper <= example/Issue2040.BarSpec#convertToAnyShouldWrapper().
       |[26:43..26:44): o <= example/Issue2040.BarSpec#convertToAnyShouldWrapper().(o)
       |[26:46..26:49): Any => scala/Any#
       |[26:60..26:70): prettifier <= example/Issue2040.BarSpec#convertToAnyShouldWrapper().(prettifier)
       |[26:72..26:82): Prettifier => example/Issue2040.Prettifier#
       |[26:85..26:101): AnyShouldWrapper => example/Issue2040.AnyShouldWrapper#
       |[28:4..28:5): i => example/Issue2040.Base#i().
       |[28:6..28:14): shouldBe => example/Issue2040.AnyShouldWrapper#shouldBe().
       |""".stripMargin
  )
  // Checks def macros that we can't test in expect tests because expect tests have no dependencies.
  occurrences(
    """|package e
       |import scala.meta._
       |import munit._
       |object x extends FunSuite {
       |  val x = q"Foo"
       |  val y = q"Bar"
       |  val z = q"$x + $y"
       |  val k = sourcecode.Name.generate
       |  assert(x.value == "Foo")
       |}
    """.stripMargin,
    """|[0:8..0:9): e <= e/
       |[1:7..1:12): scala => scala/
       |[1:13..1:17): meta => scala/meta/
       |[2:7..2:12): munit => munit/
       |[3:7..3:8): x <= e/x.
       |[3:17..3:25): FunSuite => munit/FunSuite#
       |[3:26..3:26):  => munit/FunSuite#`<init>`().
       |[4:6..4:7): x <= e/x.x.
       |[4:10..4:11): q => scala/meta/internal/quasiquotes/Unlift.
       |[5:6..5:7): y <= e/x.y.
       |[5:10..5:11): q => scala/meta/internal/quasiquotes/Unlift.
       |[6:6..6:7): z <= e/x.z.
       |[6:10..6:11): q => scala/meta/internal/quasiquotes/Unlift.
       |[6:13..6:14): x => e/x.x.
       |[6:18..6:19): y => e/x.y.
       |[7:6..7:7): k <= e/x.k.
       |[7:10..7:20): sourcecode => sourcecode/
       |[7:21..7:25): Name => sourcecode/Name.
       |[7:26..7:34): generate => sourcecode/NameMacros#generate().
       |[8:2..8:8): assert => munit/Assertions#assert().
       |[8:9..8:10): x => e/x.x.
       |[8:11..8:16): value => scala/meta/Term.Name#value().
       |[8:17..8:19): == => java/lang/Object#`==`().
       |""".stripMargin
  )

  targeted(
    """package f
      |object an {
      |  for {
      |    i <- List(1, 2)
      |    <<j>> <- List(3, 4)
      |  } yield j
      |}
      """.stripMargin, { (db, j) =>
      val denot = db.symbols.find(_.symbol == j).get
      assert(denot.symbol.startsWith("local"))
    }
  )

  targeted(
    """package g
      |object ao {
      |  object <<foo>>
      |  def <<foo>>(a: Int): Unit = ()
      |  def <<foo>>(a: String): Unit = ()
      |}
    """.stripMargin,
    (doc, foo1, foo2, foo3) => {
      assert(foo1 == "g/ao.foo.")
      assert(foo2 == "g/ao.foo().")
      assert(foo3 == "g/ao.foo(+1).")
    }
  )

  targeted(
    """package h
      |object ao {
      |  val local = 1
      |  def foo(a: Int = 1, b: Int = 2, c: Int = 3): Int = a + b + c
      |  def baseCase = <<foo>>(<<local>>, <<c>> = 3)
      |}
    """.stripMargin,
    (doc, foo1, local, c) => {
      assert(foo1 == "h/ao.foo().")
      assert(local == "h/ao.local.")
      assert(c == "h/ao.foo().(c)")
    }
  )

  targeted(
    """package i
      |object ao {
      |  val local = 1
      |  def foo(a: Int = 1, b: Int = 2, c: Int = 3): Int = a + b + c
      |  def recursive = <<foo>>(<<local>>, <<c>> = <<foo>>(<<local>>, <<c>> = 3))
      |}
    """.stripMargin,
    (doc, foo1, local1, c1, foo2, local2, c2) => {
      assert(foo1 == "i/ao.foo().")
      assert(foo2 == "i/ao.foo().")
      assert(local1 == "i/ao.local.")
      assert(local2 == "i/ao.local.")
      assert(c1 == "i/ao.foo().(c)")
      assert(c2 == "i/ao.foo().(c)")
    }
  )

  targeted(
    """package j
      |object ao {
      |  case class Msg(body: String, head: String = "default", tail: String)
      |  val bodyText = "body"
      |  val msg = <<Msg>>(<<bodyText>>, tail = "tail")
      |}
    """.stripMargin,
    (doc, msg, bodyText) => {
      assert(msg == "j/ao.Msg.")
      assert(bodyText == "j/ao.bodyText.")
    }
  )

  targeted(
    """package k
      |class target {
      |  def foo(a: Int, b: Int = 1, c: Int = 2): Int = ???
      |}
      |object consumer {
      |  def unstableQual = new target
      |  unstableQual.<<foo>>(1, <<c>> = 1)
      |}
    """.stripMargin,
    (doc, foo, c) => {
      assert(foo == "k/target#foo().")
      assert(c == "k/target#foo().(c)")
    }
  )
}

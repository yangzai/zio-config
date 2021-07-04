package zio.config.magnolia

import zio.config.magnolia._
import zio.config._, ConfigDescriptor._

final case class A(a: B)

final case class B(
  b: String,
  c: C,
  d: List[C],
  e: Option[C],
  f: Either[C, E],
  g: Option[E],
  h: Option[E],
  i: Option[P],
  j: Option[P]
)

final case class C()

@name("E")
enum E:
  case D
  case F
  @name("G")
  case G(value: String)

enum P:
  case Q
  case R
  case S(z: String)
  @name("t")
  case T(u: String)


object Example extends App :
  val source =
    ConfigSource.fromMap(
      Map(
        "a.b" -> "v1",
        "a.c" -> "C",
        "a.d" -> "C, C",
        "a.f.E.G.value" -> "v2",
        "a.g.E" -> "D",
        "a.h.E" -> "F",
        "a.h" -> "Q",
        "a.i" -> "Q",
        "a.j.T.u" -> "v3"
      ),
      keyDelimiter = Some('.'),
      valueDelimiter = Some(',')
    )

  val desc = descriptor[A]

  val res = read(desc from source)
  // Right(A(B(v1,C(),List(C(), C()),None,Right(G(v2)),Some(D),Some(F),None,None)))

  println(res)
  val map = write(desc, res.getOrElse(throw new Exception())).map(_.flattenKeyAndValue(pathDelimiter = ".", valueDelimiter = ", ")).getOrElse(throw new Exception)
   println(map)
  // Right(A(B(hi,C(),List(C(), C()),None,Right(F))))

end Example

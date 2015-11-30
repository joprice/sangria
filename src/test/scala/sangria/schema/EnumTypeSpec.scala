package sangria.schema

import org.scalatest.{Matchers, WordSpec}
import sangria.schema._
import sangria.util.{Pos, GraphQlSupport}
import spray.json.{JsObject, JsString, JsNumber}

class EnumTypeSpec extends WordSpec with Matchers with GraphQlSupport {
  val ColorType = EnumType("Color", values = List(
    EnumValue("RED", value = 0),
    EnumValue("GREEN", value = 1),
    EnumValue("BLUE", value = 2)
  ))

  object Args {
    val fromEnum = Argument("fromEnum", OptionInputType(ColorType))
    val fromInt = Argument("fromInt", OptionInputType(IntType))
  }

  val QueryType = ObjectType("Query", fields[Unit, Unit](
    Field("colorEnum", OptionType(ColorType),
      arguments = Args.fromEnum :: Args.fromInt :: Nil,
      resolve = ctx ⇒
        ctx.args.argOpt(Args.fromInt).orElse(ctx.args.argOpt(Args.fromEnum))),
    Field("colorInt", OptionType(IntType),
      arguments = Args.fromEnum :: Args.fromInt :: Nil,
      resolve = ctx ⇒
        ctx.args.argOpt(Args.fromInt).orElse(ctx.args.argOpt(Args.fromEnum)))
  ))

  val MutationType = ObjectType("Mutation", fields[Unit, Unit](
    Field("favoriteEnum", OptionType(ColorType),
      arguments = Args.fromEnum :: Nil,
      resolve = _.args.argOpt(Args.fromEnum))
  ))

  val SubscriptionType = ObjectType("Subscription", fields[Unit, Unit](
    Field("subscribeToEnum", OptionType(ColorType),
      arguments = Args.fromEnum :: Nil,
      resolve = _.args.argOpt(Args.fromEnum))
  ))

  val schema = Schema(QueryType, Some(MutationType), Some(SubscriptionType))

  "Type System: Enum Values" should {
    "accepts enum literals as input" in check(
      (),
      "{ colorInt(fromEnum: GREEN) }",
      Map("data" → Map("colorInt" → 1)))

    "enum may be output type" in check(
      (),
      "{ colorEnum(fromInt: 1) }",
      Map("data" → Map("colorEnum" → "GREEN")))

    "enum may be both input and output type" in check(
      (),
      "{ colorEnum(fromEnum: GREEN) }",
      Map("data" → Map("colorEnum" → "GREEN")))

    "does not accept string literals" in checkContainsErrors(
      (),
      """{ colorEnum(fromEnum: "GREEN") }""",
      Map("colorEnum" → null),
      List("Field 'fromEnum' has wrong value: Enum value expected." → Some(Pos(1, 23))),
      validateQuery = false)

    "does not accept internal value in place of enum literal" in checkContainsErrors(
      (),
      """{ colorEnum(fromEnum: 1) }""",
      Map("colorEnum" → null),
      List("Field 'fromEnum' has wrong value: Enum value expected." → Some(Pos(1, 23))),
      validateQuery = false)

    "accepts JSON string as enum variable" in check(
      (),
      """query test($color: Color!) { colorEnum(fromEnum: $color) }""",
      Map("data" → Map("colorEnum" → "BLUE")),
      JsObject("color" → JsString("BLUE")))

    "accepts enum literals as input arguments to mutations" in check(
      (),
      """mutation x($color: Color!) { favoriteEnum(fromEnum: $color) }""",
      Map("data" → Map("favoriteEnum" → "GREEN")),
      JsObject("color" → JsString("GREEN")))

    "accepts enum literals as input arguments to subscriptions" in check(
      (),
      """subscription x($color: Color!) { subscribeToEnum(fromEnum: $color) }""",
      Map("data" → Map("subscribeToEnum" → "GREEN")),
      JsObject("color" → JsString("GREEN")))

    "accepts internal value as enum variable as long as type matches" in check(
      (),
      """query test($color: Color!) { colorEnum(fromEnum: $color) }""",
      Map("data" → Map("colorEnum" → "GREEN")),
      JsObject("color" → JsNumber(1)))

    "does not accept string variables as enum input" in checkContainsErrors(
      (),
      """query test($color: String!) { colorEnum(fromEnum: $color) }""",
      null,
      List("Variable '$color' of type 'String!' used in position expecting type 'Color'." → Some(Pos(1, 51))),
      JsObject("color" → JsString("BLUE")),
      validateQuery = true)

    "does not accept internal value variable as enum input" in checkContainsErrors(
      (),
      """query test($color: Int!) { colorEnum(fromEnum: $color) }""",
      null,
      List("Variable '$color' of type 'Int!' used in position expecting type 'Color'." → Some(Pos(1, 48))),
      JsObject("color" → JsNumber(2)),
      validateQuery = true)

    "enum value may have an internal value of 0" in check(
      (),
      """
       {
         colorEnum(fromEnum: RED)
         colorInt(fromEnum: RED)
       }
      """,
      Map("data" → Map(
        "colorEnum" → "RED",
        "colorInt" → 0)))

    "enum inputs may be nullable" in check(
      (),
      """
       {
         colorEnum
         colorInt
       }
      """,
      Map("data" → Map(
        "colorEnum" → null,
        "colorInt" → null)))
  }
}
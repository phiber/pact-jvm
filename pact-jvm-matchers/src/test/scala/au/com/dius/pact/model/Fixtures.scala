package au.com.dius.pact.model

import org.json4s.JsonAST.{JBool, JObject}
import org.json4s.jackson.JsonMethods.pretty

object Fixtures {
  import HttpMethod._
  import org.json4s.JsonDSL._

  val provider = Provider("test_provider")
  val consumer = Consumer("test_consumer")


  val request = Request(Get, "/", "q=p&q=p2&r=s", Map("testreqheader" -> "testreqheadervalue"),
    pretty(JObject("test" -> JBool(true))), null)

  val response = Response(200,
    Map("testreqheader" -> "testreqheaderval"),
    pretty(JObject("responsetest" -> JBool(true))), null)

  val interaction = Interaction(
    description = "test interaction",
    providerState = Some("test state"),
    request = request,
    response = response
  )

  val interactions = List(interaction)

  val pact: Pact = Pact(
    provider = provider,
    consumer = consumer,
    interactions = interactions
  )
}

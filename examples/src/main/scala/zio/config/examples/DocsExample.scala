package zio.config.examples

import zio.config._, magnolia._
import ConfigDescriptor._
import java.io.PrintWriter
import java.io.File
import zio.config.typesafe.TypesafeConfigSource
import zio.config.examples.typesafe.EitherImpureOps

object DocsExample extends App with EitherImpureOps {

  sealed trait CredentialsProvider

  case object Default extends CredentialsProvider

  case class Credentials(token: String, secret: String) extends CredentialsProvider

  final case class Config(provider: CredentialsProvider)

  val printwriter = new PrintWriter(new File("something.md"))

  val typesafeConfigSource = TypesafeConfigSource
    .fromHoconString(
      s"""
     {
       "provider" : {
         "Credentials" : {

         }
       }
     }
    
    
    """
    )
    .loadOrThrow

  printwriter.write(
    generateDocs(descriptor[Config] from typesafeConfigSource).toTable.toGithubFlavouredMarkdown
  )

  printwriter.close()

  println(read(descriptor[Config] from typesafeConfigSource))

  final case class Database(port: Int, url: Option[String])

  val config =
    nested("database") {
      (int("PORT") ?? "Example: 8088" |@|
        string("URL").optional ?? "Example: abc.com")(
        Database.apply,
        Database.unapply
      ) ?? "Database related"
    }

  val docs =
    generateDocs(config)

  val markdown =
    docs.toTable.toGithubFlavouredMarkdown

  assert(
    markdown ==
      s"""
         |## Configuration Details
         |
         |
         ||FieldName           |Format            |Description     |Sources|
         ||---                 |---               |---             |---    |
         ||[database](database)|[all-of](database)|Database related|       |
         |
         |### database
         |
         ||FieldName|Format   |Description                                           |Sources|
         ||---      |---      |---                                                   |---    |
         ||PORT     |primitive|value of type int, Example: 8088                      |       |
         ||URL      |primitive|value of type string, optional value, Example: abc.com|       |
         |""".stripMargin
  )

  val confluenceMarkdown =
    docs.toTable.toConfluenceMarkdown(None)

  assert(
    confluenceMarkdown ==
      s"""
         |## Configuration Details
         |
         |
         ||FieldName          |Format           |Description     |Sources|
         ||---                |---              |---             |---    |
         ||[database|database]|[all-of|database]|Database related|       |
         |
         |### database
         |
         ||FieldName|Format   |Description                                           |Sources|
         ||---      |---      |---                                                   |---    |
         ||PORT     |primitive|value of type int, Example: 8088                      |       |
         ||URL      |primitive|value of type string, optional value, Example: abc.com|       |
         |""".stripMargin
  )
}

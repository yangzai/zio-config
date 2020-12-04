
## Make your config easy..

---

## I am

Afsal, Sydney
Working at Simple Machines


---
## Isn't config already easy?

```scala

Properties defaultProps = new Properties();

FileInputStream in = new FileInputStream("defaultProperties");

defaultProps.load(in);

in.close();

```


^ Configuration Management can be as easy as grabbing a set of key-value pairs as string
from any source. But it depends on what exactly you are doing with these config.
From the perspecitve of developing a configuration driven application, something that you
probably expose to the users that they fill in these configs, it gets complicated when you 
try and answer a few important questions.

---

## Here are the questions ?

* Is your config typesafe? 
* How do you accumulate the errors?
* How do you document your config?
* How do you set an example of your config for the end user?
* How do you manage multiple sources in your config?

We will cover these and a lot more..

^ Goes back to the question of how logically consistent your application is.
 Config retrieval is starting point of your application. It's much better to bring that
 consistency as early as possible. Isn't it muuch better if the types says the list is always of
 the size greater than 2 than checking it at every point, or forgetitng to check it at every point
 leading to bugs?
 when you try and form a typesafe config from some information lying outside, there are chances
 that it can fail. Do you fail early or do you give a complete report of what needs to be done?
 How do you actually document this config? Do we do that manually? Probably for the most time yes.
 And that documentation can quickly become old in a few days of time. Do you expect the user to read this big doc (automated or manual) to actually write a config. The answer
 should be No.

---

## ZIO Config & Example usage

```scala

// The config
final case class MyConfig(host: String, port: Int)

```

^ As most of you would have thought, we represent config as products & coproducts - scala case classes and sealed traits.


---

## Example usage

```scala

// Given a source

val source: ConfigSource = 
  ConfigSource.fromMap(
    Map(
      "host" -> "aurora.db", 
      "port" -> "8080"
    )
  )


```

---
## Example usage: Automatic

```scala

val config: ConfigDescriptor[MyConfig] = 
  descriptor[MyConfig] from source

read(config) 

// Right(MyConfig("aurora.db",8080))  


```

---
## Example usage : Manual

```scala

// Manual
val config: ConfigDescriptor[MyConfig] = 
  (string("host") |@| int("port"))(MyConfig.apply, MyConfig.unapply) 


read(config from source) 

// Right(MyConfig("aurora.db",8080))  


```

---
## Where is typesafety ?

```scala

// Bring config into static world!
sealed trait CredentialsProvider

// We define the terms in ADT in a companion object
object CredentialsProvier {
  case object Default extends CredentialsProvider
  case class Credentials(token: String, secret: String) extends CredentialsProvider
}

```

---

## Where is typesafety ?

```scala

final case class MyConfig(provider: CredentialsProvider)

val config: ConfigDescriptor[MyConfig] = 
 descriptor[MyConfig]

val source =
  ConfigSource.fromMap(
    "provider.Credentials" -> "invalid", 
     keyDelimiter = Some('.')
  )

read(config from source)

// yields pretty printed error message

```

---

## And if it fails, errors are accumulated
```scala
        ╥
        ╠══╦══╗
        ║  ║  ║
        ║  ║  ╠─MissingValue
        ║  ║  ║ path: provider.Credentials.secret
        ║  ║  ║ Details: value of type string
        ║  ║  ▼
        ║  ║
        ║  ╠─MissingValue
        ║  ║ path: provider.Credentials.token
        ║  ║ Details: value of type string
        ║  ▼
        ║
        ╠─FormatError
        ║ cause: Provided value is of type Record, expecting the type Leaf
        ║ path: provider
        ║ Details: constant string 'Default'
        ▼

```

---

## Managing multiple sources


```scala

val sysEnv:      ConfigSource = fromSystemEnv
val commandLine: ConfigSource = fromCommandLineArgs 

// <> is orElse
val mySource = sysEnv <> commandLine

read(config from mySource)


```

---

## Datasource overrides

```scala

// Tag sources per field
val x = string("username") from systemEnv
val y = string("password") from credentialSource

val config: ConfigDescriptor[MyConfig] = 
 (x |@| y)(MyConfig.apply, MyConfig.unapply)

val testSource = 
 ConfigSource.fromMap(Map("username" -> "x", "password" -> "y"))
 
// Override all sources with a constant mapSource
config.updateSource(source => testSource <> source)

```
---

## Rich documentation

```scala 
val config: ConfigDescriptor[MyConfig] = 
  descriptor[MyConfig] from source

generateDocs(config)
  .toTable.toGithubFlavouredMarkdown

// Or
generateDocs(config)
  .toTable.toConfluenceFlavouredMarkdown

```


---

![](markdown.png)


---

## Generate a Config for user

```scala

 sealed trait AwsRegion

 object AwsRegion {
   @name("ap-southeast")
   case object ApSouthEast2 extends AwsRegion

   @name("us-east")
   case object UsEast extends AwsRegion
 }

 final case class Endpoint(port: Int, host: java.net.URL)
 final case class MyConfig(region: Region, endpoint: Endpoint)

```

---
## Generate a Config for user

```scala

generateConfigJson(descriptor[MyConfig], 2).unsafeRunChunk

// yields 

Chunk({
    "endpoint" : {
        "host" : "http://def",
        "port" : "7300"
    },
    "region" : "ap-southeast"
  }
, {
    "endpoint" : {
        "host" : "http://abc",
        "port" : "8908"
    },
    "region" : "us-east"
  }
)

```

---
## That's coz it can write back!

```scala

  val description: ConfigDescriptor[MyConfig] = 
    descriptor[MyConfig]

  val myConfig: MyConfig = 
    MyConfig(UsEast, Endpoint(8908, http://abc))

  // Pass program to toMap action
  // write(description, myConfig).map(tree => ..)
  myConfig.toMap(description)

   Map(
    "region" -> "us-east", 
    "endpoint.port" -> "8908",
    "endpoint.host" -> "http://abc"
   )

```

---
## It can write back!

```scala

  // Pass program toHoconString action
  myConfig.toHoconString(description)

  { 
    region : us-east, 
    endpoint : { 
      port : 8908, 
      host : http://abc 
    }
  }

```
---

## Work with Refined types


```scala

 // No more manual validations

 type NonEmptyString  = Refined[String, NonEmpty]
 type GreaterThan1024 = Refined[Int, Greater[W.`1024`.T]]

 final case class RefinedConfig(
   username: NonEmptyString,
   id: GreaterThan1024
 )

// More you push towards static world, more reliable your program becomes!
 
```



---

## Work with Refined types

```scala

  val invalidSource =
    ConfigSource.fromMap(
      Map("id" -> "10", "username" -> "")
    )

  read(
    descriptor[RefinedConfig] from invalidSource
  )
 

```


---
## Work with Refined types

```scala 

// fail with

  Left(ReadError:
  ╥
  ╠══╦══╗
  ║  ║  ║
  ║  ║  ╠─ConversionError
  ║  ║  ║ cause: Predicate isEmpty() did not fail.
  ║  ║  ║ path: username
  ║  ║  ▼
  ║  ║
  ║  ╠─ConversionError
  ║  ║ cause: Predicate (10 < 1024) did not fail.
  ║  ║ path: id
  ║  ▼
  ▼)


```

---

### Let's dive in a bit more..

---


## Ok? What are the sources supported?

```scala


val mapSource = ConfigSource.fromMap(..)

val propertySource = ConfigSource.fromPropertyFile(..)

val commandLine = ConfigSource.fromCommandLineArgs(..)

val systemEnv = ConfigSource.fromSystemEnv(..)

val hoconSource = TypesafeConfigSoure.fromHoconFile(..)

val yamlSource = YamlConfigSource.fromYamlFile(...)


// and so on, and easy enough to add more


```

---

## Attach sources to descriptor

```scala
val hoconSource =
  TypesafeConfigSource.fromHoconFile(..)

val config =
  descriptor[MyConfig] from hoconSource
 
```
---

## Update sources of descriptor

```scala
  val constantSource = ConfigSource.fromMap(...)

  val udpatedConfig = 
    config.updateSource(_ <> constantSource)

```

---

## Strip off a source from descriptor

```scala

val unsourcedConfig = 
  config.unsourced

// And may be attach a constant map in your tests.
val updatedConfig =
  config.unsourced.from(constantSource)

```

---

## Manual descriptor 

```scala
  // Example: New type
  final case class Port(n: Int)

  val portCfg = int("port").transformOrFail(
    s => if (s < 1000) Left("port should be greater than 1000") else Right(Port(s)),
    port => Right(port.n)
  )

```

---

## Manual descriptor

```scala

  // int here is ConfigDescriptor[Int]
  val config: ConfigDescriptor[List[Int]] = 
    list("ports")(int)
 
  // How to read?
  
  // if string("username")   implies there exists key "username" and value is of type String
  // then list("ports")(int) implies there exists key "ports" and value is of type List[Int]

```

---

## Manual, Yet Refined

```scala

  // returns ConfigDescriptor[Refined[Int, NonEmpty]]
  refined[String, NonEmpty]("DB_URL").optional

  // which can return ConfigDescriptor[Refined[List[Int], Size[Greater[W.2.T]]
  refined[Size[Greater[W.2.T]](
    list("PORT")(int)
  )

```

---

## Interestingly

There needn't be keys too. We will see why

```scala

  val strConfig: ConfigDescriptor[Int] = 
    int("port") 

  val listConfig: ConfigDescriptor[List[Int]] =
    list("ports")(int)

```

---

## Interestingly

There needn't be keys too. We will see why

```scala

  val strConfig: ConfigDescriptor[Int] = 
    int 

  val listConfig: ConfigDescriptor[List[Int]] =
    list(int)

```

---


## Let's see that through Either

```scala
 
 val config = nested("version")(double orElseEither string)

 // Same as: double("version") orElseEither string("version")

```


---

## An example of Either

```scala

 val source = fromHoconString(
   {
     "version" : "1.1" 
   } 
  )

 read(config from source)

 // Left(1.1)

```



---

## An example of Either

```scala

 val source = fromHoconString(
   {
     "version" : "latest" 
   } 
  )

 read(config from source)

 // Right("latest")

```

---
## Leak of datastructures in Config

```scala
  
sealed trait VersionStrategy

object VersionStrategy {
  case object Latest        extends VersionStrategy
  case class Number(n: Int) extends VersionStrategy
}

final case class VersionInfo(name: String, strategy: VersionStrategy)

 
```
---

## Leak of datastructure in Config

```scala

  final case class Database(host: java.net.URL, port: Int)
  
  final case class MyConfig(
    database: Database, versionInfo: VersionInfo, inputDir: String
  )

 

```

---

## Is fully automatic the way to go?

```scala
 // A fully automatic VersionInfo would 
 // require a config (if json) to be like this. Hmmm ?
 
    {                                 {
      "versionInfo" : {                 "versionInfo" : {
         "name" : "version",    OR        "name" : "version",
         "strategy" : {                   "strategy" : "latest"
            "Number" : {                 }   
               "n" : 1                 }       
            }                               
         }                                 
      }                                                                   
    }                                                                         
                                           
 
```

---

## While that can be simplified for the user


```scala
  // Far simpler for user
 
  "versionInfo" : {          
    "version" : "1"         
   }                           

   // or

  "versionInfo" : {
    "version" : "latest"
   }

```

---

## Let's write a manual config

```scala

  val versionConfig = 
    map(int.orElseEither(string)).transformOrfail(
      _.headOption match {
          case Some((k, v)) => 
            v match {
              case Right(string) if string == "latest" => Right(VersionInfo(k, Latest))
              case Left(n) => Right(VersionInfo(k, Number(n)))
              case Right(v) => Left(..) 
            }
          case None => Left(..)
      },  
      i => i.strategy match {
         case Latest => Map(i.name -> Right("latest"))
         case Number(n) => Map(i.name -> Left(n))
      }
    )

```

---

## Let's use automatic for the rest

```scala
  implicit val versionInfo: Descriptor[VersionInfo] = 
    Descriptor(versionConfig)
  
  val config = descriptor[MyConfig].mapKey(toKebabCase)    
```

---

## May be change the keys to kebab case 

```scala

  val config = descriptor[MyConfig].mapKey(toKebabCase)

  generateConfigJson(config, 1).unsafeRunChunk

 // yields 

    {
       "database" : {
           "host" : "http://abc",
           "port" : "5502"
       },
       "input-dir" : "5Vf0GTG",
       "version-info" : {
           "QK8mNc5eciBlH" : "latest"
       }
    } 

```

---

## And a lot more

 **_~30 Examples_** in https://github.com/zio/zio-config/tree/gen/examples
 
 **_Website_** at https://zio.github.io/zio-config/
 
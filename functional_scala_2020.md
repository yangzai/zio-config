
## Make your config easy..

---

## Simple Configs are easy isn't it?

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
* How do you set an example of your config for users?

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
## and more..

* How do you manage multiple sources in your config?
* How do you prioritise these sources?
* How do you test the config logic when the source is a heavy weight?
* In case you want to write the config back ?..

We will cover these and a lot more..

---

## Let's see some example code using ZIO-Config

```scala

// The config
final case class MyConfig(host: String, port: Int)

```

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
  description[MyConfig] from source

read(config) 

// Right("aurora.db",8080)  


```

---
## Example usage : Manual

```scala

// Manual
val config: ConfigDescriptor[MyConfig] = 
  (string("host") |@| int("port"))(MyConfig.apply, MyConfig.unapply)


read(config) 

// Right("aurora.db",8080)  


```

---
## Where is typesafety ?

```scala

// Bring config into static world!

sealed trait CredentialsProvider

case object Default extends CredentialsProvider

case class Credentials(token: String, secret: String) 
  extends CredentialsProvider

```

---

## Where is typesafety ?

```scala

// your config
case class MyConfig(provider: CredentialsProvider)

val config: ConfigDescriptor[MyConfig] = 
  descriptor[Config] from source

read(config)
// MyConfig(Credentials(..))

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
val sysEnv: ConfigSource = ???

val commandLine: ConfigSource = ??? 

// orElse
val mySource = sysEnv <> commandLine

val result = read(config from mySource)


```

---

## Managing multiple sources

Attach ConfigSource to any part of your program

```scala

val x = string("username") from systemEnv
val y = string("password") from credentialSource

(x |@| y)(MyConfig.apply, MyConfig.unapply)


```
---

## Rich documentation


```scala 


val config = descriptor[MyConfig] from source

generateDocs(config)
  .toTable.toGithubFlavouredMarkdown

```


---

![](markdown.png)


---

## Generate a Config for user

```scala

 sealed trait Region

 @name("ap-southeast")
 case object ApSouthEast2 extends Region

 @name("usEast")
 case object UsEast extends Region

 final case class Database(port: Int, host: java.net.URL)
 final case class MyConfig(region: Region, database: Database)


```

---
## Generate a Config for user

```scala

generateConfigJson(descriptor[MyConfig], 2).unsafeRunChunk

// yields 

Chunk({
    "database" : {
        "host" : "http://def",
        "port" : "7300"
    },
    "region" : "ap-southeast"
  }
, {
    "database" : {
        "host" : "http://abc",
        "port" : "8908"
    },
    "region" : "usEast"
  }
)

```

---
## That's coz it can write back!

```scala

  val config: ConfigDescriptor[MyConfig] = 
    descriptor[MyConfig]

  val myConfig: MyConfig = 
    MyConfig(UsEast, Database(8908, http://abc))

  myConfig.toJson(config)
  myConfig.toMap(config)

```

---

## Work with Refined types


```scala

 // No more manual validations

 case class RefinedConfig(
   port: Refined[Int, GreaterEqual[W.`1024`.T]],
   dbUrl: Option[Refined[String, NonEmpty]]
 )

 More it is in static, the more it is reliable..

```

---

## Work with Refined types

```scala

  val invalidSource =
    ConfigSource.fromMap(
      Map("port" -> "10", "dbUrl" -> "")
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
  ║  ║  ║ path: dbUrl
  ║  ║  ▼
  ║  ║
  ║  ╠─ConversionError
  ║  ║ cause: Predicate (10 < 1024) did not fail.
  ║  ║ path: port
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
## Priortised config source


```scala    
  
  // <> is orElse
  val source = 
   commandLine <> systemEnv <> propertySource

```
---

## Attach sources to descriptor

```scala
val typesafeSource =
  TypesafeConfigSource.fromHoconFile(..)

val config =
  descriptor[MyConfig] from typesafeSource
 
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

 final case class Host(url: String)

 final case class MyConfig(port: Port, host: String)

 val hostCfg = string("host")

 val config = (portCfg |@| string("host"))(MyConfig.apply, MyConfig.unapply)

```

---

## Data types

```scala
 val config: ConfigDescriptor[String] = 
   string("username")

 // says, key username has a value of the type string

```

---

## Data types

```scala

 
 val config: ConfigDescriptor[List[Int]] = 
  list("ports")(int)
 //  says key usernames has a value of the type of list of string

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

### An example of a list

```scala

 case class Data(bucket: String, prefix: String)
 case class Config(tables: List[Data])
  
 val config = 
  list("tables")(descriptor[Data])(Config.apply, Config.unapply)

 // Same as: val config = descriptor[Config]

```

---

## An example of Either

```scala
 
 val config = nested("version")(double orElseEither string)

 // Same as: int("version") orElseEither string("version")

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

  // Think of how this would look in actual source
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

## Was that as simple as this ?


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
    map(int.orElseEither(string).transformOrfail(
      _.headOption match {
          case Some((k, v)) => 
            versionValue match {
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
  
  val config = descriptor[MyConfig]    
```

---

## How do you communicate this to the user

```scala

  val config = descriptor[MyConfig]

  generateConfigJson(config, 1).unsafeRunChunk

 // yields 

    {
       "database" : {
           "host" : "http://abc",
           "port" : "5502"
       },
       "inputDir" : "5Vf0GTG",
       "versionInfo" : {
           "QK8mNc5eciBlH" : "latest"
       }
    } 

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

 https://github.com/zio/zio-config/tree/gen/examples
 
 https://zio.github.io/zio-config/
 
 https://javadoc.io/doc/dev.zio/zio-config_2.12/latest/index.html
 
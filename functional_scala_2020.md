
## Make your config easy..

---
## Why is it challenging ?

Managing application configuration can sound easy, yet ends up being challenging.

---


## Simple Configs are easy isn't it?

```scala

Properties defaultProps = new Properties();

FileInputStream in = new FileInputStream("defaultProperties");

defaultProps.load(in);

in.close();

```

---

## Here are the questions ?

* Is your config typesafe? (More of, is your program typsafe?)
* How do you accumulate the errors?
* How do you document your config?
* How do you set an example of your config for users?


---
## Continuing these questions

* How do you represent multiple sources in your config?
* How do you prioritise sources?
* How do you test these config without heavy weight sources?

---
## And a few advanced questions?

* How do you validate your config parameters? 
* And how to statically represent the validated config?
* How do you write the config back to the source if needed?

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

## Composable ConfigSource

**_ConfigSource_** is composable within itself.

This means

```scala
val sysEnv: ConfigSource = ???

val commandLine: ConfigSource = ??? 

val mySource = sysEnv orElse commandLine

val result = read(config from mySource)

// Tries systemEnv first, and if it fails tries CommandLine

```

---

## Flexible ConfigSource

Attach ConfigSource to any part of your program

```scala

val x = string("username") from systemEnv
val y = string("password") from credentialSource


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

## Generate a Config for user to begin with

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
## Generate a Config for user to begin with

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
## Because it can write back!

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

## Let's lens through..

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

```
  // Example: New type
  final case class Port(n: Int)

  val portCfg = int("port").transformOrFail(
    s => if (s < 1000) Left("port should be greater than 1000") else Right(Port(s)),
    port => Right(port.n)
  )

```

---

## Manual descriptor

```

 final case class Host(url: String)

 final case class MyConfig(port: Port, host: String)

 val hostCfg = string("host")

 val config = (portCfg |@| string("host"))(MyConfig.apply, MyConfig.unapply)

```


---

[Stop]

---

## Custom Config Source


```scala

 // Example
 def credentialSource(client: SystemManager): Task[ConfigSource] =
   client.getParameters("/path").flatMap(kv => ConfigSource.fromMap(kv)()

 credentialStore(client).flatMap(creds =>
  (string("username") from sysEnv |@|
    string("password") from creds)(MyConfig.apply, MyConfig.unapply)
 )

```

---

## What happens when there is a failure?

provider is **_Credentials_** but forgot **_token_** and **_password_**. 

```scala
  val source = fromHoconString(
     {
       "provider" : {
         "Credentials" : {

         }
       }
     }
    )

```

**Note** : source is actually **_Either[ReadError[String], ConfigSource]_**

---
## Hierarchy of Errors - A Tree!

```python

val config = read(config from typesafeConfigSource)

```

will fail with 

```scala

Left(ReadError[String](...))

```

---
## Saving to any data source 

**Given a `ConfigDescriptor` we can write it back**

```scala

case class MyConfig(db: String, port: Int)

val config: ConfigDescritpor[MyConfig] = 
  descriptor[MyConfig]

```
---

# [fit] Focus

---










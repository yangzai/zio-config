
## Make your application config easy

---
## Why is it challenging ?

Managing application configuration can be quite challenging.

We will see why.
We will see a solution.
We will see an ecosystem of libraries in Scala.

---

## Simple Config is easy isn't it?

```scala

Properties defaultProps = new Properties();

FileInputStream in = new FileInputStream("defaultProperties");

defaultProps.load(in);

in.close();

```

---

## Here are the questions ?

* How do you use these configs? Are they type safe ?
* What if I need to extract the same config from a different source?
* How do you ensure you are accumulating the errors?


---
## Continuing these questions

* How do you differentiate between missing values and format errors?
* How do you **_write_** this config back to a source? Example: Your config is a state file.
* How do you **_document_** these config?

---
## And a few advanced questions?

* How do you compose configuration parameters? 
* While there are automatic derivations (spring boot in Java) how do you combine this behaviour with a composable versions?

---

## Let's see some example code using ZIO-Config

The best way to describe your config is a case class.

```scala

final case class MyConfig(username: String, password: String)

val source = ConfigSource.fromMap(..)

val config = description[MyConfig]

val result: Either[ReadError[K], MyConfig] = 
  read(config from source) 


```

---
## Where is typesafety ?

Define your data type

```scala

sealed trait CredentialsProvider

case object Default extends CredentialsProvider

case class Credentials(token: String, secret: String) 
  extends CredentialsProvider

```

---

Read config to your data type

```scala

case class Config(provider: CredentialsProvider)

val config = descriptor[Config]

read(config from source)


```

---

## Program is independent of actions.

`config` above is independent of an action

```scala

import zio.config._, ConfigDescriptor._, magnolia._

val config = description[MyConfig]

read(config from source)

//Right(MyConfig(user, pass))


```

---
## We have more such actions


We will see more actions similar to **_read_**, such as **_write_**, **_generateDocs_** etc.



---

### Independent Config and ConfigSource

config is completely independent of `ConfigSource`.

Implies, we can attach config to any ConfigSource

---

##

```scala

val configSource: Either[ReadError[String], ConfigSource] = 
  TypesafeConfigSource.fromHoconFile(new File("path-to-file"))

 configSource.flatMap(source => read(config from source)
 // Right(MyConfig(name, pass)

```

---

## We have more such sources

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
## Data source overrides

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

## Data source overrides

Or you can update an existing config that is already tagged to a ConfigSource

```scala
val typesafeSource  = TypesafeConfigSource.fromHoconFile(..)
val configFromHocon = descriptor[MyConfig] from typesafeSource
val constantSource   = ConfigSource.fromMap(...)

val udpatedConfig = configFromHocon.updateSource(
  existingSource => existingSource.orElse(constantSource)
)


```

---

## Strip off a source from Config

```scala

val unsourcedConfig = configFromHocon.unsourced

// And may be attach a constant map in your tests.
val updatedConfig = configFromHocon.unsourced.from(constantSource)


```

---
## Tag source to each field

```scala
val x = string("username") from systemEnv
val y = string("password") from credentialSource

val detailedConfig = (x |@| y)(MyConfig.apply, MyConfig.unapply) 

// and then read(detailedConfig)
// Manual derivation - we will cover this later.
```

----

## Override with a global source

```scala
val hoconSource = TypesafeConfigSource.fromFile(..)

val finalConfig = detailedConfig from hoconSource

// and then read(finalConfig)

// Priority is the sources attached invididually, and then the global source

```


---

## Custom Source

In the last example, credentialSource is a custom **_ConfigSource_** .

key-values (map) are retrieved from a credential store and then form a **_ConfigSource_** from this map.


---

## Rich documentation


```scala 


val config = descriptor[MyConfig] from constantMap

generateDocs(config)
  .toTable.toGithubFlavouredMarkdown

```

---

## Result of documentation

```scala
## Configuration Details
|FieldName           |Format                |Description|Sources|
|---                 |---                   |---        |---    |
|[provider](provider)|[any-one-of](provider)|           |       |

### provider
|FieldName                 |Format               |Description              |Sources |
|---                       |---                  |---                      |---     |
|[Credentials](credentials)|[all-of](credentials)|                         |        |
|                          |primitive            |constant string 'Default'|constant|

### Credentials
|FieldName|Format   |Description         |Sources |
|---      |---      |---                 |---     |
|token    |primitive|value of type string|constant|
|secret   |primitive|value of type string|constant|
```
---

![](markdown.png)

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
## And that's prettyPrinted!

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
## Saving to any data source 

**Given a `ConfigDescriptor` we can write it back**

```scala

case class MyConfig(db: String, port: Int)

val config: ConfigDescritpor[MyConfig] = 
  descriptor[MyConfig]

```


---
## Saving to any data source

```scala
val value: MyConfig = MyConfig("xyz.com", 8080)

value.toJson(config)

// returns: { "db" : "xyz.com", "port" : "8080" }

value.toMap(config)

// returns: Map("db" -> "xyz.com", "port" -> "8080")
```


---
# [fit] Deckset 
# [fit] has something
# [fit] for **_you_**…


---

# [fit] Here is 
# [fit] The lowdown

---

# [fit] **_1_**

---

# [fit] Add _**[fit]**_ 

---

# [fit] To the start
# [fit] of any headline

---

# [fit] _**(After the hash & before the headline)**_
# [fit] _**Like so:**_ `# [fit] Your awesome headline`

---

# [fit] **_2_**

---

# [fit] Only 
# [fit] use Headlines
# [fit] _**Start it with a # — no Paragraphs or lists within your slide**_

---

# [fit] **_3_**

---

# [fit] Go forth and create:

---

# [fit] Impact

---

# [fit] &

---

# [fit] Focus

---

# [fit] :heart:









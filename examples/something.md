
## Configuration Details


|FieldName           |Format                |Description|Sources|
|---                 |---                   |---        |---    |
|[provider](provider)|[any-one-of](provider)|           |       |

### provider

|FieldName                 |Format               |Description              |Sources|
|---                       |---                  |---                      |---    |
|[Credentials](credentials)|[all-of](credentials)|                         |       |
|                          |primitive            |constant string 'Default'|hocon  |

### Credentials

|FieldName|Format   |Description         |Sources|
|---      |---      |---                 |---    |
|token    |primitive|value of type string|hocon  |
|secret   |primitive|value of type string|hocon  |

[![CircleCI](https://circleci.com/gh/suusan2go/kotlin-fill-class.svg?style=svg)](https://circleci.com/gh/suusan2go/kotlin-fill-class)
[![jetBrains](https://img.shields.io/jetbrains/plugin/d/10942-kotlin-fill-class.svg)](https://plugins.jetbrains.com/plugin/10942-kotlin-fill-class)

# kotlin-fill-class plugin
Intellij plugin that provide quick fix action for empty constructor to fill property with default value.
Inspired by Go [fillstruct](https://github.com/davidrjenni/reftools/tree/master/cmd/fillstruct)

## Usage
This plugin add quick fix action for invalid constructor expression.
![fill-class](https://user-images.githubusercontent.com/8841470/44616661-ce042280-a88e-11e8-81fe-b7ce5e7c4871.gif)

## How to install
Install from jetbrains plugins repository.
https://plugins.jetbrains.com/plugin/10942-kotlin-fill-class

## TODO
- Fill default parameter for non primitive type. Currently this plugin dose not support class like below. 
```kotlin
data class Address(zipCode: String)

data class User(
       val name: String,
       val age: Int,
       val address: Address  // Currently this plugin fills empty value for this parameter
)
```

- Fill parameters for non primary constructor. Currently this plugin only fill parameters for primary constructor.

## Thanks
- @shiraji Convert to intention & Support non-empty constructor
- @Pluu @naofumi-fujii fix #2 Double type fill error

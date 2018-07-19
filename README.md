# kotlin-fill-class plugin
Intellij plugin that provide quick fix action for empty constructor to fill property with default value.
Inspired by [go-fill-struct](https://github.com/s-kostyaev/go-fill-struct)

## Usage
This plugin add quick fix action for invalid constructor expression.
![fill-class](https://user-images.githubusercontent.com/8841470/42932763-24e15c72-8b7e-11e8-9e60-ee2f8095d6cc.gif)

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

## How to install
TBD


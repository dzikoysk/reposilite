---
id: kotlin
title: Kotlin
---

A lot of people come from Java to Reposilite, but never really gave Kotlin a try. 
Its syntax may look confusing at first, but it's really simple and clear when you'll learn what it does.

### General

* Semicolons are optional in Kotlin, so we don't use them in Reposilite
* Forget about primitive types, those are handled automatically under the hood whenever possible
* Declarations are reversed, 
  so the name of property is first and then comes its type,
  e.g. `surname: String`. 
  That's because in most cases, type is optional and you don't have to declare it explicitly.
* `Object` type is represented in Kotlin by `Any` type
* Methods returns `Unit` instead of `void` by default
* You should use [IntelliJ IDEA](https://www.jetbrains.com/idea/) to get the best experience.

### Variables
Let's start with the most basic structure - variables.

<CodeVariants>
  <CodeVariant name="Kotlin">

```kotlin
// Full declaration
val immutableVariable: String = "text"
var mutableVariable: String = "text"

// With type interference
val immutableVariable = "text"
var mutableVariable = "text"
```

  </CodeVariant>
  <CodeVariant name="Java">

```java
// Full declaration
final String immutableVariable = "text"
String mutableVariable = "text"

// With type interference
final var immutableVariable = "text"
var mutableVariable = "text"
```

  </CodeVariant>
</CodeVariants>

### Null safety
In general, we can all agree that `null` is quite painful value to handle.
To cover this design flaw in Java, 
we can use `Optional<T>` wrapper to make sure the absence of the value will be handled by user.
In Kotlin, instead of wrapping every value in such wrapper,
we can just simply add `?` symbol to type to mark it as nullable:

<CodeVariants>
  <CodeVariant name="Kotlin">

```kotlin
val value: String? = "text"

// compilation error: cannot call isEmpty() on nullable type
// val isEmpty = value.isEmpty()

val isEmpty = value
  ?.isEmpty() // ?. operator calls method if 'value' is not null or returns null
  ?: true // ?: returns default value if previous expression was null
```

  </CodeVariant>
  <CodeVariant name="Java">

```java
Option<String> value = Option.of("text")

boolean isEmpty = value
  .map(text -> text.isEmpty())
  .orElseGet(true)
```

  </CodeVariant>
</CodeVariants>

In Java we need to wrap every type into Optional to get compile-time insights about illegal calls on nullable types.
Kotlin compiler will warn you multiple times whenever you try to do this directly on a type.
Also, Kotlin supports those nullable signatures through whole Kotlin STD, 
which means that collections etc. returns it by default.

<CodeVariants>
  <CodeVariant name="Kotlin">

```kotlin
// Creates immutable Map<String, String>
// Use mutableMapOf() for MutableMap
val map = mapOf(
  // Syntax sugar 'Key to Value' creates instance of Pair<Key, Value>
  "key1" to "value1",
  "key2" to "value2"
)

val handled: String? = map["key3"]
val value: String = map["key3"] ?: "default"
```

  </CodeVariant>
  <CodeVariant name="Java">

```java
Map<String, String> map = new HashMap<>();
map.put("key1", "value1");
map.put("key2", "value2");

// Not really handled, you have to remember about its nullability
String nullable = map.get("key3"); 
// Usually we need to wrap a lot of such values 
Option<String> handled = Option.of(map.get("key3"));
// We could use map.getOrDefault() in this case, 
// but it's still quite rare scenario to see such extra methods.
// Usually, modern API returns Optional<T> for nullable responses, 
// but Java can't change its API to keep compatibility, 
// so it'll never get better at this point.
Option<String> value = Option.of(map.get("key3")).orElseGet("default");
```

  </CodeVariant>
</CodeVariants>

### Classes
Class definition looks quite similar,
the biggest change you'll see is probably definition of fields through constructor that is placed directly in class definition. 
The best way to see the difference between those languages is by comparison:

<CodeVariants>
  <CodeVariant name="Kotlin">

```kotlin
class User(
  val username: String,
  var balance: Double = 0.00
) : Serializable {

  override fun toString(): String =
    "$username ($balance USD)"

}

val user = User("Michael Scott") // no 'new' keyword
user.balance = 4.20 // Kotlin translates `setBalance` to `balance`
```

  </CodeVariant>
  <CodeVariant name="Java">

```java
public final class User implements Serializable {

  private final String username;
  private final double balance;

  public User(String name, String surname) {
    this.name = name;
    this.surname = surname;
  }

  public User(String username) {
    this(username, 0.00)
  }

  public String getName() { return name; }

  public Double getBalance() { return balance; }
  public void setBalance(double newBalance) { this.balance = newBalance; }

  @Override
  public String toString() {
    return String.format("%s (%s USD)", username, balance)
  }
}

User user = new User("Dwight Schrute", 4.20)
user.setBalance(0)
```

  </CodeVariant>
</CodeVariants>

Of course that's like a minimal example of class, 
but you should see the overall pattern.
In general Kotlin generates a lot of boilerplate under the hood, so you don't have to write it.
A few notes:

* When method body is a one-liner like `fun method(): Any { return expression }`, 
  you can use `= expression` operator to return this directly without a need to write standard body. 
  _(Just like in the example above with toString() method)_
* Whenever you want to override field or method, you have to use `override` keyword
* You can pass arguments to constructors and methods using named arguments, 
  e.g. `User(balance = 7, username = "Michael Scott")`.

### Functions
Kotlin supports multiple variants of functions, you can find them all in Kotlin docs:

* [Kotlin Docs / Functions](https://kotlinlang.org/docs/functions.html)
* [Kotlin Docs / Lambdas](https://kotlinlang.org/docs/lambdas.html)


We'll just show its fundamentals here,
but it should be enough to understand most of use-cases in Reposilite.

#### Definition 
Kotlin supports functional signatures on language level, 
it means that instead of using interface based signatures like in Java (e.g. `BiFunction<A, B, R>`),
you can just write `(A, B) -> R`.

#### Calls

In Kotlin, lambda definition looks like this:

<CodeVariants>
  <CodeVariant name="Kotlin">

```kotlin
val runnable: () -> Unit = { println("Reposilite") }
val consumer: (String) -> Unit = { println(it) }
val function: (Int) -> String = { it.toString() }
val biFunction: (Int, Int) -> Int = { a, b -> a + b }
```

  </CodeVariant>
  <CodeVariant name="Java">

```java
Runnable runnable = () -> out.println("Reposilite");
Consumer<String> consumer = value -> out.println(values);
Function<Integer, String> function = value -> Integer.toString(value);
BiFunction<Integer, Integer, String> biFunction = (a, b) -> a + b;
```

  </CodeVariant>
</CodeVariants>

As you can see, we're using `it` variable in those lambdas. 
You can think about it like a keyword in Kotlin that refers to the unnamed argument in single-parameter lambdas.
The most confusing part is often the we pass lambdas to functions:

<CodeVariants>
  <CodeVariant name="Kotlin">

```kotlin
val hasMoney = Result.ok("10")
  /* Single parameter */
  .map({ it.toInt() })     // Standard parameter
  .map() { it.toString() } // Simplify it by pulling the last lambda argument of out method
  .map { it.toInt() }      // Because () brackets are empty, you can just skip it
  /* Multiple parameters */
  .filter({ it > 0 }, { "Error: Negative balance" }) // Standard parameters
  .filter({ it > 0 }) { "Error: Negative balance" } // Only the last lambda argument can be pulled out
  .isOk()
```

  </CodeVariant>
  <CodeVariant name="Java">

```java
boolean hasMoney = Result.ok("10")
  .map((value) -> Integer.parseInt(value))
  .filter(value -> value > 0, value -> "Error: Negative balance")
  .isOk()
```

  </CodeVariant>
</CodeVariants>

#### DSL

Quite useful enhancement to functions offered by Kotlin is a special type of argument that affects lambda context:

```kotlin
data class Request(val url: String, val ip: String)

// 'Request.' declares that we'll change context of given labda
// So 'this' will point to `Request` instance
fun handleRequest(callable: Request.() -> Unit) =
  // Context argument is now the fist parameter in 'callable' function
  callable.invoke(Request("reposilite.com", "127.0.0.1")) 

// Usage of our DSL function
handleRequest {
  println(this.url) // Explicit call using 'this.'
  println(ip) // We can now use Request's properties directly
}
```

### Kotlin Standard Library
List of functions available on every object:

| Function | Example | Result | Description |
| :--: | :--: | :--: | :--: |
| `let` | `"10".let { it.toInt() }` | `10` | Maps one value to another |
| `also` | `createUser().also { users.add(it) }` | Result of createUser() | Consumes value and returns it as result |
| `with` | `with(user) { this.username }` | Username | Maps value with DSL function

### Error handling
Reposilite uses `panda.std.Result<Value, Error>` wrapper from [expressible](https://github.com/panda-lang/expressible) 
library to handle errors gracefully, without unexpected runtime exceptions. 
For instance, instead of:

```kotlin
fun createUser(username: String): User {
  if (username.isEmpty()) {
    throw IllegalArgumentException("Name cannot be empty")
  }

  return User(username)
}
```

You should return `Result<User, ErrorResponse>`: 

```kotlin
fun createUser(username: String): Result<User, ErrorResponse> =
  username.asSuccess()
    .filter({ it.isEmpty() }) { ErrorResponse(BAD_REQUEST, "Name cannot be empty") }
    .map { User(it) }
```

It's a little bit harder to write, 
but safer - when it comes to control flow,
and easier to handle by users - 
because they're forced to handle it properly.
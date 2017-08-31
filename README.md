# annotations.processor

This repository provides an annotation processor, that generates command dispatch classes for individual commands.

## Commands

Commands consist of several possible __paths__. One path ends at exactly one method and is defined by its required types, including possible constraints.
Each method that is considered as a path needs to have an `@Endpoint` annotation.

```Java
@Endpoint(priority=0)
public void multiply(@IntArg int a, @DoubleArg int b) {
  ...
}

@Endpoint(priority=1)
public void add(@IntArg(min=3) int a, DoubleArg int b) {
  ...
}
```

Compiling a command class containing methods like this results in a second class, that accepts a `String[]` and tries to find a matching path for this argument array.
Example:
 - `{"2", "2.3"}` can be matched only to the `multiply` method, therefore `multiply(2, 2.3)` is called.
 - `{"4", "2.3"}` can be matched to both the `multiply` and the `add` method. Since `add` has a higher priority, `add(4, 2.3)` is called.
 - `{"3", "Test"}` has no matching path. The dispatcher method returns `false` to signal that no path has been found.

## Available annotations

### `@Endpoint`

Used to mark paths for the dispatched.

Options:
 - `priority`: Higher priority guarantees execution, when more than one path matches. __Default__: `0`
 - `target`: An array of `CommandSource` enums. Specifies which senders can call a command. __Default__: `{CommandSender.PLAYER}`

### `@IntArg`

An argument of type int.

Options:
 - `min`: The lowest allowed value. __Default__: `Integer.MIN_VALUE`
 - `max`: The highest allowed value. __Default__: `Integer.MAX_VALUE`

### `@DoubleArg`

An argument of type double.

Options:
 - `min`: The lowest allowed value. __Default__: `Double.MIN_VALUE`
 - `max`: The highest allowed value. __Default__: `Double.MAX_VALUE`
 - `allowNaN`: If `NaN` is an allowed value. __Default__: `false`
 - `allowInfinity`: If `Infinity` is an allowed value. __Default__: `false`

### `@StringArg`

An argument of type String.

### `@LiteralArg`

A literal string value. It's use can be best described by giving an example.

```Java
@Endpoint
public void copy(@LiteralArg(value="copy") String l, @StringArg String msg) {
  ...
}
```
- `{"copy", "test.pdf"}` can be matched only to the `copy` method, therefore `copy("copy", "test.pdf")` is called.
- `{"delete", "taxes.txt"}` can not be matched to any method. 

Required:
 - `value`: The literal string.

Optional:
 - `aliases`: Allowed aliases of the literal string. __Default__: `{}`

### Arrays

Every argument, except for `@LiteralArg` can be an array, if they are the last parameter of a method.
The array consumes every following argument matching its constraints, and calls the method if all arguments were consumed successfully.

### Custom annotations

Custom argument annotations can be created. These annotations must itself have the `@Argument` annotation.

## Example maven config

```xml
<build> 
    <plugins>
        <plugin>
            <groupId>org.bsc.maven</groupId>
            <artifactId>maven-processor-plugin</artifactId>
            <version>2.2.4</version>
            <executions>
                <execution>
                    <id>process</id>
                    <goals>
                        <goal>process</goal>
                    </goals>
                    <configuration>
                        <processors>
                            <processor>com.spleefleague.annotations.processor.Processor</processor>
                        </processors>
                    </configuration>
                    <phase>generate-sources</phase>
                </execution>
            </executions>
            <dependencies>
                <dependency>
                    <groupId>com.spleefleague</groupId>
                    <artifactId>annotations.processor</artifactId>
                    <version>1.0</version>
                    <scope>compile</scope>
                    <optional>true</optional>
                </dependency>
            </dependencies>
        </plugin>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>2.3.2</version>
            <configuration>
                <compilerArgument>-proc:none</compilerArgument>
            </configuration>
        </plugin>
    </plugins> 
</build>
<repositories>
    <repository>
        <id>spleefleague-maven</id>
        <name>spleefleague-maven</name>
        <url>https://maven.spleefleague.com</url>
    </repository>
</repositories>
<dependencies>
    <dependency>
        <groupId>com.spleefleague</groupId>
        <artifactId>annotations</artifactId>
        <version>1.0</version>
    </dependency>
</dependencies>
```

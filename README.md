# AbstractDatabaseORM

This ORM was created to provide easy-to-use and easy-to-implement databases in all simple projects.
When you need to create small project with database usage - you can try this library.

## How to use?

In your project create model interface:

```kotlin
interface Example {
    @PrimaryKey
    @Autoincrement
    var id: Int?
    var text: String = ""
}
```

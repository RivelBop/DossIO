# Contribution and Issues

We appreciate any assistance in making DossIO even better!

Please report any major issues you come across to
the [GitHub Issues Page](https://github.com/RivelBop/DossIO/issues), even if you don't have the
technical knowledge (all issues/bug reports are helpful)!

Below we have attached the dependencies we use in this project, as well as our coding style
guidelines.

Please refer to [Google's Java Style Guide](https://google.github.io/styleguide/javaguide.html) for
guidance. Make sure to run the
<code>check.sh</code> or <code>check.bat</code> commands to keep your code consistent and check
for common java errors!

## Dependencies

### Core Functionality

* [JDK 21](https://www.oracle.com/java/technologies/downloads/#java21): The Project's JDK; We are
  using Java 21 for this project for a couple reasons: it is a modern LTS version of Java, it
  supports JavaFX 21 (an LTS version), and it doesn't have Java 25's new main method changes and
  print statements (which could be seen as a new confusing convention).
* [JavaFX](https://openjfx.io/): The GUI Framework; We use this over Java Swing since it is more
  modern and features a more polished user interface.
* [Kryonet](https://github.com/crykn/kryonet): The Client/Server Network Communication Library; We
  use this over Java Sockets due to its feature-rich setup, multi-threading capabilities, and
  ease-of-use.
* [Directory Watcher](https://github.com/gmethvin/directory-watcher): A Java Library for Watching
  Directories for File Changes; This allows us to monitor file changes in real-time for syncing.
* [JGit](https://github.com/eclipse-jgit/jgit): Java Implementation of Git; This allows us to parse
  .gitignore and .dosshide files in our file filter system.
* [SLF4J](https://slf4j.org/): Simple Logging Facade for Java; We don't directly use this for
  logging, but to avoid warnings when JGit calls SLF4J methods.
* [FindBugs JSR305](https://mvnrepository.com/artifact/com.google.code.findbugs/jsr305): Annotations
  for Software Defect Detection; This allows us to use annotations like @CheckForNull for better
  code safety.

### Extras

* [Shadow](https://github.com/GradleUp/shadow): Gradle Plugin to Create FAT JARs; We use this to
  pack and export all our dependencies into a FAT jar for ease-of-use.
* [Checkstyle](https://github.com/checkstyle/checkstyle): A Tool that Ensures Adherence to a Code
  Standard; This checks our code style against the Google Java Style Guide to keep our code clean
  and consistent.
* [Error Prone](https://github.com/google/error-prone): A Static Analysis Tool for Java; This helps
  us catch common programming mistakes at compile-time.
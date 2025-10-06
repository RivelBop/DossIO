# DossIO

Open Source Java software for distributed collaborative software development.

## Cross-Platform Desktop Collaboration Software

**DossIO is a cross-platform desktop collaboration software based on [JavaFX](https://openjfx.io/)
and [Kryonet](https://github.com/crykn/kryonet), designed for Windows, Linux, and macOS.** It allows
users to synchronize files locally and run projects on device.

## Open Source

DossIO is released under the [Apache 2.0 License](https://www.apache.org/licenses/LICENSE-2.0.html),
offering unrestricted usage in both commercial and non-commercial projects. While not mandatory, we
appreciate any credit given to DossIO when you release a project using it.

## Getting Started with DossIO

TODO

## Dependencies

### Core Functionality

* [JDK 21](https://www.oracle.com/java/technologies/downloads/#java21): The Project's JDK; We are
  using Java 21 for this project for a couple reasons: it is a modern LTS version of Java, it
  supports
  JavaFX 21 (an LTS version), and it doesn't have Java 25's new main method changes and print
  statements (which could be seen as a new confusing convention).
* [JavaFX](https://openjfx.io/): The GUI Framework; We use this over Java Swing since it is more
  modern
  and features a more polished user interface.
* [Kryonet](https://github.com/crykn/kryonet): The Client/Server Network Communication Library; We
  use
  this over Java Sockets due to its feature-rich setup, multi-threading capabilities, and
  ease-of-use.
* [JGit](https://github.com/eclipse-jgit/jgit): Java Implementation of Git; This allows us to parse
  .gitignore and .dosshide files in our file filter system.
* [SLF4J](https://slf4j.org/): Simple Logging Facade for Java; We don't directly use this for
  logging,
  but to avoid warnings when JGit calls SLF4J methods.

### Extras

* [Shadow](https://github.com/GradleUp/shadow): Gradle Plugin to Create FAT JARs; We use this to
  pack and export all our dependencies into a FAT jar for ease-of-use.
* [Checkstyle](https://github.com/checkstyle/checkstyle): A Tool that Ensures Adherence to a Code
  Standard; This checks our code style against the Google Java Style Guide to keep our code clean
  and consistent.
* [Error Prone](https://github.com/google/error-prone): A Static Analysis Tool for Java; This helps
  us catch common programming mistakes
  at compile-time.

## Contribution and Issues

We appreciate any assistance in making DossIO even better. Note that contributing involves working
directly with DossIO's source code, a process that regular users do not typically undertake. Refer
to [Google's Java Style Guide](https://google.github.io/styleguide/javaguide.html) for guidance.

Please report any major issues you come across!
HeLI Language Identifier for ELG
================================

This is a modified version of the [HeLI-OTS](https://zenodo.org/record/6077089) "off-the-shelf" language identification tool that offers an HTTP API compatible with the requirements of the [European Language Grid](https://www.european-language-grid.eu).  The original HeLI operates only as a single-threaded command-line tool - this project makes a few minor modifications to the original `HeLI.java` to make it possible to invoke the HeLI language ID logic as a method call within a larger application, then defines a simple ELG-compatible wrapper around this method call using the [ELG Micronaut LT Service helper](https://gitlab.com/european-language-grid/platform/lt-service-micronaut).

Docker images for HeLI can be built in two ways, either via `jib` to build an image that includes a normal JVM, or via `./gradlew dockerBuildNative` to build a GraalVM native image.  The native image runs the HeLI initialization routine at image build time rather than container startup, so it produces a larger Docker image but one that starts up more quickly (~1-5 seconds rather than ~25-30) and requires less memory at runtime.

Licence and Copyright
---------------------

The original HeLI-OTS code is published under the Apache Licence version 2.0, and is copyright 2020 Tommi Jauhiainen, 2022 University of Helsinki and 2022 Heidi Jauhiainen.  The language models are released under CC-BY-4.0.  Citation information is available [at Zenodo](https://doi.org/10.5281/zenodo.6077089).

The modifications to make HeLI usable as a library and the Micronaut application itself are by Ian Roberts and are released under the same Apache Licence 2.0.
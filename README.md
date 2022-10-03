# ELG API for HeLI-OTS language identifier

[HeLI-OTS](https://zenodo.org/record/7066611) "off-the-shelf" language identifier
has language models for 200 languages.
This modified version offers an HTTP API compatible with the requirements of the
[European Language Grid](https://www.european-language-grid.eu).
The original HeLI operates only as a single-threaded command-line tool - this project
makes a few minor modifications to the original `HeLI.java` to make it possible to invoke
the HeLI language ID logic as a method call within a larger application, then defines a
simple ELG-compatible wrapper around this method call using the
[ELG Micronaut LT Service helper](https://gitlab.com/european-language-grid/platform/lt-service-micronaut).

## Authors

### HeLI-OTS

The original HeLI-OTS code is published under the Apache Licence version 2.0,
and is copyright Tommi Jauhiainen and Heidi Jauhiainen, University of Helsinki (2022).
The language models are released under CC-BY-4.0.
Citation information is available [at Zenodo](https://doi.org/10.5281/zenodo.7066611).

### HeLI Language Identifier for ELG

The modifications to make HeLI usable as a library and the Micronaut application itself are by
[Ian Roberts](https://gitlab.com/i.roberts/heli-elg)
and are released under the same Apache Licence 2.0. His version is forked to the branch `ian`.

### ELG API

This version was developed in EU's CEF project:
[Microservices at your service](https://www.lingsoft.fi/en/microservices-at-your-service-bridging-gap-between-nlp-research-and-industry).
And it is released under the same Apache License 2.0.

## Development

### Download HeLI.jar

Download `HeLI.jar` and move manually jar file to directory `local-lib`:

```
cd utils
./download.sh
```

### Run service

Startup takes about 25 seconds (model downloading). Service runs at port 8080.

```
./gradlew run
```

### Example call

#### Request

```
curl -H "Content-Type: application/json" -d @utils/sample.json http://localhost:8080/process
```

**Sample.json**

```json
{
    "type": "text",
    "content": "Hello, world!",
    "params": {
        "nbest": 2,
        "languages": ["fin","swe","eng"]
    }
}
```

Parameters are optional, and can be used to change the number of top languages
or to restrict the language set.

- `nbest` (integer, default=5)
- `languages` (list of strings, default=all languages). Use ISO 639-3 language codes.

#### Response

The identifier will return *nbest* languages (ISO 639-3 codes) followed by their
languages scores (lower is better).

```json
{
  "response": {
    "type": "classification",
    "classes": [
      {
        "class": "eng",
        "score": 3.9662707
      },
      {
        "class": "fin",
        "score": 5.7083645
      }
    ]
  }
}
```

### Integration tests

Wait that Micronaut has started

```
cd utils/
python3 -m unittest -v
```

## Docker image

Docker images for HeLI can be built in three ways, via traditional `dockerBuild`,
via `jib` to build an image that includes a normal JVM, or via
`./gradlew dockerBuildNative` to build a GraalVM native image.
The native image runs the HeLI initialization routine at image build time
rather than container startup, so it produces a larger Docker image but one
that starts up more quickly (~1-5 seconds rather than ~25-30) and requires less
memory at runtime.

```
./gradlew dockerBuild
docker run --rm -p 8080:8080 heli-elg
```
